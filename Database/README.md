# Database

Initial MySQL schema for Discord Server Management.

Run:

```bash
mysql -u root -p < Database/schema.sql
```

Tables:

- `users`: base account data for email/password registration.
- `oauth_accounts`: links Discord, Google, and GitHub identities to one user.
