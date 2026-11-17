# Working with Claude Code on SparkPoint

A practical guide for Sam: how sessions hand off, how to review what Claude
writes, and how to keep usage down. Written for this project, but the ideas
carry over to others.

## 1. The one idea behind everything

**A new session remembers nothing.** Each session starts with an empty context
and only knows what is loaded into it. So anything that must survive has to
live in a file, in git, or on GitHub. Everything below is a way of deciding
what goes where.

| What | Where it lives | Loaded |
|---|---|---|
| Rules that rarely change (commands, conventions, workflow) | `CLAUDE.md` | Every session, automatically |
| Where the work stands and what is next | `docs/HANDOFF.md` | Every session (imported by `CLAUDE.md`) |
| Why the system is built the way it is | `docs/DESIGN.md`, `docs/PLAN.md` | Only when Claude reads them |
| What changed and why | Git history and pull request descriptions | Only when looked up |
| How to do a repeated task | Skills in `.claude/skills/` | Name and one-line description always; the body only when used |
| Things that must happen automatically | Hooks in `.claude/settings.json` | Run by the app, not by Claude |
| Facts about you and your preferences | Claude's memory (outside the repo) | Relevant ones, automatically |

The first two rows cost context in every session, so keep them short.
`HANDOFF.md` should stay under about 60 lines.

## 2. The handoff routine

**Ending a session**

1. Type `/handoff`. Claude checks git and GitHub, rewrites `docs/HANDOFF.md`,
   and tells you what the next session will start on.
2. Make sure nothing is left uncommitted that you care about.

**Starting a session**

1. Open a new session in this folder. Three things load by themselves:
   `CLAUDE.md`, `docs/HANDOFF.md`, and the output of the session-start hook
   (current branch, uncommitted files, open pull requests).
2. Say what you want in one line, for example "continue with the next step in
   the handoff" or "I merged pull request 3, carry on".

You should never need to re-explain the project. If you find yourself doing
that, the missing fact belongs in `CLAUDE.md` or `HANDOFF.md`.

## 3. When to start a new session

Long sessions cost more and get worse. Every message re-sends the whole
conversation, and once it grows too long the app summarises older parts,
which loses detail.

- **One session per slice** (one pull request) is a good default.
- Start fresh when the topic changes, after a pull request is opened, or when
  the conversation has wandered.
- Stay in the same session for follow-ups on the work just done: review
  fixes, a failing CI run, a question about the code.

The session that built Milestones 0 and 1 also covered planning, hosting
questions and this guide. That is longer than ideal; Milestone 2 should start
in a new one.

## 4. Keeping usage down

- **Ask for one slice at a time.** Small tasks need less context and produce
  pull requests you can actually review.
- **Point at files** ("look at `SupplyChain.java`") instead of asking Claude
  to search the whole project.
- **Put repeated instructions in a skill** rather than retyping them. A skill
  costs one line of context until it is used.
- **Keep `CLAUDE.md` lean.** Every line is paid for in every message of every
  session. Remove rules that are no longer needed.
- **Do not paste long logs.** Ask Claude to run the command; it can filter the
  output itself.
- **Pick the model for the task.** The most capable model is worth it for
  design and tricky code; a faster, cheaper one is fine for mechanical edits.
- **Subagents and parallel sessions multiply cost.** Each starts cold and
  re-reads what it needs. Use them when work is truly independent.
- **Plan before big changes.** Agreeing on an approach first is cheaper than
  rewriting code built on a misunderstanding.

## 5. Reviewing what Claude writes

Treat Claude as a capable colleague whose work you have not seen before:
usually right, sometimes confidently wrong.

**Before reading the code**

1. Read the pull request description. Check that "what this adds" matches what
   you asked for and nothing more.
2. Look at the list of changed files. Anything unexpected is worth a question.
3. Confirm CI is green.

**Reading the code**

4. **Read the tests first.** They say what the code is supposed to do. Ask
   yourself what case is missing, especially at boundaries and for bad input.
5. Then read the implementation, starting with "where to look hardest".
6. Be suspicious of expected values in tests. Where did the number come from?
   If it came from running the code, the test proves nothing.
7. Check claims rather than trusting them. If the description says "all three
   deliberate breaks were caught", try one yourself.

**Running it**

8. Check out the branch in IntelliJ and run the tests. Change something and
   watch a test fail. Use the debugger on anything you do not follow.

**Asking questions**

9. Ask Claude to explain any line you do not understand, and ask "what would
   break if this were different?" If the answer is unconvincing, that is a
   finding.
10. For a second opinion, open a **new** session and ask for a review of the
    pull request (`/code-review 4`). A fresh session has no stake in the code
    it is reviewing; the session that wrote it does.

**What to watch for in AI-written code specifically**

- Code that handles cases nobody asked about, or abstractions with one user.
- Tests that restate the implementation instead of checking behaviour.
- Plausible details that are invented: library versions, API endpoints,
  constants. Ask where each came from.
- Comments or descriptions that claim more than the code does.
- Quiet scope growth: files changed that the task did not need.

**The rule that matters most:** do not merge what you do not understand. The
point of this project is your review, and `main` lets you merge with a single
click.

## 6. Skills in this project

A skill is a Markdown file with instructions that Claude loads when it is
needed, or when you type its name with a slash.

| Skill | Use it when | File |
|---|---|---|
| `/handoff` | Ending a session | [.claude/skills/handoff/SKILL.md](../.claude/skills/handoff/SKILL.md) |
| `/slice <what>` | Starting a new piece of a milestone | [.claude/skills/slice/SKILL.md](../.claude/skills/slice/SKILL.md) |

To change how Claude does either task, edit the file. To add a skill, create
`.claude/skills/<name>/SKILL.md` with a `name` and a `description` at the top;
the description is what tells Claude when to use it, so make it specific.

A good candidate for a skill is anything you have explained to Claude twice.

## 7. Hooks in this project

A hook is a shell command the app runs at a fixed moment. Claude does not
decide whether it runs, which is the point: use a hook when something must
happen every time.

| Hook | When | What it does | File |
|---|---|---|---|
| Session start | A session opens | Prints branch, uncommitted file count and open pull requests into the session | [.claude/hooks/session-start.sh](../.claude/hooks/session-start.sh) |

It is registered in [.claude/settings.json](../.claude/settings.json). Other
events you might use later: after a file is edited (run a formatter), before a
command runs (block something dangerous), when Claude stops (run the tests).

Two cautions:

- A hook runs with your permissions on your machine. Read any hook before
  adding it, especially one from someone else's repository.
- Hook output is added to the session, so a chatty hook costs context every
  time it fires.

## 8. Rule of thumb for where a new instruction goes

- "Always do X in this project" → `CLAUDE.md`
- "Here is how to do task Y" → a skill
- "X must happen automatically when Z" → a hook
- "Right now we are in the middle of W" → `docs/HANDOFF.md`
- "We chose A over B because…" → `docs/DESIGN.md` or the pull request
