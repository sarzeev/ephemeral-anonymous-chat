# Ephemeral Anonymous Chat System

A secure, real-time, session-based anonymous chat platform designed with ephemeral messaging, distributed system thinking, and privacy-first architecture.

This project explores the engineering challenges of building a scalable real-time communication system with strict message lifecycle guarantees and session-scoped encryption.

---

## 🚀 Key Features

- Real-time WebSocket chat engine
- Ephemeral messaging (auto-expiry lifecycle)
- Session-scoped encryption simulation
- Anonymous identity lifecycle management
- Replay attack protection
- Message integrity verification
- Rate limiting & typing debounce
- Matrix-style terminal UI
- Event-driven session architecture
- Distributed scaling design (planned)

---

## 🧠 System Design Highlights

### Ephemeral Messaging Engine
Messages exist only for a short duration and are removed using a lifecycle-driven in-memory expiry scheduler.

### Anonymous Session Model
Users are identified through temporary session identities with secure handshake tokens.

### Security Architecture
- Join token handshake
- Replay protection per session
- Integrity verification via HMAC
- Session-scoped encryption design

### Real-Time Infrastructure
- STOMP over WebSockets
- Concurrent session management
- Typing indicator event routing
- Graceful session teardown

---

## 🏗 Architecture Overview
