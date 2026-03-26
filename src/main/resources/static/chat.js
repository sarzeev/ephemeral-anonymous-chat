(function () {
    const body = document.body;
    const statusElement = document.getElementById("connection-status");
    const messageLog = document.getElementById("message-log");
    const typingIndicator = document.getElementById("typing-indicator");
    const connectForm = document.getElementById("connect-form");
    const messageForm = document.getElementById("message-form");
    const connectButton = document.getElementById("connect-button");
    const disconnectButton = document.getElementById("disconnect-button");
    const messageInput = document.getElementById("message-input");
    const tempUserIdInput = document.getElementById("temp-user-id");
    const joinTokenInput = document.getElementById("join-token");
    const sessionIdInput = document.getElementById("session-id");
    const controlsPanel = document.getElementById("controls-panel");
    const summaryUser = document.getElementById("summary-user");
    const summarySession = document.getElementById("summary-session");
    const introGateway = document.getElementById("intro-gateway");
    const introTitle = document.getElementById("intro-title");
    const introLetters = Array.from(introTitle ? introTitle.querySelectorAll("[data-letter]") : []);
    const introTagline = document.getElementById("intro-tagline");
    const ethicalModal = document.getElementById("ethical-modal");
    const ethicalAgreement = document.getElementById("ethical-agreement");
    const enterButton = document.getElementById("enter-xyron");
    const reduceMotionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");

    const state = {
        client: null,
        subscription: null,
        initSubscription: null,
        sessionId: null,
        sessionReady: false,
        tempUserId: null,
        sessionKey: null,
        encryptionEnabled: false,
        typingTimerId: null,
        typingSentAt: 0,
        messageElementsById: new Map(),
        expiryIndex: [],
        expirySweepHandle: null,
        introComplete: false,
        introReadyForEntry: false,
        introDismissed: false,
        introModalVisible: false
    };

    const INTRO_SESSION_KEY = "xyron-intro-seen";
    const SCRAMBLE_GLYPHS = "XYZRON01<>[]{}$#*+-/";

    function setGatewayVisible(isVisible) {
        body.classList.toggle("gateway-active", isVisible);
        if (!introGateway) {
            return;
        }

        if (isVisible) {
            introGateway.hidden = false;
            introGateway.classList.remove("is-exiting");
        }
    }

    function finishGatewayEntry() {
        if (!introGateway || state.introDismissed || !state.introReadyForEntry || !ethicalAgreement.checked) {
            return;
        }

        state.introDismissed = true;
        document.removeEventListener("keydown", onGatewayKeydown);
        sessionStorage.setItem(INTRO_SESSION_KEY, "1");
        introGateway.classList.add("is-exiting");
        window.setTimeout(() => {
            introGateway.hidden = true;
            setGatewayVisible(false);
            tempUserIdInput.focus();
        }, reduceMotionQuery.matches ? 0 : 560);
    }

    function onGatewayKeydown(event) {
        if (event.key !== "Enter") {
            return;
        }

        if (!state.introReadyForEntry) {
            return;
        }

        event.preventDefault();
        finishGatewayEntry();
    }

    function revealEthicalModal() {
        if (!ethicalModal || state.introModalVisible) {
            return;
        }

        state.introModalVisible = true;
        state.introReadyForEntry = true;
        ethicalModal.hidden = false;
        ethicalAgreement.focus();
    }

    function revealTaglineAndModal() {
        if (!introTagline) {
            revealEthicalModal();
            return;
        }

        introTagline.classList.add("is-visible");
        window.setTimeout(revealEthicalModal, reduceMotionQuery.matches ? 0 : 1000);
    }

    function resolveLetterSequentially(letterElement, targetCharacter, duration, delay) {
        if (!letterElement) {
            return;
        }

        if (reduceMotionQuery.matches) {
            window.setTimeout(() => {
                letterElement.textContent = targetCharacter;
                letterElement.classList.add("is-resolved");
            }, delay);
            return;
        }

        window.setTimeout(() => {
            const startedAt = performance.now();
            const scrambleHandle = window.setInterval(() => {
                const elapsed = performance.now() - startedAt;
                if (elapsed >= duration) {
                    window.clearInterval(scrambleHandle);
                    letterElement.textContent = targetCharacter;
                    letterElement.classList.add("is-resolved");
                    return;
                }

                const randomIndex = Math.floor(Math.random() * SCRAMBLE_GLYPHS.length);
                letterElement.textContent = SCRAMBLE_GLYPHS[randomIndex];
            }, 34);
        }, delay);
    }

    function runGatewaySequence() {
        if (!introGateway) {
            return;
        }

        const introAlreadySeen = sessionStorage.getItem(INTRO_SESSION_KEY) === "1";
        if (introAlreadySeen) {
            introGateway.hidden = true;
            setGatewayVisible(false);
            state.introDismissed = true;
            state.introComplete = true;
            return;
        }

        setGatewayVisible(true);
        document.addEventListener("keydown", onGatewayKeydown);

        if (reduceMotionQuery.matches) {
            introLetters.forEach((letterElement) => {
                letterElement.textContent = letterElement.dataset.letter;
                letterElement.classList.add("is-resolved");
            });
            introTagline.classList.add("is-visible");
            revealEthicalModal();
            state.introComplete = true;
            return;
        }

        introTitle.classList.add("is-flickering");
        const totalDuration = 2000;
        const perLetterDuration = 330;
        const perLetterDelay = Math.floor((totalDuration - perLetterDuration) / Math.max(1, introLetters.length - 1));

        introLetters.forEach((letterElement, index) => {
            resolveLetterSequentially(letterElement, letterElement.dataset.letter, perLetterDuration, index * perLetterDelay);
        });

        window.setTimeout(() => {
            introTitle.classList.remove("is-flickering");
            state.introComplete = true;
            revealTaglineAndModal();
        }, totalDuration + 120);
    }

    function setStatus(status, text) {
        statusElement.textContent = text;
        statusElement.className = "status-value " + status;
    }

    function updateSummary() {
        summaryUser.textContent = state.tempUserId || "idle";
        summarySession.textContent = state.sessionId || "pending";
    }

    function escapeHtml(value) {
        return value
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function appendSystemLine(text) {
        const row = document.createElement("div");
        row.className = "message-line message-system";
        row.innerHTML =
            `<span class="message-prefix">[sys]</span>` +
            `<span class="message-time">${formatClock(new Date())}</span>` +
            `<span class="message-content">${escapeHtml(text)}</span>`;
        messageLog.appendChild(row);
        messageLog.scrollTop = messageLog.scrollHeight;
    }

    async function appendChatMessage(payload) {
        if (state.messageElementsById.has(payload.messageId)) {
            return;
        }

        const createdAt = new Date(payload.createdAt);
        const renderedContent = await resolveRenderedMessage(payload);
        const row = document.createElement("div");
        row.className = "message-line";
        row.dataset.messageId = payload.messageId;
        row.dataset.expiryTime = payload.expiryTime;
        row.innerHTML =
            `<span class="message-prefix">[${escapeHtml(payload.senderId)}]</span>` +
            `<span class="message-time">${formatClock(createdAt)}</span>` +
            `<span class="message-content">${escapeHtml(renderedContent)}</span>`;

        state.messageElementsById.set(payload.messageId, row);
        state.expiryIndex.push({ messageId: payload.messageId, expiryTime: Date.parse(payload.expiryTime) });
        messageLog.appendChild(row);
        messageLog.scrollTop = messageLog.scrollHeight;
    }

    function removeExpiredMessages() {
        if (state.expiryIndex.length === 0) {
            return;
        }

        const now = Date.now();
        let writeCount = 0;

        state.expiryIndex = state.expiryIndex.filter((entry) => {
            if (entry.expiryTime > now) {
                return true;
            }

            const row = state.messageElementsById.get(entry.messageId);
            if (row) {
                row.remove();
                state.messageElementsById.delete(entry.messageId);
                writeCount += 1;
            }

            return false;
        });
    }

    function showTypingIndicator() {
        typingIndicator.hidden = false;
        clearTimeout(state.typingTimerId);
        state.typingTimerId = window.setTimeout(() => {
            typingIndicator.hidden = true;
        }, 900);
    }

    function hideTypingIndicator() {
        clearTimeout(state.typingTimerId);
        typingIndicator.hidden = true;
    }

    function buildClient(tempUserId, joinToken) {
        const url = `${resolveWsProtocol()}://${window.location.host}/ws/chat?tempUserId=${encodeURIComponent(tempUserId)}&joinToken=${encodeURIComponent(joinToken)}`;
        return new StompJs.Client({
            brokerURL: url,
            reconnectDelay: 2500,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            onConnect: () => {
                setStatus("status-online", "ONLINE");
                subscribeToSessionInit();
                if (state.sessionId) {
                    subscribeToSession(state.sessionId);
                }
                appendSystemLine("link established");
                messageInput.focus();
            },
            onStompError: () => {
                setStatus("status-error", "ERROR");
                appendSystemLine("broker rejected connection");
            },
            onWebSocketClose: () => {
                setStatus("status-offline", "OFFLINE");
                appendSystemLine("connection closed");
                unsubscribeFromSession();
            },
            onWebSocketError: () => {
                setStatus("status-error", "ERROR");
                appendSystemLine("websocket fault");
            }
        });
    }

    function subscribeToSession(sessionId) {
        unsubscribeFromTopic();
        state.subscription = state.client.subscribe(`/topic/session.${sessionId}`, (frame) => {
            const payload = JSON.parse(frame.body);
            if (typeof payload.typing === "boolean") {
                if (payload.senderId !== state.tempUserId) {
                    showTypingIndicator();
                }
                return;
            }

            void appendChatMessage(payload);
        });
    }

    function subscribeToSessionInit() {
        if (state.initSubscription) {
            state.initSubscription.unsubscribe();
        }

        state.initSubscription = state.client.subscribe("/user/queue/session.init", (frame) => {
            const payload = JSON.parse(frame.body);
            state.sessionId = payload.sessionId;
            state.sessionReady = true;
            state.sessionKey = payload.sessionKey || null;
            state.encryptionEnabled = Boolean(payload.encryptionEnabled);
            sessionIdInput.value = payload.sessionId;
            updateSummary();
            controlsPanel.open = false;
            subscribeToSession(payload.sessionId);
            appendSystemLine(state.encryptionEnabled ? "session key received" : "development relay mode active");
        });
    }

    function unsubscribeFromTopic() {
        if (state.subscription) {
            state.subscription.unsubscribe();
            state.subscription = null;
        }
        hideTypingIndicator();
    }

    function unsubscribeFromSession() {
        unsubscribeFromTopic();
        if (state.initSubscription) {
            state.initSubscription.unsubscribe();
            state.initSubscription = null;
        }
    }

    function sendTypingEvent() {
        if (!state.client || !state.client.connected || !state.sessionReady || !state.sessionId) {
            return;
        }

        const now = Date.now();
        if (now - state.typingSentAt < 350) {
            return;
        }

        state.typingSentAt = now;
        state.client.publish({
            destination: "/app/chat.typing",
            body: JSON.stringify({ typing: true })
        });
    }

    async function connect(event) {
        event.preventDefault();

        const tempUserId = tempUserIdInput.value.trim();
        const joinToken = joinTokenInput.value.trim();
        const sessionId = sessionIdInput.value.trim();
        if (!isValidTempUserId(tempUserId) || !isValidJoinToken(joinToken)) {
            appendSystemLine("chat id must be 3-8 letters or numbers, and join code must be 4-8 digits");
            return;
        }

        disconnect();
        connectButton.disabled = true;
        appendSystemLine("bootstrapping session...");

        const bootstrapResult = await bootstrapSession(tempUserId, joinToken);
        if (!bootstrapResult.ok) {
            connectButton.disabled = false;
            return;
        }

        state.tempUserId = bootstrapResult.credentials.tempUserId;
        state.sessionId = sessionId || null;
        updateSummary();
        state.client = buildClient(bootstrapResult.credentials.tempUserId, bootstrapResult.credentials.joinToken);
        setStatus("status-connecting", "CONNECTING");
        state.client.activate();
        connectButton.disabled = false;
    }

    function disconnect() {
        unsubscribeFromSession();
        if (state.client) {
            state.client.deactivate();
            state.client = null;
        }
        state.sessionId = null;
        state.sessionReady = false;
        state.sessionKey = null;
        state.encryptionEnabled = false;
        updateSummary();
        setStatus("status-offline", "OFFLINE");
        connectButton.disabled = false;
    }

    async function sendMessage(event) {
        event.preventDefault();
        const content = messageInput.value.trim();
        if (!content || !state.client || !state.client.connected || !state.sessionReady || !state.sessionId) {
            if (content && state.client && state.client.connected && !state.sessionReady) {
                appendSystemLine("waiting for session initialization");
            }
            return;
        }

        const messageId = `msg-${crypto.randomUUID()}`;
        const encryptedMessage = await buildOutboundMessage(messageId, content);
        state.client.publish({
            destination: "/app/chat.send",
            body: JSON.stringify(encryptedMessage)
        });
        messageInput.value = "";
    }

    async function buildOutboundMessage(messageId, content) {
        if (!state.encryptionEnabled || !state.sessionKey) {
            return {
                messageId,
                encryptedPayload: content,
                iv: "development-mode",
                hmacSignature: "development-mode"
            };
        }

        const keyBytes = decodeBase64(state.sessionKey);
        const iv = crypto.getRandomValues(new Uint8Array(12));
        const cryptoKey = await crypto.subtle.importKey("raw", keyBytes, "AES-GCM", false, ["encrypt"]);
        const encryptedBuffer = await crypto.subtle.encrypt(
            { name: "AES-GCM", iv },
            cryptoKey,
            new TextEncoder().encode(content)
        );

        const encryptedPayload = encodeBase64(new Uint8Array(encryptedBuffer));
        const encodedIv = encodeBase64(iv);
        const hmacSignature = await signMessage(messageId, encryptedPayload, encodedIv, keyBytes);

        return {
            messageId,
            encryptedPayload,
            iv: encodedIv,
            hmacSignature
        };
    }

    async function resolveRenderedMessage(payload) {
        if (!state.encryptionEnabled || !state.sessionKey) {
            return payload.encryptedPayload;
        }

        try {
            const keyBytes = decodeBase64(state.sessionKey);
            const cryptoKey = await crypto.subtle.importKey("raw", keyBytes, "AES-GCM", false, ["decrypt"]);
            const decryptedBuffer = await crypto.subtle.decrypt(
                { name: "AES-GCM", iv: decodeBase64(payload.iv) },
                cryptoKey,
                decodeBase64(payload.encryptedPayload)
            );

            return new TextDecoder().decode(decryptedBuffer);
        } catch (error) {
            return "[decryption failed]";
        }
    }

    async function signMessage(messageId, encryptedPayload, iv, keyBytes) {
        const hmacKey = await crypto.subtle.importKey(
            "raw",
            keyBytes,
            { name: "HMAC", hash: "SHA-256" },
            false,
            ["sign"]
        );
        const payload = `${messageId}|${state.sessionId}|${state.tempUserId}|${encryptedPayload}|${iv}`;
        const signature = await crypto.subtle.sign("HMAC", hmacKey, new TextEncoder().encode(payload));
        return encodeBase64(new Uint8Array(signature));
    }

    function decodeBase64(value) {
        const binary = atob(value);
        const bytes = new Uint8Array(binary.length);
        for (let index = 0; index < binary.length; index += 1) {
            bytes[index] = binary.charCodeAt(index);
        }
        return bytes;
    }

    function encodeBase64(value) {
        let binary = "";
        for (let index = 0; index < value.length; index += 1) {
            binary += String.fromCharCode(value[index]);
        }
        return btoa(binary);
    }

    function formatClock(date) {
        return date.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false
        });
    }

    function resolveWsProtocol() {
        return window.location.protocol === "https:" ? "wss" : "ws";
    }

    async function bootstrapSession(tempUserId, joinToken) {
        try {
            const response = await fetch("/api/session/bootstrap", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    tempUserId,
                    joinToken
                })
            });

            if (response.status === 409) {
                appendSystemLine("chat id already active, choose another one");
                return { ok: false };
            }

            if (response.status === 429) {
                appendSystemLine("too many join attempts, slow down and retry");
                return { ok: false };
            }

            if (!response.ok) {
                appendSystemLine("bootstrap rejected, check chat id and join code format");
                return { ok: false };
            }

            const credentials = await response.json();
            return { ok: true, credentials };
        } catch (error) {
            appendSystemLine("bootstrap failed, network unavailable");
            return { ok: false };
        }
    }

    function isValidTempUserId(value) {
        return /^[A-Za-z0-9]{3,8}$/.test(value);
    }

    function isValidJoinToken(value) {
        return /^\d{4,8}$/.test(value);
    }

    function startExpirySweep() {
        state.expirySweepHandle = window.setInterval(removeExpiredMessages, 250);
    }

    function initMatrixRain() {
        const canvas = document.getElementById("matrix-rain");
        const context = canvas.getContext("2d", { alpha: true });
        if (!context) {
            return;
        }

        let width = 0;
        let height = 0;
        let fontSize = 16;
        let columns = 0;
        let drops = [];
        const glyphs = "01<>[]{}$#*+-/".split("");

        function resize() {
            width = canvas.width = window.innerWidth;
            height = canvas.height = window.innerHeight;
            columns = Math.max(12, Math.floor(width / fontSize));
            drops = Array.from({ length: columns }, () => Math.random() * height / fontSize);
        }

        function draw() {
            context.fillStyle = "rgba(2, 4, 2, 0.12)";
            context.fillRect(0, 0, width, height);
            context.fillStyle = "#39ff72";
            context.font = `${fontSize}px monospace`;

            for (let index = 0; index < columns; index += 1) {
                const glyph = glyphs[Math.floor(Math.random() * glyphs.length)];
                const x = index * fontSize;
                const y = drops[index] * fontSize;
                context.fillText(glyph, x, y);

                if (y > height && Math.random() > 0.985) {
                    drops[index] = 0;
                } else {
                    drops[index] += 0.45;
                }
            }

            window.requestAnimationFrame(draw);
        }

        resize();
        window.addEventListener("resize", resize, { passive: true });
        window.requestAnimationFrame(draw);
    }

    connectForm.addEventListener("submit", connect);
    disconnectButton.addEventListener("click", disconnect);
    messageForm.addEventListener("submit", sendMessage);
    messageInput.addEventListener("input", sendTypingEvent, { passive: true });
    ethicalAgreement.addEventListener("change", () => {
        enterButton.disabled = !ethicalAgreement.checked;
    });
    enterButton.addEventListener("click", finishGatewayEntry);
    introGateway.addEventListener("click", (event) => {
        if (!state.introReadyForEntry || !ethicalAgreement.checked) {
            return;
        }

        const interactiveTarget = event.target.closest("button, input, label");
        if (interactiveTarget && interactiveTarget !== enterButton) {
            return;
        }

        finishGatewayEntry();
    });

    startExpirySweep();
    initMatrixRain();
    runGatewaySequence();
    updateSummary();
    appendSystemLine("terminal ready");
})();
