# Handoff

The state of the work, for whoever picks it up next (a new Claude session, or
Sam after a break). Keep it short: this file is loaded into every session.
Update it with `/handoff` before ending a session.

Last updated: 2026-11-16

## Where things stand

- **Milestone 0** (skeleton, CI): merged.
- **Milestone 1** (core model): code complete, waiting on Sam's review.

| Pull request | What | State |
|---|---|---|
| #2 | Great-circle distance calculations | Open, based on `main` |
| #3 | Supply-chain graph and validation | Open, based on `main` |
| #4 | XML format, reader, sample chain | Open, stacked on #3's branch |

## Waiting on Sam

- Review and merge #2 and #3 (either order), then #4.

## Do next

1. After the first of #2/#3 merges: merge `main` into the other branch and
   resolve the conflict at the end of `docs/DESIGN.md` (both add a section).
2. After #3 merges: retarget #4 to `main` (`gh pr edit 4 --base main`) and
   update its branch.
3. Then start **Milestone 2** (rules and propagation), one pull request each:
   1. Status model: `Status` enum, reasons, observation types
      (`FireDetection`, `AirQualityReading`).
   2. Rules: `Rule` interface, `FireProximityRule`, `AqiThresholdRule`,
      `AqiForecastRule`, `RouteFireProximityRule`.
   3. `rules.xml` thresholds and its reader.
   4. Downstream propagation with delay estimate and chain-impact summary.
      This is where `Route` gains transit time.

## Decisions still open

- Final status model (draft in `docs/PLAN.md` section 3): confirm with Sam at
  the start of Milestone 2, especially whether `WATCH` propagates.
- API keys for FIRMS and AirNow: Sam registers when Milestone 3 starts.

## Things that are easy to trip over

- CI builds on JDK 21. The machine's default `java` is 26, so check with
  `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify` before pushing.
- `main` is protected: pull request plus a green "Build and test" check, and
  the branch must be up to date with `main`.
- This checkout commits with Sam's GitHub no-reply email (set in the local git
  config). A fresh clone needs that set again before committing.
- Schema error messages from the XML reader are wordy and doubled; revisit
  when the dashboard shows them (see #4).
