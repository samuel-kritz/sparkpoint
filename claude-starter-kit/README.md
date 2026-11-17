# Claude Code starter kit

A generic copy of the setup used on SparkPoint, for starting your next project
with handoffs, a pull-request workflow and a session-start hook already in
place. Everything here is meant to be edited.

## What is in it

| File | Purpose | Edit it? |
|---|---|---|
| `CLAUDE.md` | Project rules loaded into every session | Yes: fill in every `<placeholder>` |
| `docs/HANDOFF.md` | Current state and next steps, loaded into every session | Yes, then let `/handoff` maintain it |
| `dot-claude/skills/handoff/SKILL.md` | `/handoff`: updates the handoff file | Rarely |
| `dot-claude/skills/slice/SKILL.md` | `/slice`: builds one reviewable pull request | Yes: set your build command and PR sections |
| `dot-claude/hooks/session-start.sh` | Prints branch, changes and open pull requests at session start | Rarely |
| `dot-claude/settings.json` | Registers the hook | Only to add hooks |

## How to use it

1. Copy the contents of this folder into the root of the new project, then
   rename `dot-claude` to `.claude`, which is the name Claude Code looks for:

   ```bash
   cp -R claude-starter-kit/. /path/to/new-project/ && mv /path/to/new-project/dot-claude /path/to/new-project/.claude
   ```

   The folder is stored as `dot-claude` here so that the kit's skills are not
   picked up as extra skills of whichever project the kit is sitting in.

2. Delete this `README.md` from the copy, or replace it with the project's own.
3. Fill in the placeholders in `CLAUDE.md` and `docs/HANDOFF.md`.
4. Make sure the hook script is executable:

   ```bash
   chmod +x .claude/hooks/session-start.sh
   ```

5. Add `.claude/settings.local.json` to `.gitignore`. That file is for
   personal overrides and should not be committed.
6. Start a new Claude Code session in the project. The hook only takes effect
   in sessions started after the files exist.

## Notes

- The hook uses `git` and, if installed, the GitHub CLI (`gh`). Without `gh`
  it simply skips the pull request list.
- Keep `CLAUDE.md` and `docs/HANDOFF.md` short. Both are loaded into every
  session, so every line costs context each time.
- The full explanation of why things are arranged this way is in SparkPoint's
  `docs/WORKING-WITH-CLAUDE.md`.
