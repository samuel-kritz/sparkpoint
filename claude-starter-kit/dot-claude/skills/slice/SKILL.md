---
name: slice
description: Build one reviewable slice of the project as a pull request - branch, tests first, implementation, mutation check, design doc, PR. Use when starting a new piece of a milestone.
---

# Build one slice

A slice is one pull request that the user can review in one sitting. Argument: what
the slice is (for example "login form validation").

1. **Sync.** `git fetch`, then branch from `origin/main`. Branch from another
   open branch only if this slice needs its code, and say so in the pull
   request.
2. **Design briefly.** State the types, their responsibilities and the
   alternatives you rejected. If a choice is the user's to make (behaviour a user
   would notice), ask before coding.
3. **Tests first.** Write the tests before the implementation. Expected values
   must come from somewhere other than the code under test: exact maths, a
   hand-worked example, or an independent calculation.
4. **Implement.** Follow the conventions in `CLAUDE.md`.
5. **Verify the way CI does:**
   `<your full build-and-test command>`
6. **Check the tests can fail.** Break the new code in two or three realistic
   ways, one at a time, and confirm a test fails each time. Restore the file
   after each and confirm `git diff` is clean. If a break survives, add a test
   or explain why no behaviour test can catch it.
7. **Document.** Add or update the design notes, with a
   diagram where structure or flow is involved.
8. **Open the pull request** with these sections:
   - What this adds
   - Design choices (table: choice, why, alternative rejected)
   - Known limits or roughness
   - How it was tested (counts, the deliberate breaks and their results, and
     honestly whether tests were seen failing before passing)
   - Where to look hardest
9. **Report** to the user: the link, what to scrutinise, anything unverified.

Do not start the next slice on top of unreviewed work unless the user asks.
