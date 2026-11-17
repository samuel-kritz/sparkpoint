# Handoff

The state of the work, for whoever picks it up next (a new Claude session, or
Sam after a break). Keep it short: this file is loaded into every session.
Update it with `/handoff` before ending a session.

Last updated: 2026-11-17

## Where things stand

- **Milestone 0** (skeleton, CI): merged (#1).
- **Milestone 1** (core model): #2 merged; #3 and #4 waiting on Sam's review.
  Both branches are up to date with `main` and pass `./mvnw clean verify` on
  JDK 21 (66 and 88 tests).
- #5 (this workflow material) was merged without squashing, then reverted
  (#6). #7 puts it back as a single commit.
- No uncommitted work.

| Pull request | What | Branch → base |
|---|---|---|
| #3 | Supply-chain graph and validation | `m1-supply-chain-graph` → `main` |
| #4 | XML format, reader, sample chain | `m1-xml-loader` → `m1-supply-chain-graph` (stacked on #3) |
| #7 | Handoff file, skills, session-start hook, guide (re-land of #5) | `claude-workflow-squash` → `main` |

## Waiting on Sam

- Squash-merge #7 (no product code; independent of the others).
- Review and merge #3, then #4.

## Do next

1. Each merge makes the other open branches out of date with `main`, which
   protection requires. After a merge, for each remaining branch:
   `git checkout <branch> && git merge origin/main`, verify, push.
2. After #3 merges: `gh pr edit 4 --base main`, then update its branch.
3. Then start **Milestone 2** (rules and propagation) with `/slice`, one pull
   request each:
   1. Status model: `Status` enum, reasons, observation types
      (`FireDetection`, `AirQualityReading`). Mark the status model in
      `docs/PLAN.md` section 3 as final rather than proposed.
   2. Rules: `Rule` interface, `FireProximityRule`, `AqiThresholdRule`,
      `AqiForecastRule`, `RouteFireProximityRule`.
   3. `rules.xml` thresholds and its reader.
   4. Downstream propagation with delay estimate and chain-impact summary.
      This is where `Route` gains transit time.

## Decisions

- Status model: Sam approved the draft in `docs/PLAN.md` section 3 as
  written (2026-11-17). `WATCH` does not propagate; it is only listed in the
  downstream node's detail pane.
- Still open: API keys for FIRMS and AirNow. Sam registers when Milestone 3
  starts.

## Things that are easy to trip over

- Sam squash-merges pull requests. When #3 is squash-merged, #4 still holds
  #3's original commits. Merging `origin/main` into #4 should resolve them as
  identical changes; check the pull request's diff afterwards shows only #4's
  own files.
- CI builds on JDK 21 but the machine's default `java` is 26. Before pushing:
  `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean verify`.
- `main` is protected: pull request, green "Build and test", branch up to date.
- This checkout commits with Sam's GitHub no-reply email (local git config).
  A fresh clone needs that set again before committing.
- `claude-starter-kit/` is a local folder, excluded through
  `.git/info/exclude`. Leave it alone.
- The session-start hook fired on 2026-11-17 in a checkout on the old
  `claude-workflow` branch. Still to confirm: that it fires from `main` once
  #7 is merged. If a new session was not told the branch and open pull
  requests at start, it did not.
