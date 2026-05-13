# AGENTS.md

คู่มือสั้นสำหรับ Codex/agent ที่มาทำงานต่อใน repo นี้

## Project Shape

- `Database/` - MySQL schema และ migration ของระบบ SaaS จัดการ Discord bot
- `Backend/` - Spring Boot backend, JWT auth, OAuth, customer dashboard, admin runtime, automation, payment
- `Discord-Server-Management/` - Vue/Vite frontend สำหรับ customer/admin portal
- `Bots/` - local snapshot source ของ bot จริงจาก VM ใช้เพื่อวิเคราะห์ feature และออกแบบ productization ยังไม่ push โค้ดบอทจนกว่าจะ optimize/clean

## Safety Rules

- ห้าม commit `.env`, `.env.*`, token, private key, payment secret, OAuth secret, Discord bot token หรือ production config
- ห้าม commit `node_modules`, `target`, `dist` ทั่วไป, logs, runtime data และ customer data
- `Bots/**` ถูก ignore ทั้งหมด ยกเว้น `Bots/README.md`; ถ้าจะ push bot code ต้องทำ cleanup/optimization และขออนุญาตก่อน
- ไฟล์ JSON runtime ของ bot เช่น `data/*.json`, `config.json`, `embeds.json` ให้ถือเป็น production/customer data แม้จะอยู่ใน local snapshot
- ก่อน push ให้รัน secret scan อย่างน้อย:

```bash
rg --no-ignore -n "(TOKEN|SECRET|PASSWORD|API_KEY|CLIENT_SECRET|BOT_TOKEN|DISCORD_TOKEN|sk_live|skey_|pkey_|mongodb|mysql://|postgres://|redis://|OMISE|PRIVATE|WEBHOOK)" .
```

## Development Commands

Backend:

```bash
cd Backend
./mvnw test
```

Frontend:

```bash
cd Discord-Server-Management
npm install
npm run build
```

Diff hygiene:

```bash
git diff --check
git status --short
```

## Implementation Notes

- ใช้ migrations ใน `Database/migrations/` สำหรับ database change ใหม่ อย่าแก้ production data ด้วยมือเปล่า
- Backend ใช้ DTO/service/controller แยกชั้นตาม pattern ที่มีอยู่แล้ว
- Frontend ใช้ visual language เดียวกันกับ Dashboard/Admin/Shop ปัจจุบัน อย่าสร้าง landing page ถ้าเป็นหน้าระบบใช้งานจริง
- Payment MVP ใช้ Omise PromptPay และมี mock flow เมื่อยังไม่มี key หรือยอดต่ำกว่า minimum จริง
- Automation ค่า default ต้องปลอดภัย โดยเฉพาะการ suspend runtime ต้องไม่เปิดเองถ้า admin ยังไม่ตั้งใจ
- Runtime/VM action ต้องคิดเรื่อง rollback เสมอ เช่น backup config, restart PM2, verify, rollback เมื่อ fail

## Bot Snapshot Direction

เป้าหมายของ `Bots/` ไม่ใช่การ deploy หรือ push จาก repo นี้ทันที แต่ใช้เพื่อแยก feature เป็นสินค้า:

- Feature เดี่ยว เช่น topup, slip check, credit wallet, order tracking, status embed, voice keeper, price embed scheduler
- Pack เช่น starter shop bot, payment pro, Roblox seller pack, operations suite
- ทุก feature ควรมี required variables ชัดเจน เช่น Discord token, guild id, channel id, role id, payment config, Roblox config
- ระยะถัดไปควรให้ bot runtime ดึง entitlement และ variables จาก backend แทน hardcode ใน `.env`/JSON
- ก่อนย้าย code จาก `Bots/` เข้า repo จริง ต้อง remove secret/runtime state, ลด duplicate, แยก module, เพิ่ม config contract และทดสอบ start/build

## Git Style

ใช้ Conventional Commit และแยก scope เมื่อ push:

```txt
feat(database): ...
feat(backend): ...
feat(frontend): ...
fix(database): ...
fix(backend): ...
fix(frontend): ...
docs(project): ...
```

ถ้าผู้ใช้ขอ push แบบแยกส่วน ให้ใช้ skill `split-project-push`.
