# Bot Source Snapshots

This directory keeps source snapshots copied from the production bot VM for feature analysis and future productization.

Safety rules:

- Do not commit `.env`, `.env.*`, `node_modules`, logs, PM2 state, or local runtime files.
- Runtime/customer JSON data under `data/` and `update/` is ignored by git.
- Treat values such as Discord channel ids, user ids, payment history, and wallet state as production data, not source code.
- Use these snapshots to identify reusable features, config variables, and migration paths into the SaaS backend.

