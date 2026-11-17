# <Project name>

<One or two sentences: what this project is and who it is for.>

<Who reviews the work and what they care about, for example: "I review every
change, so the work must be easy to review.">

## Current state

@docs/HANDOFF.md

Before ending a session, or when I say I am stopping, run `/handoff` so that
file is accurate for the next session.

## Commands

- Build and run all tests: `<command>`
- Run one test: `<command>`
- Run the app locally: `<command>`

## Workflow

- One pull request per small slice of work. Never commit directly to `main`.
  The steps are in the `/slice` skill.
- Write the test first. Tests and the code they drive go in the same commit.
- A pull request description says what changed, why, the design choices made
  and alternatives rejected, and how it is tested.
- Run the full build before pushing.

## Code conventions

- <Language and version>
- <Structure rules, for example which module may depend on which>
- <Testing style>
- Add a dependency only when code in the same pull request uses it; look up
  the current version instead of guessing.

## Secrets

Keys and passwords come from environment variables or a file outside the
repository. Never commit them.
