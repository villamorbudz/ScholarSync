# ScholarSync API Documentation

## Base URL
- `http://localhost:8080`

## Authentication
- Method: Microsoft OAuth 2.0 (Authorization Code Flow)
- Domain restriction: only `@cit.edu` emails
- Session: `JSESSIONID` cookie (created on OAuth login)
- JWT: `SESSION_TOKEN` HttpOnly cookie (also accepted via `Authorization: Bearer <token>`)

## Endpoints

### Auth
- `GET /login`
  - Serves custom login page with “Sign in with Microsoft”.
  - Public.

- `GET /login/oauth2/authorization/microsoft`
  - Starts Microsoft OAuth flow.
  - Public.

- `GET /login/oauth2/code/microsoft`
  - OAuth callback (handled by Spring Security).
  - Public (framework endpoint).

- `GET /api/auth/success`
  - Returns auth success payload and user info (uses session data set by success handler).
  - Requires authentication (session/JWT).
  - Response (success):
    ```json
    {
      "success": true,
      "message": "Signed in successfully",
      "isNewUser": false,
      "user": {
        "id": "uuid",
        "email": "user@cit.edu",
        "displayName": "User Name",
        "role": "STUDENT|TEACHER|ADMIN|UNKNOWN",
        "institutionalId": "22-1234-567|1643|null",
        "microsoftId": "microsoft-object-id",
        "accountCreatedAt": "2024-01-01T00:00:00",
        "lastLoginAt": "2024-01-01T00:00:00"
      },
      "microsoftProfile": {
        "id": "microsoft-object-id",
        "displayName": "User Name",
        "mail": "user@cit.edu",
        "userPrincipalName": "user@cit.edu",
        "jobTitle": "22-1234-567"
      }
    }
    ```

- `GET /api/auth/me`
  - Returns current authenticated user (session or JWT).
  - Requires authentication.
  - Response:
    ```json
    {
      "id": "uuid",
      "email": "user@cit.edu",
      "displayName": "User Name",
      "role": "STUDENT|TEACHER|ADMIN|UNKNOWN",
      "institutionalId": "22-1234-567|1643|null"
    }
    ```

- `POST /api/auth/logout`
  - Invalidates session, clears `JSESSIONID`.
  - Requires authentication.
  - Response:
    ```json
    { "success": true, "message": "Logged out successfully" }
    ```

### Test (public)
- `POST /api/public/test/role`
  - Body: `{ "institutionalId": "<id>" }`
  - Returns determined role and matched pattern.
  - Public (intended for testing; disable in production).

## Roles & Institutional ID Patterns
- Student:
  - `YY-####-###` (e.g., `22-1234-567`)
  - `YYYY-#####` (e.g., `2010-12345`)
- Teacher:
  - Plain number, 1–4 digits (e.g., `1`, `12`, `133`, `1643`)
- Admin:
  - Contains `ADMIN` or `ADMINISTRATOR`
- Otherwise: `UNKNOWN`

## JWT Contents
- Subject: `sub` = user UUID
- Claims:
  - `email`
  - `role` (`STUDENT|TEACHER|ADMIN|UNKNOWN`)
  - `auth_method` = `MICROSOFT_OAUTH`
  - `microsoftId`
  - `iat`, `exp` (default 30 minutes; `jwt.expiration`)
- Not included: `institutionalId`, passwords, displayName

## Required Config
- Environment:
  - `JWT_SECRET_BASE64` (Base64-encoded signing key for JWT)
- OAuth:
  - Tenant: `823cde44-4433-456d-b801-bdf0ab3d41fc`
  - Client ID: `f7de2082-19fd-4c38-b76a-07eefc673493`
  - Scopes: `openid, profile, email, User.Read, offline_access`

## Security Rules
- Public: `/`, `/login`, `/static/**`, `/error`, `/login/oauth2/**`, `/oauth2/**`, `/api/public/**`
- Protected: all other endpoints (session or JWT)

## Notes
- Domain restriction enforced (`@cit.edu` only).
- Auto-registration on first OAuth login; profile and role re-evaluated each login.
- `institutionalId` is unique and stored from Microsoft Graph `jobTitle` or extracted from `given_name` as fallback.

