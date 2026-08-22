# Driver app

The driver app uses Keycloak OIDC Authorization Code + PKCE and the API Gateway.
Copy `.env.example` to a local environment file and adjust the Gateway and
Keycloak host addresses for the device or emulator.

Metro uses port `8082`; the API Gateway remains on `8081`. The `start`, `dev`,
and `web` scripts already use Metro port `8082`.

For Android emulators, run `npm run dev:android`. It configures the Keycloak
reverse tunnel (`tcp:8180`) for every connected emulator in `device` state,
then starts the Expo development client on Metro port `8082`. It does not
rebuild the native app.

When running Docker for a device, set the root `KEYCLOAK_PUBLIC_URL` to the
same reachable base URL before starting Compose, and use that value as
`EXPO_PUBLIC_KEYCLOAK_BASE_URL`. The backend issuer and Keycloak discovery must
use the same public host.

## Development build

Background location is not available in Expo Go. Install the native development
client after changing native configuration:

```bash
npx expo run:android
npm run dev
```

The EAS `development` profile is also configured in `eas.json`.
Remote push notifications require that development build and an Expo/EAS
project configuration; Expo Go is not a valid remote-push test target.

## Keycloak client

Create a public OIDC client named `food-delivery-driver-mobile` in realm
`food-delivery` with:

- Standard Flow enabled
- Client authentication disabled
- PKCE method `S256`
- Redirect URI `fooddeliverydriver://oauth/callback`
- Post-logout redirect URI `fooddeliverydriver://oauth/callback`

Google sign-in is requested through Keycloak with the `google` IdP alias. The
Google OAuth secret belongs only in Keycloak, never in this app.

Offer and active-delivery state always refreshes from delivery-service after a
push signal, foreground resume, or app restart. Push data is never the state
authority.
