# Pull Request

## Jira Ticket

- [BH-99](https://tranngochuyen.atlassian.net/browse/BH-99)
- [BH-102](https://tranngochuyen.atlassian.net/browse/BH-102)
- [BH-103](https://tranngochuyen.atlassian.net/browse/BH-103)
- [BH-106](https://tranngochuyen.atlassian.net/browse/BH-106)

---

## Description

Backend implementation for the User Settings feature. Included building out APIs to manage general settings, active device sessions, and account security. Added functionality for updating passwords, querying login devices, remotely revoking sessions, and updating user schemas across the board.

---

## Type of change

Please select the appropriate option:

- [ ] Bug fix
- [x] New feature
- [ ] UI / UX update
- [x] Refactor
- [ ] Other (please describe)

---

## What was changed?

- **`UserSettingController` Details**: Introduced a comprehensive suite of API endpoints to handle setting state updates without loading entire user documents (utilizing MongoDB dot notation). Includes endpoints for getting user settings (`/me`, `/{userId}`) and retrieving specific blocks (`/me/general`, `/me/privacy`, `/me/notification`). Features highly specific PUT endpoints for granular toggles: `/general`, `/security`, `/privacy`, `/sync`, `/appearance`, `/message`, `/notification`, and `/utilities`. Provided a `/me/reset` endpoint to restore global defaults.
- **`DeviceController` Details**: Implemented complete session management over the `/auth/devices` route. Includes endpoints to register devices during login (`POST /auth/devices`), retrieve instances (`GET` by ID, Session ID, or Account ID), and fetch currently active session footprints directly by inspecting incoming JWT context (`GET /active-sessions`). Empowered robust remote logout options via explicitly defined `DELETE` endpoints for a solitary instance (`/{id}`) or globally purging an account (`/account/{accountId}`).
- Handled Authentication interactions including a new API for Change Password, and added device-level tracking on login/logout processes.
- Refactored `User`, `Account`, and `Outbox` entity classes to ensure models are correctly using `FieldType.ObjectId` for their ID properties.
- Enhanced global CORS configurations and modified token-store/session repositories.
- Re-organized Request and Response DTOs into specific packages (e.g., separating user models). 
- Updated Postman documentation (`BondHub_Auth_API.postman_collection.json`).

---

## Screenshots / Video

> Required if there are UI changes

---

## How Has This Been Tested? (Manual)

1. Start Gateway, Auth-Service, and User-Service locally.
2. Login to retrieve access tokens.
3. Verify:
   - [ ] Hitting the Change Password API updates credentials properly and invalidates existing sessions if required.
   - [ ] Login records a new session under the Device API, and it can be fetched successfully.
   - [ ] Terminating an active device session properly throws unauthorized when attempting to use its token.
   - [ ] Hitting user setting PUT endpoints successfully persists settings to MongoDB.

---

## Risk Level

- [ ] Low – UI-only or safe change
- [ ] Medium – affects logic or state
- [x] High – affects authentication or data flow

---

## Checklist

- [x] PR title contains `[BH-99]`, `[BH-102]`, `[BH-103]`, `[BH-106]`
- [x] Commit messages follow the convention
- [x] Jira ticket is linked
- [x] API documentation/Postman collections are updated
- [x] Unit/Manual backend testing completed
