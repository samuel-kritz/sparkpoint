# Handoff

The state of the work, for whoever picks it up next (a new Claude session, or
Sam after a break). Keep it short: this file is loaded into every session.
Update it with `/handoff` before ending a session.

Last updated: 2026-11-17

## Where things stand

- **Milestone 0** (skeleton, CI): merged (#1).
- **Milestone 1** (core model): merged (#2, #3, #4). `main` passes
  `./mvnw clean verify` on JDK 21 (88 tests).
- Session workflow (this file, skills, session-start hook): merged (#7).
- **Milestone 2** (rules and propagation): not started.
- No open pull requests. No uncommitted work.

## Waiting on Sam

- Optional: delete the merged branches `m1-geo-distance` and
  `m1-supply-chain-graph` on GitHub.

## Do next

Start **Milestone 2** with `/slice`, one pull request each, branched from
`main`:

1. Status model: `Status` enum, reasons, observation types
   (`FireDetection`, `AirQualityReading`). In `docs/PLAN.md` section 3, mark
   the status model as final rather than proposed.
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

- CI builds on JDK 21 but the machine's default `java` is 26. Before pushing:
  `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean verify`.
- `main` is protected: pull request, green "Build and test", branch up to date
  (after a merge: `git merge origin/main` into each open branch, verify,
  push). The repository does not allow auto-merge.
- Stacked pull requests conflict after the lower one is squash-merged. Prefer
  branching each slice from `main`.
- This checkout commits with Sam's GitHub no-reply email (local git config).
  A fresh clone needs that set again before committing.
- `claude-starter-kit/` is a local folder, excluded through
  `.git/info/exclude`. Leave it alone.
- The session-start hook has fired from a branch, not yet from `main`. If a
  new session was not told the branch and open pull requests at start, it
  did not.
