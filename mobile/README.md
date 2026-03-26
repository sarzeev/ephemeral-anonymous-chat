# XYRON Mobile Wrapper

This folder contains the isolated mobile packaging for XYRON. Native projects, mobile web assets, and build setup all live under `mobile/` so the desktop web app can stay separate.

## Before building

1. Deploy the Spring Boot backend to a public HTTPS URL.
2. Update `mobile/web/mobile-config.js` and replace `https://your-deployed-chat-backend.example.com` with that deployed backend URL.

## Build setup

1. From `/mobile`, run `npm install`.
2. Run `npx cap add android`.
3. Run `npx cap add ios`.
4. Run `npx cap sync`.

## Open native projects

- Android Studio: `npm run open:android`
- Xcode: `npm run open:ios`

## Notes

- The packaged mobile web assets live in `mobile/web`.
- Android will use `https://localhost` as the local WebView origin.
- iOS will use `capacitor://localhost`, which is already allowed by the backend WebSocket config.
