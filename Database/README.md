# Database

Initial MySQL schema for Discord Server Management.

Run:

```bash
mysql -u root -p < Database/schema.sql
```

If you already created the tables before social login support was added, run:

```bash
mysql -u root -p < Database/migrations/001_allow_social_user_age_null.sql
```

Tables:

- `users`: base account data for email/password registration.
- `oauth_accounts`: links Discord, Google, and GitHub identities to one user.
