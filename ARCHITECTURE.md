# Ephemeral Encrypted Chat System Architecture

## 1. Overview

This document describes the architecture of the ephemeral anonymous encrypted chat system in two stages:

- the current single-node implementation
- a multi-node production scaling path

The system is designed around these principles:

- anonymous identity bootstrap with short-lived join credentials
- in-memory, ephemeral session and message state
- server-blind relay for encrypted message payloads
- aggressive expiry and teardown behavior
- clean upgrade path toward distributed coordination and stronger end-to-end cryptography

---

## 2. Current Single-Node Architecture

### 2.1 Functional Summary

The current implementation runs as a single Spring Boot node that handles:

- session bootstrap over REST
- WebSocket/STOMP connection management
- temporary anonymous identity tracking
- session pairing and lifecycle
- in-memory ephemeral message storage
- expiry scanning and message removal
- session-scoped encryption key generation and distribution
- encrypted payload relay with HMAC validation

### 2.2 Single-Node Component Diagram

```text
                          +-----------------------------------+
                          |           Browser Client          |
                          |-----------------------------------|
                          | Bootstrap REST                    |
                          | WebSocket/STOMP                   |
                          | AES-GCM encrypt/decrypt           |
                          | HMAC-SHA256 sign                 |
                          +----------------+------------------+
                                           |
                                           |
                              HTTP / WebSocket over TLS
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                             Spring Boot Application                               |
|-----------------------------------------------------------------------------------|
|  REST Layer                                                                       |
|  +--------------------------+                                                     |
|  | SessionBootstrapController|                                                    |
|  +------------+-------------+                                                     |
|               |                                                                   |
|  Security Layer                                                                   |
|  +-------------------------------+     +-------------------------+                 |
|  | AnonymousChatHandshakeInterceptor |  | SecurityConfig         |                 |
|  +----------------+--------------+     +-------------------------+                 |
|                   |                                                              |
|  WebSocket/STOMP Layer                                                           |
|  +---------------------------+     +------------------------------+               |
|  | ChatWebSocketController   |     | WebSocketSessionEventListener|               |
|  +-------------+-------------+     +---------------+--------------+               |
|                |                                   |                              |
|  Service Layer |                                   | Session Events               |
|  +-------------v-------------+     +---------------v--------------+               |
|  | ChatService               |     | AnonymousSessionManager      |               |
|  | MessageLifecycleManager   |     | WebSocketSessionTracker      |               |
|  | EncryptionService         |     +------------------------------+               |
|  | SessionInitialization...  |                                                |
|  +-------------+-------------+                                                |
|                |                                                              |
|  In-Memory State                                                              |
|  +-------------------------------------------------------------------------+   |
|  | activeTempUsers, activeSessions, userToSessionMap                       |   |
|  | sessionMessages, DelayQueue expiry queue                                |   |
|  | sessionKeysBySessionId                                                  |   |
|  | replay protection sets, rate limit windows, typing debounce maps        |   |
|  +-------------------------------------------------------------------------+   |
|                                                                                   |
|  Scheduler                                                                        |
|  +---------------------------+                                                    |
|  | MessageExpiryScheduler    |                                                    |
|  +---------------------------+                                                    |
+-----------------------------------------------------------------------------------+
```

### 2.3 Current Interaction Flow

#### Bootstrap

```text
Client -> POST /api/session/bootstrap
Server -> generate tempUserId + joinToken
Server -> store hashed joinToken only
Server -> register temp user in memory
Server -> return JoinCredentials(tempUserId, joinToken)
```

#### Connect and Session Pairing

```text
Client -> WebSocket handshake with tempUserId + joinToken
Handshake interceptor -> validate token + IP rate limit
Session manager -> bind socket to temp user
Second matching user arrives -> create ChatSession
Encryption service -> generate ephemeral AES session key
Server -> emit ChatSessionInitializedEvent
Server -> send session key to both users on /user/queue/session.init
```

#### Messaging

```text
Client -> encrypt plaintext with AES-GCM
Client -> compute HMAC-SHA256 over message metadata
Client -> send encrypted payload via STOMP
Server -> validate session, replay window, rate limits, HMAC
Server -> never decrypt
Server -> store encrypted message ephemerally
Server -> broadcast encrypted payload to /topic/session.{sessionId}
Client -> decrypt locally
```

---

## 3. Single-Node Limitations

The current node keeps all important runtime state in memory:

- temp users
- chat sessions
- replay tracking
- typing debounce windows
- message queues
- expiry queues
- ephemeral AES session keys

This keeps latency low, but means:

- node loss destroys all live sessions
- horizontal scale is not yet safe without shared state
- websocket affinity is required if more than one node is added

---

## 4. Multi-Node Scaling Design with Redis

### 4.1 Goals

The first scaling step should preserve ephemeral behavior while allowing:

- multiple application nodes
- shared session state
- reconnect tolerance
- consistent expiry and security decisions

Redis is a strong fit because the system state is:

- short-lived
- key-value oriented
- latency sensitive
- coordination heavy

### 4.2 Recommended Redis Responsibilities

Use Redis for:

- `tempUser:{tempUserId}` -> temp user metadata, join token hash reference, websocket owner node
- `session:{sessionId}` -> paired user ids, timestamps, encryption-enabled flag
- `sessionKey:{sessionId}` -> short-lived AES session key with TTL
- `userSession:{tempUserId}` -> session id
- `waitingUsers` -> queue/set for matchmaking
- `messageReplay:{sessionId}` -> bounded set or bloom-like structure with TTL
- `rate:bootstrap:{ip}` -> sliding counters
- `rate:session:{sessionId}` -> message counters
- `typing:{sessionId}:{userId}` -> debounce timestamps
- `expiry:index` -> sorted set keyed by expiry timestamp

Keep Redis TTL aligned with ephemeral design so keys disappear automatically when idle.

### 4.3 Multi-Node Diagram

```text
                         +----------------------+
                         |      Load Balancer   |
                         |  TLS + WS Upgrade    |
                         +----------+-----------+
                                    |
                 +------------------+------------------+
                 |                                     |
                 v                                     v
      +------------------------+           +------------------------+
      |   App Node A           |           |   App Node B           |
      |------------------------|           |------------------------|
      | REST + STOMP + Relay   |           | REST + STOMP + Relay   |
      | Local connection map   |           | Local connection map   |
      | Local delivery only    |           | Local delivery only    |
      +-----------+------------+           +-----------+------------+
                  |                                    |
                  +----------------+-------------------+
                                   |
                                   v
                         +----------------------+
                         |        Redis         |
                         |----------------------|
                         | temp users           |
                         | sessions             |
                         | session keys         |
                         | replay windows       |
                         | expiry indexes       |
                         | rate limits          |
                         +----------------------+
```

### 4.4 Redis Synchronization Strategy

- local node owns active socket connection objects
- Redis owns distributed authoritative session state
- node-local caches are allowed for read optimization but must be disposable
- use Redis pub/sub or streams for cross-node session and message notifications

For example:

```text
Node A receives encrypted message
Node A validates against Redis-backed session state
Node A publishes "session message relay" event
Node B sees event if peer socket lives on Node B
Node B pushes to its local WebSocket connection
```

---

## 5. WebSocket Gateway Routing Strategy

### 5.1 Near-Term Strategy

Use sticky sessions at the edge:

- HTTP bootstrap can go to any node
- WebSocket upgrade should stay pinned to one node
- each user socket lives on exactly one gateway instance

This simplifies:

- delivery to `/user/queue/session.init`
- per-connection backpressure
- connection-local bookkeeping

### 5.2 Gateway Routing Diagram

```text
Client A ----\
              \          +-------------------+
               +-------> | Gateway / LB      | ---> Node A
              /          | sticky by socket  |
Client B ----/           +-------------------+ ---> Node B

Node A local sockets: A
Node B local sockets: B

Shared session state lives in Redis.
Cross-node fanout uses Redis pub/sub or broker topics.
```

### 5.3 Recommended Routing Rules

- route WebSocket upgrades using cookie or consistent hash
- store `ownerNodeId` for each connected temp user in Redis
- route direct user initialization events to the node owning the socket
- route session broadcasts through a shared broker when peers are on different nodes

---

## 6. Message Broker Upgrade Path

### 6.1 Current State

The current implementation uses Spring’s simple in-process STOMP broker. This is fine for:

- development
- single-node low-scale usage
- minimal operational overhead

It is not sufficient for:

- cross-node fanout
- delivery buffering
- operational observability
- replayable event pipelines

### 6.2 Upgrade Options

#### RabbitMQ Path

Best when:

- STOMP/WebSocket integration is primary
- routing semantics matter
- operational simplicity is preferred

Use RabbitMQ for:

- session event fanout
- encrypted message relay topics
- per-node user delivery queues

Diagram:

```text
Node A -> RabbitMQ exchange -> Node B consumer -> local WebSocket push
Node B -> RabbitMQ exchange -> Node A consumer -> local WebSocket push
```

Pros:

- easy Spring integration
- strong routing patterns
- fits transient messaging well

Cons:

- less ideal for very high-throughput analytics/event history

#### Kafka Path

Best when:

- very high throughput is expected
- long-term event pipeline evolution matters
- audit, analytics, or moderation sidecars may later consume metadata

Use Kafka for:

- session lifecycle streams
- encrypted relay event streams
- distributed expiry orchestration

Pros:

- durable streaming backbone
- scalable partitions
- strong consumer ecosystem

Cons:

- more operational complexity
- per-user or fine-grained routing is less natural than RabbitMQ

### 6.3 Recommended Upgrade Sequence

1. Single-node in-process broker
2. Multi-node with Redis pub/sub for lightweight coordination
3. RabbitMQ for production cross-node relay
4. Kafka only if event scale or downstream stream processing justifies it

---

## 7. Failure Scenarios

### 7.1 Node Crash

#### Single Node

Impact:

- all sockets disconnect
- all in-memory temp users, sessions, keys, and messages are lost
- ephemeral guarantees are preserved, but availability is lost

Mitigation:

- accept as part of ephemeral model in development
- reconnect and bootstrap again

#### Multi Node

Impact:

- only users connected to crashed node lose sockets
- if state is Redis-backed, reconnect can restore temp identity/session state

Mitigation:

- Redis-backed session records
- gateway reconnection support
- node ownership transfer on reconnect

### 7.2 Network Partition

Impact:

- clients may remain connected to one node but lose access to Redis or broker
- split-brain delivery is possible if local fallback behavior is too permissive

Mitigation:

- fail closed on session state uncertainty
- reject message acceptance when distributed state validation cannot complete
- terminate sessions that cannot confirm authority within a timeout

Recommended rule:

```text
No authoritative session state -> no message acceptance
```

### 7.3 Delayed Expiry

Impact:

- messages may visually persist slightly longer on server or client
- client-side timers and backend expiry scans can drift

Mitigation:

- keep expiry timestamp inside payload
- enforce client-side vanish on `expiryTime`
- use Redis sorted sets or broker timers for distributed expiry
- tolerate short delay as a UI artifact, not a confidentiality guarantee

Important note:

Ephemeral expiry is best-effort for runtime state, not cryptographic erasure of already-delivered plaintext on user devices.

---

## 8. Security Threat Model

### 8.1 Assets to Protect

- session join credentials
- ephemeral session keys
- encrypted message payloads
- replay resistance
- session routing integrity

### 8.2 Primary Threats

#### Credential Theft

Threat:

- attacker obtains `tempUserId` and `joinToken`

Mitigations:

- join token required in addition to temp user id
- token stored hashed only
- token consumed on handshake
- token TTL
- IP rate limits on bootstrap and handshake

#### Replay Attacks

Threat:

- attacker resends previously observed message frames

Mitigations:

- per-session duplicate `messageId` rejection
- HMAC over encrypted payload metadata
- session teardown destroys replay tracking state

#### Message Tampering

Threat:

- intermediary modifies ciphertext or metadata

Mitigations:

- HMAC-SHA256 validation before relay
- client-side AES-GCM authentication
- no server-side plaintext handling

#### Session Hijack

Threat:

- attacker attempts unauthorized websocket join

Mitigations:

- validated handshake interceptor
- consumed join token
- user principal bound from validated handshake attributes

#### Memory Scraping / Host Compromise

Threat:

- attacker with host access reads in-memory session keys

Mitigations:

- no persistent key storage
- fast session teardown
- minimize key lifetime
- isolate workloads and restrict operator access

### 8.3 Residual Risks

- simulated session key distribution is not real E2E trust establishment
- if the server generates the AES key, the server is part of the trust model
- browser memory remains a plaintext exposure point

---

## 9. Performance Bottleneck Analysis

### 9.1 Current Likely Bottlenecks

#### WebSocket Fanout

The main hot path is:

- inbound frame parsing
- session validation
- HMAC validation
- relay broadcast

Pressure points:

- STOMP frame overhead
- JSON serialization/deserialization
- single-node socket fanout

#### In-Memory Maps

Large concurrent maps for:

- temp users
- sessions
- replay windows
- expiry indices

These are fast, but memory-heavy under large concurrency.

#### Expiry Cleanup

Even with `DelayQueue`, large expired bursts may cause:

- cleanup spikes
- GC pressure from short-lived objects

#### Frontend DOM Growth

Client-side message logs can become expensive if many live messages exist simultaneously, though expiry bounds this naturally.

### 9.2 Secondary Bottlenecks in Multi-Node Mode

- Redis round trips for every message
- cross-node user delivery lookup
- broker fanout lag
- TLS termination pressure at gateway

---

## 10. Memory Pressure Mitigation Strategy

### 10.1 Server-Side Controls

- hard cap on active sessions
- hard cap on bootstrap rate and message rate
- TTL-based Redis eviction for temp identities and replay keys
- bounded replay sets per session
- purge message queues immediately on session termination
- destroy AES session keys immediately on session termination

### 10.2 Recommended Additional Controls

- maximum active temp users
- idle temp user expiration before pairing
- cap live messages per session even before expiry
- approximate replay filter for very large scale
- offload low-value counters to Redis with aggressive TTLs

### 10.3 JVM Considerations

- use G1 or ZGC depending on memory profile
- keep object models compact
- avoid storing redundant copies of encrypted payloads
- avoid per-message timers or threads

---

## 11. Future E2E Encryption Upgrade Path with Diffie-Hellman

### 11.1 Current Simulation

Current model:

- server generates AES session key
- server distributes it to both users
- server never decrypts ciphertext

This simulates encrypted relay, but is not true end-to-end cryptography because the server originates the session secret.

### 11.2 Future True E2E Path

Use authenticated ephemeral Diffie-Hellman:

- X25519 or P-256 ECDH for key agreement
- per-user ephemeral keypairs
- server relays public keys only
- clients derive shared secret locally
- symmetric message keys derived with HKDF

### 11.3 Proposed Handshake Evolution

```text
1. Bootstrap issues anonymous identity token only
2. Client A generates ephemeral DH public key
3. Client B generates ephemeral DH public key
4. Server relays public keys through session.init channel
5. Both clients derive same shared secret locally
6. Clients derive:
   - AES-GCM encryption key
   - HMAC key or AEAD-only scheme
7. Server never knows the final session secret
```

### 11.4 Additional Requirements for Real E2E

- key confirmation flow
- identity binding strategy, even for anonymous mode
- forward secrecy rotation policy
- optional post-compromise recovery
- authenticated session-init transcript

### 11.5 Best Long-Term Design

Use:

- X25519 for shared secret derivation
- HKDF-SHA256 for key schedule
- AES-GCM or ChaCha20-Poly1305 for message encryption
- per-message nonce discipline
- signed handshake transcripts if stronger authenticity is required

---

## 12. Deployment Topology

### 12.1 Development Topology

```text
+------------------+
| Browser Client   |
+--------+---------+
         |
         v
+------------------+
| Spring Boot Node |
| REST + WS + STOMP|
+------------------+
```

### 12.2 Initial Production Topology

```text
               +----------------------+
               |   CDN / Edge TLS     |
               +----------+-----------+
                          |
                          v
               +----------------------+
               | Load Balancer / WAF  |
               +----------+-----------+
                          |
          +---------------+---------------+
          |                               |
          v                               v
 +-------------------+           +-------------------+
 | Chat App Node A   |           | Chat App Node B   |
 | REST + WS Gateway |           | REST + WS Gateway |
 +---------+---------+           +---------+---------+
           \                               /
            \                             /
             +-----------+---------------+
                         |
                         v
                 +---------------+
                 | Redis Cluster |
                 +---------------+
```

### 12.3 Larger-Scale Topology

```text
Users
  |
  v
+-------------------------+
| Global Edge / Anycast   |
+------------+------------+
             |
             v
+-------------------------+
| Regional LB + WAF       |
+------------+------------+
             |
   +---------+----------+------------------+
   |                    |                  |
   v                    v                  v
Gateway A           Gateway B          Gateway C
   |                    |                  |
   +---------+----------+------------------+
             |
             v
       Redis / Broker Layer
             |
             v
    Observability + Control Plane
```

### 12.4 Supporting Infrastructure

Recommended supporting services:

- Redis Sentinel or Redis Cluster
- RabbitMQ or Kafka when cross-node relay scales
- Prometheus/Grafana for metrics
- centralized logs with payload redaction
- synthetic websocket probes

---

## 13. Component Interaction Explanation

### 13.1 Bootstrap and Connect

```text
Client -> Bootstrap Controller
Bootstrap Controller -> JoinTokenService
JoinTokenService -> return tempUserId + joinToken
Bootstrap Controller -> SessionManager.registerIssuedTempUser

Client -> WebSocket handshake
HandshakeInterceptor -> validate join token + IP rate limit
HandshakeHandler -> bind Principal(tempUserId)
SessionEventListener -> SessionManager.attachWebSocketToTempUser
```

### 13.2 Session Initialization

```text
SessionManager -> pair user A and user B
SessionManager -> EncryptionService.initializeSessionKey
SessionManager -> publish ChatSessionInitializedEvent
SessionInitializationNotifier -> /user/queue/session.init to both clients
Clients -> begin encrypted messaging
```

### 13.3 Encrypted Messaging

```text
Sender Client:
plaintext -> AES-GCM encrypt -> ciphertext
ciphertext metadata -> HMAC-SHA256
publish STOMP frame

Server:
validate session
validate replay window
validate HMAC
store encrypted message ephemerally
broadcast encrypted payload

Receiver Client:
receive ciphertext
decrypt locally with session key
render plaintext
```

### 13.4 Termination

```text
Disconnect or session teardown
-> remove users from session maps
-> purge message queues
-> clear replay windows
-> destroy session key
-> remove pending session init payloads
```

---

## 14. Recommended Next Steps

1. Move shared ephemeral state into Redis with TTLs.
2. Add websocket node ownership and cross-node relay events.
3. Replace simple broker with RabbitMQ when multi-node relay is needed.
4. Add idle temp-user expiration.
5. Add bounded replay memory per session.
6. Implement real E2E key agreement with X25519 + HKDF.
7. Add observability for:
   - active sockets
   - active sessions
   - message relay latency
   - expiry lag
   - HMAC failures
   - rejected joins

---

## 15. Conclusion

The current system is well-suited for a low-latency single-node ephemeral chat service with simulated encrypted relay. The cleanest production scaling path is:

- Redis for distributed ephemeral state
- sticky websocket routing at the gateway
- RabbitMQ for cross-node fanout when needed
- later upgrade to client-generated Diffie-Hellman keys for true end-to-end secrecy

This preserves the system’s core qualities:

- anonymous access
- short-lived state
- fast teardown
- low-latency messaging
- minimal long-term data retention
