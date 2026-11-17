# SparkPoint

Client-server desktop tool that maps wildfire and smoke exposure onto a
user-defined supply chain. Roadmap: [docs/PLAN.md](docs/PLAN.md). Design:
[docs/DESIGN.md](docs/DESIGN.md).

Sam reviews every change, so the work must be easy to review.

## Current state

@docs/HANDOFF.md

Before ending a session, or when Sam says he is stopping, run `/handoff` so
that file is accurate for the next session.

## Commands

- Build and run all tests and coverage checks: `./mvnw verify`
- One module: `./mvnw -pl sparkpoint-core verify`
- One test class: `./mvnw -pl sparkpoint-core test -Dtest=GeoPointTest`

Always use the wrapper (`./mvnw`); Maven is not installed globally.

## Workflow

- One pull request per slice of a milestone, small enough to review in one
  sitting. Never commit directly to `main`. The steps are in the `/slice`
  skill.
- Write the test first. Tests and the code they drive go in the same commit.
- A pull request description says what changed, why, the design choices made
  and alternatives rejected, and how it is tested.
- Update `docs/PLAN.md` and `docs/DESIGN.md` in the same pull request as the
  change that affects them. Diagrams are Mermaid.
- Run `./mvnw verify` before pushing.

## Code conventions

- Java 21. Prefer records for values, sealed interfaces for closed hierarchies.
- Base package `io.github.samuelkritz.sparkpoint`.
- `sparkpoint-core` has no compile-scope dependencies and does no I/O. Time
  comes from an injected `java.time.Clock`, never `Instant.now()`.
- The client module depends only on the RPC contract, never on core or server.
- Hand-written fakes over mocking libraries. AssertJ for assertions.
- Compiler warnings fail the build (`-Xlint:all -Werror`); fix them, do not
  suppress them without a comment explaining why.
- New modules are added to the parent POM only when they first have content.
- Add a dependency only when code in the same pull request uses it; look up
  the current version on Maven Central instead of guessing.

## Secrets

FIRMS and AirNow API keys come from environment variables or a file outside
the repository. Never commit keys, and never call the live APIs from tests
that run in CI.
