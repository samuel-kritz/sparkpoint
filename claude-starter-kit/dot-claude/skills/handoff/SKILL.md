---
name: handoff
description: Update docs/HANDOFF.md so the next session can pick up the work. Use before ending a session, when the user says they are stopping, or when asked for a handoff.
---

# Handoff

Bring `docs/HANDOFF.md` up to date with reality. The next session loads that
file automatically and knows nothing else about this one.

1. Gather facts; do not rely on memory of the conversation:
   - `git status -sb` and `git branch --show-current`
   - `gh pr list --state open --json number,title,baseRefName,headRefName`
   - `gh pr list --state merged --limit 5 --json number,title`
2. Rewrite `docs/HANDOFF.md`, keeping its headings:
   - **Where things stand**: milestones done, and a table of open pull requests.
   - **Waiting on the user**: what only he can do (reviews, decisions, sign-ups).
   - **Do next**: ordered, concrete steps. Name branches, pull request numbers
     and commands. Delete steps that are done.
   - **Decisions still open**.
   - **Things that are easy to trip over**: only what would cost the next
     session time. Remove entries that no longer apply.
3. Set "Last updated" to today's date.
4. Keep the file under about 60 lines. It is loaded into every session, so
   every line costs context. History belongs in git and pull requests.
5. If uncommitted work exists, say so in the file and tell the user.
6. Commit the file. If `main` is protected, include it in the current
   feature branch's pull request, or open a small one titled "Update handoff".

Finish by telling the user, in a few lines, what the next session will start on.
