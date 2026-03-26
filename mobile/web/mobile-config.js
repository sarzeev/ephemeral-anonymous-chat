(function () {
    const nativePlatform = Boolean(
        window.Capacitor &&
        typeof window.Capacitor.isNativePlatform === "function" &&
        window.Capacitor.isNativePlatform()
    );

    window.XYRON_CONFIG = Object.freeze({
        nativePlatform,
        backendOrigin: nativePlatform ? "https://ephemeral-anonymous-chat-1.onrender.com" : null
    });
})();
