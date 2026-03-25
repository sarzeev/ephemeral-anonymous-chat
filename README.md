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
Matrix Terminal UI
->
WebSocket Gateway (STOMP)
->
Session Manager (Ephemeral Identity Engine)
->
Message Lifecycle Manager
->
Expiry Scheduler (In-Memory)

---


## Planned distributed architecture:

- Redis session coordination
- Kafka/RabbitMQ message broker integration
- Horizontal WebSocket gateway scaling

---

## 🛠 Technology Stack

### Backend
- Java 21
- Spring Boot
- Spring WebSocket
- Concurrent Data Structures

### Frontend
- Vanilla JavaScript
- Web Crypto API
- Matrix-style UI rendering

### Infrastructure (Planned)
- Docker containerization
- Render / Vercel deployment
- Distributed system scaling

---

## 🔐 Security Model

- Anonymous identity lifecycle
- Session join-token handshake
- Replay protection mechanisms
- Message integrity via HMAC
- Planned E2E encryption upgrade

---

## ⚡ Performance Considerations

- Lock-aware concurrency design
- Centralized expiry scheduling
- Rate limiting per session
- Typing event debounce optimization
- Memory pressure mitigation strategy

---

## 🎯 Project Motivation

Modern communication systems demand:

- Privacy-preserving infrastructure
- Real-time performance guarantees
- Scalable session management
- Security-first architecture

This project demonstrates system-level engineering of such constraints.

---

## 📌 Current Status

Core backend architecture and real-time engine complete.  
Production deployment and distributed scaling in progress.

---

## 👨‍💻 Author

Sarjeev Sharma  
B.Tech Computer Science Engineering
