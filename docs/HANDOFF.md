# Handoff

The state of the work, for whoever picks it up next (a new Claude session, or
Sam after a break). Keep it short: this file is loaded into every session.
Update it with `/handoff` before ending a session.

Last updated: 2026-11-17

## Where things stand

- **Milestone 0** (skeleton, CI): merged (#1).
- **Milestone 1** (core model): code complete, waiting on Sam's review.
- No uncommitted work. Nothing is merged to `main` beyond #1.

| Pull request | What | Branch → base |
|---|---|---|
| #2 | Great-circle distance calculations | `m1-geo-distance` → `main` |
| #3 | Supply-chain graph and validation | `m1-supply-chain-graph` → `main` |
| #4 | XML format, reader, sample chain | `m1-xml-loader` → `m1-supply-chain-graph` (stacked on #3) |
| #5 | Handoff file, skills, session-start hook, guide | `claude-workflow` → `main` |

## Waiting on Sam

- Review and merge #5 (no product code; independent of the others).
- Review and merge #2 and #3 in either order, then #4.

## Do next

1. Each merge makes the other open branches out of date with `main`, which
   protection requires. After a merge, for each remaining branch:
   `git checkout <branch> && git merge origin/main`, verify, push.
2. #2 and #3 both append a section to `docs/DESIGN.md`; the second to merge
   conflicts there. Keep both sections: 2 (geographic model, from #2) before
   3 (supply-chain graph, from #3).
3. After #3 merges: `gh pr edit 4 --base main`, then update its branch.
4. Then start **Milestone 2** (rules and propagation) with `/slice`, one pull
   request each:
   1. Status model: `Status` enum, reasons, observation types
      (`FireDetection`, `AirQualityReading`).
   2. Rules: `Rule` interface, `FireProximityRule`, `AqiThresholdRule`,
      `AqiForecastRule`, `RouteFireProximityRule`.
   3. `rules.xml` thresholds and its reader.
   4. Downstream propagation with delay estimate and chain-impact summary.
      This is where `Route` gains transit time.

## Decisions still open

- Final status model (draft in `docs/PLAN.md` section 3): confirm with Sam
  before slice 1 of Milestone 2, especially whether `WATCH` propagates.
- API keys for FIRMS and AirNow: Sam registers when Milestone 3 starts.

## Things that are easy to trip over

- CI builds on JDK 21 but the machine's default `java` is 26. Before pushing:
  `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean verify`.
- `main` is protected: pull request, green "Build and test", branch up to date.
- This checkout commits with Sam's GitHub no-reply email (local git config).
  A fresh clone needs that set again before committing.
- `claude-starter-kit/` is a local, git-ignored folder. Leave it alone.
- Unconfirmed: that the session-start hook fires. If a new session was not
  told the branch and open pull requests at start, it did not.
