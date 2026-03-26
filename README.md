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

---

## Mobile App Packaging

This repo now includes a Capacitor wrapper in `mobile/` so the same chat experience can be packaged as installable Android and iOS apps.

### Mobile build flow

1. Deploy this backend to a public HTTPS URL.
2. Update `mobile/web/mobile-config.js` with that deployed backend URL.
3. In `mobile/`, run `npm install`.
4. Run `npx cap add android` and `npx cap add ios`.
5. Run `npx cap sync`.
6. Open the native projects with `npm run open:android` or `npm run open:ios`.

### Mobile readiness included

- Isolated mobile web assets under `mobile/web`
- Mobile-aware API and WebSocket URL resolution
- Native WebView origin support in WebSocket configuration
- PWA manifest and offline shell
- Safe-area and phone-screen layout improvements
