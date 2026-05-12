# Discord Server Management AI Context

เอกสารนี้เป็น context สำหรับ AI หรือผู้พัฒนาที่เข้ามาทำงานในโปรเจกต์นี้ เพื่อให้เห็นว่าโปรเจกต์ทำอะไรไปแล้ว มีโครงสร้างแบบไหน และควรต่อยอดจากจุดไหนโดยไม่ทำงานซ้ำหรือแก้ผิดทิศ

## Project Structure

- `Discord-Server-Management/`: Frontend Vue/Vite/Tailwind app
- `Backend/`: Spring Boot backend
- `Database/`: MySQL schema และ migration

## Database

สร้างฐานข้อมูล MySQL ชื่อ `discord_server_management`

ไฟล์หลัก:

- `Database/schema.sql`
- `Database/migrations/001_allow_social_user_age_null.sql`
- `Database/README.md`

ตารางที่มี:

- `users`: เก็บ account หลัก เช่น email, username, password hash, age, avatar, role, status
- `oauth_accounts`: ผูก user กับ OAuth providers ได้แก่ Discord, Google, GitHub
- `discord_bots`: เก็บบอทของลูกค้า, owner, สถานะ runtime, PM2 process name, billing mode และราคา override ต่อเดือน
- `feature_catalog`: catalog feature/add-on รายเดือน เช่น moderation, ticket, music, analytics, AI
- `bot_feature_subscriptions`: ผูก feature ที่ลูกค้าซื้อเข้ากับ bot แต่ละตัว
- `billing_subscriptions`: summary subscription ต่อ user สำหรับรอบบิลรายเดือน
- `payments`: payment record สำหรับต่อระบบชำระเงินจริงภายหลัง

หมายเหตุ:

- `age` เป็น nullable แล้ว เพื่อรองรับ social login ที่ provider ไม่ส่งอายุมา
- ถ้าสร้าง DB จาก schema เก่าไปแล้ว ให้รัน migration:

```bash
mysql -u root -p < Database/migrations/001_allow_social_user_age_null.sql
mysql -u root -p discord_server_management < Database/migrations/002_customer_portal_tables.sql
mysql -u root -p discord_server_management < Database/migrations/003_bot_runtime_billing_controls.sql
```

## Backend

Backend ใช้ Spring Boot 4, Spring Data JDBC, Spring Security, OAuth2 Client, MySQL และ JWT

โครง folder สำคัญ:

- `auth/`: register, login, current user, token service
- `auth/dto/`: request/response DTO
- `config/security/`: Spring Security config และ JWT filter
- `config/web/`: CORS config
- `oauth/`: OAuth2 user upsert และ success/failure handlers
- `customer/`: customer dashboard API, bot/feature/billing DTO และ repository
- `admin/`: admin runtime operations สำหรับผูก PM2 process กับ owner และตั้งราคา
- `runtime/`: PM2 runtime bridge ผ่าน SSH ไปยัง VM
- `oauth/domain/`, `oauth/repository/`: OAuth account model/repository
- `user/domain/`, `user/repository/`: User model/repository
- `common/error/`: error response กลาง

Endpoints:

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
GET  /api/customer/dashboard
GET  /api/admin/runtime/processes?hostId=...&userSearch=...
POST /api/admin/runtime/processes/{processName}/assignment?hostId=...
POST /api/admin/runtime/processes/{processName}/{action}?hostId=...
```

OAuth endpoints จาก Spring Security:

```http
GET /oauth2/authorization/github
GET /oauth2/authorization/google
GET /oauth2/authorization/discord
```

OAuth callback URLs ที่ต้องใส่ใน provider dashboards:

```txt
http://localhost:8080/login/oauth2/code/github
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/discord
```

JWT:

- Backend สร้าง signed JWT ด้วย `JWT_SECRET`
- Frontend ส่ง token ด้วย `Authorization: Bearer <token>`
- `JwtAuthenticationFilter` verify token และโหลด user จาก database
- `/api/auth/me` ใช้ JWT เพื่อคืนข้อมูล user ปัจจุบัน

Environment:

- Local secrets อยู่ใน `Backend/.env`
- Template อยู่ใน `Backend/.env.example`
- `Backend/.env` ถูก ignore และห้าม commit

ตัวแปรสำคัญ:

```env
DB_URL=jdbc:mysql://localhost:3306/discord_server_management
DB_USERNAME=root
DB_PASSWORD=...
APP_CORS_ALLOWED_ORIGIN=http://localhost:5173,http://127.0.0.1:5173,http://127.0.0.1:5174
FRONTEND_URL=http://localhost:5173
JWT_SECRET=...
JWT_EXPIRATION_SECONDS=86400
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
DISCORD_CLIENT_ID=...
DISCORD_CLIENT_SECRET=...
BOT_RUNNER_SSH_USER=root
BOT_RUNNER_SSH_HOST=...
BOT_RUNNER_SSH_PORT=22
BOT_RUNNER_SSH_KEY_PATH=
BOT_RUNNER_PM2_BINARY=pm2
BOT_RUNNER_NAME=DigitalOcean Primary
BOT_RUNNER_REGION=sgp1
```

Runtime/VM notes:

- ตอนนี้ runtime รองรับ VM เดียวผ่าน SSH ไปที่ DigitalOcean แล้ว แต่ API มี `hostId` เพื่อเตรียมขยายหลาย VM ภายหลัง
- ห้ามให้ frontend เรียก VM ตรง ๆ ทุกอย่างต้องผ่าน Backend เพื่อเช็ค role และสิทธิ์
- `Admin Runtime` ใช้เฉพาะ user role `ADMIN`
- Backend ใช้ `pm2 jlist`, `pm2 start|stop|restart <processName>` และคำสั่ง Linux พื้นฐานเพื่ออ่าน disk/memory/load
- ก่อนเปิดใช้ production ควรใช้ SSH key เฉพาะ runtime user ที่จำกัดสิทธิ์ แทน root ถ้าเป็นไปได้
- จากการเช็ค VM ล่าสุด: disk เหลือเยอะ แต่ RAM ค่อนข้างตึงกว่า CPU/disk จึงควรมี alert/monitoring ก่อนเพิ่ม bot จำนวนมาก

Run backend:

```bash
cd Backend
set -a
source .env
set +a
./mvnw spring-boot:run
```

Verify backend:

```bash
cd Backend
./mvnw test
```

## Frontend

Frontend ใช้ Vue, Vue Router, Pinia, Tailwind และ lucide icons

หน้าที่ทำแล้ว:

- `LoginView.vue`: email/password login และ social login buttons
- `RegisterView.vue`: email/username/password/age register และ social login buttons
- `AuthCallbackView.vue`: รับผล OAuth callback จาก backend
- `HomeView.vue`: customer dashboard สำหรับดู bot, billing summary, feature catalog และสถานะ customer workspace
- `AdminRuntimeView.vue`: admin runtime operations สำหรับเลือก VM, search process/bot/owner, assign owner, ตั้งราคา Free/Paid และควบคุม PM2

Auth state:

- `src/stores/authStore.ts`: เก็บ `accessToken`, `user`, `provider`
- Persist session ลง `localStorage`
- `refreshUser()` เรียก `/api/auth/me` เพื่อ validate token กับ backend

API service:

- `src/services/auth.ts`
- login/register
- current user
- social login redirect

Navigation guard:

- ตั้งใน `src/router/index.ts`
- `/` และ `/about` ต้อง authenticated
- `/admin/runtime` ต้อง authenticated และต้องเป็น role `ADMIN`
- `/login` และ `/register` เป็น guest-only
- ถ้า token หมดอายุหรือ invalid จะ clear session และ redirect ไป `/login`

Layout:

- หน้า `/login`, `/register`, `/auth/callback` ไม่แสดง sidebar/navbar
- Sidebar แสดง user name/email/avatar และปุ่ม logout

Frontend env:

- Template: `Discord-Server-Management/.env.example`

```env
VITE_API_BASE_URL=http://localhost:8080
```

Run frontend:

```bash
cd Discord-Server-Management
npm install
npm run dev
```

Verify frontend:

```bash
cd Discord-Server-Management
npm run build
```

## OAuth Provider Setup Notes

GitHub:

- Create OAuth app at `https://github.com/settings/developers`
- Homepage URL: `http://localhost:5173`
- Callback URL: `http://localhost:8080/login/oauth2/code/github`

Google:

- Use External audience for local testing
- Create Web application OAuth client
- Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
- Add your Gmail as test user while app is in testing mode

Discord:

- Create app at `https://discord.com/developers/applications`
- Add redirect URL: `http://localhost:8080/login/oauth2/code/discord`
- `DISCORD_CLIENT_SECRET` must be the real OAuth2 client secret, not the client id

## Git Commit Pattern

ใช้ Conventional Commit แยก scope:

```txt
feat(database): ...
feat(backend): ...
feat(frontend): ...
fix(database): ...
fix(backend): ...
fix(frontend): ...
```

มี Codex skill สำหรับ workflow นี้แล้ว:

```txt
~/.codex/skills/split-project-push
```

คำสั่งที่ใช้เรียกครั้งหน้า:

```txt
ใช้ split-project-push แล้ว push แยก Database Backend Frontend ให้หน่อย
```

## Completed Work So Far

- สร้าง MySQL schema สำหรับ user และ OAuth account
- สร้าง backend auth endpoints
- เพิ่ม social login flow สำหรับ GitHub, Google, Discord
- เพิ่ม JWT authentication จริงใน backend
- เพิ่ม frontend auth store, localStorage persistence, route guard
- ต่อ login/register form กับ backend
- ทำ OAuth callback page
- ซ่อน sidebar ในหน้า auth
- เพิ่ม dashboard เบื้องต้นสำหรับดู session/user info
- เพิ่ม customer portal dashboard สำหรับ SaaS จัดการ Discord bot รายเดือน
- เพิ่ม database schema/migrations สำหรับ bot, feature catalog, feature subscription, billing subscription และ payments
- เพิ่ม backend customer dashboard API
- เพิ่ม backend admin runtime API สำหรับ PM2 process assignment, pricing และ runtime control
- เพิ่ม backend runtime bridge ผ่าน SSH ไปยัง DigitalOcean VM
- เพิ่มหน้า Admin Runtime แบบ enterprise operations มี VM selector, bot/process search, owner search แบบ server-side, health cards และ action ต่อ process
- แยก push เป็น database/backend/frontend commits ตาม pattern
