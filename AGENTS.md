# AGENTS

## Core Principle

Avoid over-engineering. Only make changes that are directly requested or clearly necessary. Keep solutions simple and focused.

## Scope

- Do not add features, refactor code, or make improvements beyond what was asked.
- A bug fix does not require surrounding code cleanup.
- A simple feature does not require extra configurability.

## Documentation

- Do not add docstrings, comments, or type annotations to code you did not change.
- Only add comments where the logic is not self-evident.

## Defensive Coding

- Do not add error handling, fallbacks, or validation for scenarios that cannot happen.
- Trust internal code and framework guarantees.
- Only validate at system boundaries such as user input and external APIs.

## Abstractions

- Do not create helpers, utilities, or abstractions for one-time operations.
- Do not design for hypothetical future requirements.
- The right amount of complexity is the minimum needed for the current task.

## Implementation Expectations

Please write a high-quality, general-purpose solution using the standard tools available.

- Do not create helper scripts or workarounds to accomplish the task more efficiently.
- Implement a solution that works correctly for all valid inputs, not just the test cases.
- Do not hard-code values or create solutions that only work for specific test inputs.
- Implement the actual logic that solves the problem generally.

## Engineering Standard

- Focus on understanding the problem requirements and implementing the correct algorithm.
- Treat tests as verification of correctness, not as the definition of the solution.
- Provide a principled implementation that follows best practices and software design principles.
- If the task is unreasonable or infeasible, or if any tests are incorrect, say so rather than working around them.
- The solution should be robust, maintainable, and extendable.

## Investigation Requirement

- Never speculate about code that has not been opened.
- If a specific file is referenced, read that file before answering.
- Investigate and read relevant files before answering questions about the codebase.
- Do not make claims about code before investigating unless the answer is already certain.
- Keep answers grounded and hallucination-free.

## Build APK Workflow

- When the user says `build the apk`, build the debug APK.
- Copy the resulting APK into `/mnt/hgfs/apks/` using the next sequential filename in this format: `calsage-vN.0-debug.apk`.
- When the user asks for a release version, build the signed release APK and copy it into `/mnt/hgfs/apks/` using the release version filename, for example `calsage-v1.6.apk`.
- Preserve the existing version history; do not overwrite older versions unless the user explicitly asks for that.
