# Demo seeder

Script: `functions/scripts/demo-seeder.js`

What it seeds
- Teacher auth + profile (default `teacher1@gmail.com`)
- Five student auth accounts + profiles (`student1@gmail.com` ... `student5@gmail.com`)
- One classroom with students enrolled
- Topics:
  - Illustrating Simple and Compound Interest
  - Distinguishing Between Simple and Compound Interest
  - Computing Interest, Maturity Value, Future Value and Present Value in Simple and Compound Interest Environment
  - Solving Problems Involving Simple and Compound Interest
- Quizzes/assignments: Pre-test and Post-test per topic (15 questions each, 60s default per question), scoring BEST, 3 attempts, closeAt = now + 14 days
- Learning materials derived from docx files under `Seed doc materials` (round-robin across topics; truncated text pulled via mammoth)

Prereqs
- Service account creds available to Firebase Admin (`GOOGLE_APPLICATION_CREDENTIALS` or emulator env)
- Install deps: `npm --prefix functions install` (mammoth is used for docx text)
- Node 18 is the target engine (Node 20 will work but prints an engine warning)

Run it
```
GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json `
SEED_PASSWORD=password `
npm --prefix functions run seed:demo
```

Useful overrides (env)
- `SEED_TEACHER_EMAIL`, `SEED_TEACHER_NAME`
- `SEED_STUDENT_EMAILS=student1@gmail.com,student2@gmail.com`
- `SEED_CLASSROOM_ID`, `SEED_CLASSROOM_NAME`, `SEED_JOIN_CODE`
- `SEED_TOPIC_ID`, `SEED_TOPIC_NAME`
- `SEED_DOC_DIR` (defaults to `Seed doc materials` at repo root)
- `SEED_MATERIAL_LIMIT` (defaults to 25)
- `SEED_RESET_PASSWORDS=false` to avoid resetting existing auth passwords

Notes
- Materials fall back to filename-only bodies if mammoth is not installed.
- Re-running the script is idempotent: existing docs are updated; createdAt timestamps are preserved when present.
