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
- `Database/migrations/002_customer_portal_tables.sql`
- `Database/migrations/003_bot_runtime_billing_controls.sql`
- `Database/migrations/004_shop_feature_catalog.sql`
- `Database/README.md`

ตารางที่มี:

- `users`: เก็บ account หลัก เช่น email, username, password hash, age, avatar, role, status
- `oauth_accounts`: ผูก user กับ OAuth providers ได้แก่ Discord, Google, GitHub
- `discord_bots`: เก็บบอทของลูกค้า, owner, สถานะ runtime, PM2 process name, billing mode และราคา override ต่อเดือน
- `feature_catalog`: catalog feature/add-on รายเดือนสำหรับขายจริง เช่น runtime, shop orders, payment, Roblox seller, review credit, admin tools และ automation
- `bot_feature_subscriptions`: ผูก feature ที่ลูกค้าซื้อเข้ากับ bot แต่ละตัว
- `billing_subscriptions`: summary subscription ต่อ user สำหรับรอบบิลรายเดือน
- `payments`: payment record สำหรับต่อระบบชำระเงินจริงภายหลัง
- `payment_items`: รายการ feature ที่อยู่ใน payment แต่ละครั้ง
- `automation_settings`, `automation_runs`, `customer_notifications`: policy/audit/notification สำหรับ automation
- `bot_config_entries`: remote `.env`/config ต่อ bot เก็บ key-value และ mask ค่า secret ตอนส่งกลับ frontend

หมายเหตุ:

- `age` เป็น nullable แล้ว เพื่อรองรับ social login ที่ provider ไม่ส่งอายุมา
- ถ้าสร้าง DB จาก schema เก่าไปแล้ว ให้รัน migration:

```bash
mysql -u root -p < Database/migrations/001_allow_social_user_age_null.sql
mysql -u root -p discord_server_management < Database/migrations/002_customer_portal_tables.sql
mysql -u root -p discord_server_management < Database/migrations/003_bot_runtime_billing_controls.sql
mysql -u root -p < Database/migrations/004_shop_feature_catalog.sql
mysql -u root -p < Database/migrations/005_automation_engine.sql
mysql -u root -p < Database/migrations/006_promptpay_checkout.sql
mysql -u root -p < Database/migrations/007_test_package_10_baht.sql
mysql -u root -p < Database/migrations/008_bot_remote_config.sql
```

`004_shop_feature_catalog.sql` ทำงานสำคัญ:

- เปลี่ยน `feature_catalog.category` จากหมวด placeholder เดิมไปเป็นหมวดธุรกิจจริง: `SHOP`, `PAYMENT`, `ROBLOX`, `ENGAGEMENT`, `RUNTIME`, `ADMIN`, `AUTOMATION`, `SUPPORT`
- เพิ่ม field สำหรับการขายและโปรโมชัน:
  - `promotion_label`
  - `promotion_price_cents`
  - `promotion_ends_at`
  - `is_featured`
  - `sort_order`
- ปิด feature placeholder เก่า และ seed feature ที่แยกจาก bot จริงบน VM

Feature catalog ปัจจุบัน:

- `runtime-247`: hosting/PM2 runtime 24/7
- `test-package-10`: package 10 บาทสำหรับทดสอบ checkout/payment/webhook
- `shop-orders`: บันทึกออเดอร์สินค้า
- `shop-status`: ประกาศเปิด/ปิดร้าน
- `payment-embed`: payment panel/embed
- `promptpay-slipok`: PromptPay + SlipOK
- `truemoney-voucher`: TrueMoney voucher
- `wallet-credit`: wallet/credit/topup history
- `roblox-seller`: Roblox seller workflow
- `top-spender-rank`: ranking/top spender role
- `review-credit`: review counter/credit
- `admin-message-tools`: ส่งข้อความ/ไฟล์/DM ผ่านบอท
- `voice-keeper`: ให้บอทอยู่ voice 24/7
- `price-embed-cron`: auto refresh price embed ตามเวลา

## Backend

Backend ใช้ Spring Boot 4, Spring Data JDBC, Spring Security, OAuth2 Client, MySQL และ JWT

โครง folder สำคัญ:

- `auth/`: register, login, current user, token service
- `auth/dto/`: request/response DTO
- `config/security/`: Spring Security config และ JWT filter
- `config/web/`: CORS config
- `oauth/`: OAuth2 user upsert และ success/failure handlers
- `customer/`: customer dashboard API, bot/feature/billing DTO และ repository
- `admin/`: admin runtime operations และ admin shop management สำหรับผูก PM2 process, owner, ราคา, feature pricing และ promotion
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
PUT  /api/customer/bots/{botId}/config
POST /api/customer/checkout
GET  /api/customer/payments/{paymentId}
POST /api/webhooks/omise
GET  /api/admin/runtime/processes?hostId=...&userSearch=...
POST /api/admin/runtime/processes/{processName}/assignment?hostId=...
POST /api/admin/runtime/processes/{processName}/{action}?hostId=...
GET  /api/admin/shop/features
PUT  /api/admin/shop/features/{featureId}
GET  /api/admin/automation
PUT  /api/admin/automation/settings
POST /api/admin/automation/run
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
OMISE_PUBLIC_KEY=...
OMISE_SECRET_KEY=...
OMISE_API_BASE_URL=https://api.omise.co
PAYMENT_CHECKOUT_EXPIRATION_MINUTES=30
```

Runtime/VM notes:

- ตอนนี้ runtime รองรับ VM เดียวผ่าน SSH ไปที่ DigitalOcean แล้ว แต่ API มี `hostId` เพื่อเตรียมขยายหลาย VM ภายหลัง
- ห้ามให้ frontend เรียก VM ตรง ๆ ทุกอย่างต้องผ่าน Backend เพื่อเช็ค role และสิทธิ์
- `Admin Runtime` ใช้เฉพาะ user role `ADMIN`
- Backend ใช้ `pm2 jlist`, `pm2 start|stop|restart <processName>` และคำสั่ง Linux พื้นฐานเพื่ออ่าน disk/memory/load
- ก่อนเปิดใช้ production ควรใช้ SSH key เฉพาะ runtime user ที่จำกัดสิทธิ์ แทน root ถ้าเป็นไปได้
- จากการเช็ค VM ล่าสุด: disk เหลือเยอะ แต่ RAM ค่อนข้างตึงกว่า CPU/disk จึงควรมี alert/monitoring ก่อนเพิ่ม bot จำนวนมาก
- จัด PM2 บน VM ให้เป็นมาตรฐานแล้วโดยไม่ใส่ secret ใน ecosystem:
  - ecosystem config อยู่ที่ `/root/bot-runtime/ecosystem.config.cjs`
  - rollback script อยู่ที่ `/root/bot-runtime/rollback-pm2-before-ecosystem.sh`
  - ทุก bot เปลี่ยนจาก `npm start` wrapper ไปเป็น `node` entry จริง
  - หลัง migrate RAM available ดีขึ้นชัดเจน และ swap ใช้น้อยลงมาก

PM2 runtime mapping ปัจจุบัน:

- `bot-kanom-roblox` -> `/root/discord-bot-001-kanom-roblox/server.js`
- `bot-idaxdshop` -> `/root/discord-bot-002-idaxdshop/dist/index.js`
- `bot-akshop` -> `/root/discord-bot-003-akashop/src/index.js`
- `bot-kanom-price` -> `/root/discord-bot-004-kanom-price/src/index.js`

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
- `ShopView.vue`: customer shop สำหรับซื้อ feature เดี่ยวหรือ pack พร้อม announcement/promotion
- `AdminRuntimeView.vue`: admin runtime operations สำหรับเลือก VM, search process/bot/owner, assign owner, ตั้งราคา Free/Paid และควบคุม PM2
- `AdminShopManagementView.vue`: admin shop management สำหรับปรับราคา feature, promotion label, promotion price, featured/active status และ sort order
- `AboutView.vue`: project overview ที่ใช้ UI language เดียวกับ dashboard/admin

Auth state:

- `src/stores/authStore.ts`: เก็บ `accessToken`, `user`, `provider`
- Persist session ลง `localStorage`
- `refreshUser()` เรียก `/api/auth/me` เพื่อ validate token กับ backend

API service:

- `src/services/auth.ts`
- login/register
- current user
- social login redirect
- `src/services/customer.ts`
- customer dashboard
- admin runtime operations
- admin shop feature pricing/promotion management

Navigation guard:

- ตั้งใน `src/router/index.ts`
- `/` และ `/about` ต้อง authenticated
- `/admin/runtime` ต้อง authenticated และต้องเป็น role `ADMIN`
- `/admin/shop` ต้อง authenticated และต้องเป็น role `ADMIN`
- `/admin/automation` ต้อง authenticated และต้องเป็น role `ADMIN`
- `/login` และ `/register` เป็น guest-only
- ถ้า token หมดอายุหรือ invalid จะ clear session และ redirect ไป `/login`

Layout:

- หน้า `/login`, `/register`, `/auth/callback` ไม่แสดง sidebar/navbar
- Sidebar แสดง user name/email/avatar และปุ่ม logout
- Sidebar/Navbar ซ่อนเมนู admin จาก user ที่ไม่ใช่ `ADMIN`
- App background ปรับเป็น surface/grid แบบเดียวกันทั้งระบบ แทน background animation เดิม

Shop/pack behavior:

- หน้า Shop มี 2 โหมด:
  - `Packs`: รวม feature ที่มักขายด้วยกัน เช่น Starter Shop Bot, Payment Pro, Roblox Seller Pack, Operations Suite, Engagement Growth
  - `Features`: ซื้อ feature เดี่ยวและ filter ตามหมวด
- Pack price คำนวณจากราคา feature ปัจจุบัน และใช้ promotion price ถ้ามี
- Announcement panel ดึง feature ที่เป็น `featured` หรือมี `promotion_price_cents` มาแสดงเป็นจุดประกาศ
- ปุ่มซื้อยังเป็น UI placeholder รอเชื่อม payment/checkout flow จริง

Automation behavior:

- Migration: `Database/migrations/005_automation_engine.sql`
- Tables:
  - `automation_settings`: policy กลาง เช่น enable, grace day, reminder day, runtime suspend
  - `automation_runs`: audit log ของ manual/scheduled run
  - `customer_notifications`: notification queue สำหรับ billing/feature/runtime event
- Backend endpoint:
  - `GET /api/admin/automation`
  - `PUT /api/admin/automation/settings`
  - `POST /api/admin/automation/run`
- Scheduled run: ทุกวัน 09:00 ตาม `Asia/Bangkok`
- งานที่ automation ทำ:
  - แจ้งเตือน billing ก่อน renewal ตาม `automation.reminder_days_before`
  - เปลี่ยน billing subscription เป็น `PAST_DUE` หลังครบ `automation.past_due_grace_days`
  - เปลี่ยน feature subscription เป็น `PAST_DUE`
  - cancel feature subscription หลังครบ `automation.cancel_grace_days`
  - stop PM2 runtime เฉพาะเมื่อเปิด `automation.runtime_suspend_enabled`
- ค่าเริ่มต้น `automation.runtime_suspend_enabled=false` เพื่อกันการหยุด bot จริงโดยไม่ตั้งใจ
- Frontend page: `/admin/automation` สำหรับดู summary, ตั้ง policy, กด manual run และดู recent runs

Payment behavior:

- Migration: `Database/migrations/006_promptpay_checkout.sql`
- Test package migration: `Database/migrations/007_test_package_10_baht.sql`
- Remote config migration: `Database/migrations/008_bot_remote_config.sql`
- Provider MVP: `OMISE_PROMPTPAY`
- Backend endpoint:
  - `POST /api/customer/checkout`: สร้าง PromptPay checkout จาก feature/pack และ bot ที่เลือก
  - `GET /api/customer/payments/{paymentId}`: refresh สถานะ checkout ของลูกค้า
  - `POST /api/webhooks/omise`: webhook public สำหรับ Omise event `charge.complete`
- ถ้า backend มี `OMISE_PUBLIC_KEY` และ `OMISE_SECRET_KEY` ที่ขึ้นต้นด้วย `pkey_`/`skey_` จะเรียก Omise API จริง
- ถ้ายังไม่ตั้ง key จะใช้ mock PromptPay QR สำหรับ local development เพื่อให้ frontend/backend flow ทดสอบได้
- เมื่อ charge status เป็น `successful`:
  - payment เปลี่ยนเป็น `PAID`
  - สร้าง billing subscription รอบ 1 เดือน
  - เปิด/ต่ออายุ `bot_feature_subscriptions` ให้ bot ที่เลือก
- Frontend Shop เลือก bot เป้าหมายก่อนซื้อ feature เดี่ยวหรือ pack แล้ว redirect ไป `/checkout/{paymentId}`
- หน้า Checkout เป็น dedicated payment page มี order summary, PromptPay QR, reference, expiry, status และ refresh payment
- มี `Test Package` ราคา 10 บาทบนหน้า Shop สำหรับทดสอบ payment flow แบบ low-value
- Omise PromptPay จริงมีขั้นต่ำ 20 บาท; checkout ที่ต่ำกว่า 20 บาทจะใช้ mock QR เพื่อทดสอบ flow ภายในระบบเท่านั้น

Dashboard / Runtime behavior:

- Customer Dashboard โฟกัสเป็น bot remote control มากขึ้น:
  - แสดง runtime expiry และ countdown ต่อ bot
  - แสดง feature expiry ต่อ feature
  - แก้ remote `.env`/config ได้หลาย key ต่อ bot เช่น `DISCORD_TOKEN`, `CLIENT_ID`, `GUILD_ID`, `COMMAND_PREFIX`, role/channel ids, payment ids, Roblox ids/cookie
  - เพิ่ม custom config key จากหน้า Dashboard ได้เอง
  - config key ที่เป็น token/secret/password/api key จะถูก mask ตอนส่งกลับ frontend
- Admin Runtime ปรับ owner, billing mode, ราคา/เดือน และ runtime expiry ต่อ bot ได้

ข้อสำคัญของ remote config:

- ตอนนี้ระบบเก็บค่า config ใน DB แล้ว แต่ยังไม่ได้ sync กลับไปเขียน `.env` บน VM หรือ restart PM2 อัตโนมัติ
- รอบถัดไปควรเพิ่ม backend job/API สำหรับ deploy config ไปยัง VM แบบ atomic:
  - render `.env` ต่อ bot จาก `bot_config_entries`
  - backup `.env` เดิม
  - เขียนไฟล์ใหม่ด้วย permission จำกัด
  - restart PM2 process
  - rollback ถ้า restart fail

Bot source snapshots:

- ดึง source snapshot จาก VM เข้าโฟลเดอร์ `Bots/` แล้ว เพื่อใช้แยก feature และออกแบบระบบขาย feature/pack
- ตอนนี้ `Bots/` ถูกตั้งเป็น local-only snapshot ยังไม่ push bot code จนกว่าจะ optimize/clean แล้ว
- โครงหลักตอนนี้:
  - `Bots/discord-bot-001-kanom-roblox` - Roblox topup/payment bot
  - `Bots/discord-bot-002-idaxdshop` - shop/credit/order/status/voice bot
  - `Bots/discord-bot-003-akashop` - review/credit bot
  - `Bots/discord-bot-004-kanom-price` - price embed/scheduled message bot
- ตอนดึงไฟล์ exclude `node_modules`, `.env`, `.env.*`, `.git`, logs และ pid files แล้ว
- `.gitignore` ตอนนี้ ignore `Bots/**` ทั้งหมด ยกเว้น `Bots/README.md` เพื่อกันไม่ให้ source/runtime data ของ bot ติด push ก่อนพร้อม
- สแกน token/secret รอบแรกแล้ว เจอเฉพาะการอ้างถึง env variable ใน code/README เช่น `DISCORD_TOKEN`, `ROBLOX_TOTP_SECRET` ยังไม่เจอค่าลับจริงจาก `.env`
- มี `Bots/README.md` สำหรับกติกาความปลอดภัยของ snapshot
- ขั้นถัดไปที่ควรทำ:
  - แยก feature definitions จาก bot เหล่านี้ เช่น topup, slip/payment check, credit wallet, order tracking, status embed, voice keeper, price embed scheduler
  - map feature แต่ละตัวเข้ากับ required variables เช่น token, guild id, channel id, role id, payment config, Roblox config
  - ทำ contract ให้ bot runtime ดึง feature entitlement + variables จาก backend แทนการ hardcode config ในไฟล์ JSON/`.env`
  - ค่อยเลือก migrate code ที่ optimize แล้วออกจาก `Bots/` เข้าส่วน product จริง

Repository guardrails:

- เพิ่ม root `.gitignore` ให้ครอบคลุม secret, key/cert, dependency, build output, Maven target, IDE files, local database dump และ bot runtime data
- ตั้งใจให้ `Bots/**` เป็น local-only snapshot ไม่ push bot code จนกว่าจะ cleanup/optimize
- เพิ่ม `AGENTS.md` สำหรับ agent/Codex รอบถัดไป:
  - อธิบายโครง `Database`, `Backend`, `Discord-Server-Management`, `Bots`
  - ระบุ safety rules เรื่อง secret และ production/customer data
  - ระบุ command หลักสำหรับ backend/frontend verification
  - ระบุแนวทาง bot feature productization และ git commit style

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
- จัด PM2 บน VM เป็น ecosystem มาตรฐานพร้อม rollback script และลด npm wrapper
- แยก feature จาก bot จริงบน VM เป็น feature catalog สำหรับขาย
- เพิ่มหน้า Shop แบบ feature เดี่ยวและ pack พร้อม announcement/promotion
- เพิ่ม Admin Shop Management สำหรับจัดการราคา promotion featured active และ sort order
- ปรับ UI ทุกหน้าให้ใช้ visual language เดียวกันมากขึ้น เช่น background, heading scale, panel/card, button, notice และ auth pages
- แยก push เป็น database/backend/frontend commits ตาม pattern
- เพิ่มระบบ Automation สำหรับ billing reminder, past due, feature cancellation, notification audit และหน้า Admin Automation Center
