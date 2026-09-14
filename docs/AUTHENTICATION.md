# Microsoft sign-in setup

CowClient does not ship another launcher's Microsoft application ID and does not accept passwords, session tokens or client secrets in its settings.

Before distributing the standalone launcher with sign-in enabled, the project owner must register a public-client Microsoft application that supports personal Microsoft accounts, enable public-client/device-code authentication, and obtain any required Minecraft services API approval. Registration alone does not guarantee Minecraft API access.

Add that application's **Application (client) ID** in CowClient > Settings. It is a public identifier, not a secret. Never paste your Microsoft password or a client secret there. The application name shown on Microsoft's consent page should identify CowClient.

The launcher opens a device-code flow against Microsoft's consumers endpoint with XboxLive.signin and offline_access. Sign in only on Microsoft's website and only when you started the flow. It exchanges the resulting access token through Xbox Live, XSTS and Minecraft services, then checks Java entitlement and retrieves the Minecraft profile. Family, age, privacy and entitlement restrictions are respected; there is no bypass or cracked-account fallback.

A refresh token is encrypted with Windows DPAPI CurrentUser and stored under %LOCALAPPDATA%/CowClient/account.dpapi. Sign out removes that file. Passwords are never collected. The game access token is held in memory and passed to Minecraft as required by the normal launch protocol. No authentication tokens are uploaded to GitHub or telemetry.

## Verification boundary

Automated tests do not log into a real Microsoft account. Until the project's approved application ID is configured and a real account test succeeds, sign-in/entitlement/online-play behavior remains unverified. The Fabric jar can be tested independently through an existing legitimate launcher.

## References

- [Microsoft's device authorization flow](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code)
- [Microsoft app registration](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app)
- [CmlLib launcher source and API](https://github.com/CmlLib/CmlLib.Core)
