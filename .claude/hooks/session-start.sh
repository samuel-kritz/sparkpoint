#!/bin/sh
# SessionStart hook: prints the live repository state so a new Claude session
# starts from facts, not from a possibly stale docs/HANDOFF.md.
# Whatever this prints is added to the session's context, so keep it short.

cd "${CLAUDE_PROJECT_DIR:-.}" 2>/dev/null || exit 0
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || exit 0

echo "Repository state at session start:"
echo "- branch: $(git branch --show-current)"

changes=$(git status --porcelain | wc -l | tr -d ' ')
echo "- uncommitted files: $changes"

if command -v gh >/dev/null 2>&1; then
    prs=$(gh pr list --state open --limit 10 \
        --json number,title,baseRefName \
        --template '{{range .}}  #{{.number}} {{.title}} (into {{.baseRefName}}){{"\n"}}{{end}}' 2>/dev/null)
    if [ -n "$prs" ]; then
        echo "- open pull requests:"
        echo "$prs"
    else
        echo "- open pull requests: none (or GitHub unreachable)"
    fi
fi
exit 0
