# Contributing

## Branches
- `main` is protected. Merge only through Pull Requests with 1 review.
- One branch per feature: `feature/F1-capacity`, `feature/F7-allocation`
- Bug fixes: `fix/short-description`

## Daily flow
```bash
git checkout main && git pull
git checkout -b feature/F1-capacity
# ...work...
git add .
git commit -m "F1: calculate used slots from allocations"
git push -u origin feature/F1-capacity
# open a Pull Request on GitHub → link the issue with "Closes #1"
```

## Commit messages
Start with the feature ID: `F12: add section comment endpoint`. This makes each person's work easy to show in sprint reports and the viva.

## Rules
- Never commit `.env`, passwords, `node_modules/` or `target/`
- Pull `main` before starting work every day
- Run tests before opening a PR: `mvn test`
- Each sprint: goals posted Saturday, report Friday
