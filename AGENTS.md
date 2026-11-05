<!--
TIREON - AI 개발 에이전트 지침

목적:
  AI 코딩 에이전트가 프로젝트의 공통 개발 규칙과
  코드 작성 원칙을 준수하도록 안내합니다.

대상:
  Claude Code, OpenCode, Codex 등 AI 개발 에이전트

관리 기준:
  팀의 개발 방식이나 아키텍처가 변경되면 함께 수정합니다.
  CONTRIBUTING.md와 내용이 충돌하지 않도록 유지합니다.
-->

# AGENTS.md

# TIREON - AI Agent Development Guidelines

## 1. Project Overview

TIREON is an AI-powered driving coach application designed to help beginner drivers develop safer driving habits.

The application is being developed for the Hyundai Motor Group Pleos Connect app development environment.

### Core Principles

- Prioritize driving safety and reliability.
- Use officially supported Pleos Connect APIs and SDKs.
- Keep the application maintainable and easy to understand.
- Avoid unnecessary complexity and premature abstraction.
- Preserve consistency across the team's codebase.

Before implementing a feature, understand its purpose and how it fits into the existing application.

## 2. General Development Rules

- Inspect the existing codebase before making changes.
- Follow established project architecture and coding conventions.
- Prefer modifying existing implementations over introducing duplicate logic.
- Do not introduce unnecessary dependencies or frameworks.
- Avoid speculative implementations and unsupported assumptions.
- Do not modify unrelated files.
- Keep changes focused on the requested task.
- Do not hide errors with arbitrary fallback logic or broad exception handling.
- Do not add comments that merely repeat what the code already expresses.
- Write comments only when they explain non-obvious decisions or important constraints.

## 3. Planning Before Implementation

For complex tasks or changes affecting multiple files:

1. Inspect the relevant code and project structure.
2. Identify dependencies and potential side effects.
3. Describe the proposed implementation plan.
4. Identify risks and required tests.
5. Wait for confirmation when the change involves significant architectural decisions or destructive operations.

For small, straightforward changes, proceed without unnecessary planning.

Do not start large-scale refactoring without first explaining its scope and expected impact.

## 4. Architecture and Code Quality

- Respect the existing separation of responsibilities.
- Keep UI, business logic, and data access appropriately separated.
- Avoid putting unrelated responsibilities into a single class.
- Prefer clear and explicit interfaces.
- Avoid unnecessary abstractions, inheritance, and design patterns.
- Do not duplicate business logic across multiple components.
- Maintain consistent naming conventions.
- Preserve existing public interfaces unless a change is necessary.

Do not introduce a new architectural pattern solely for stylistic reasons.

## 5. Kotlin and Android Development

- Follow Kotlin coding conventions.
- Prefer `val` over `var` when reassignment is unnecessary.
- Use null safety instead of unnecessary force unwrapping.
- Use coroutines and structured concurrency where appropriate and supported by the project.
- Avoid blocking the main thread.
- Handle lifecycle-related operations correctly.
- Release resources when they are no longer needed.
- Keep UI operations on the appropriate thread.
- Handle errors explicitly and provide meaningful failure information.

Do not introduce Android APIs that are incompatible with the project's configured SDK or runtime environment.

## 6. Pleos Connect SDK

Pleos Connect compatibility is a core project requirement.

- Refer to official Pleos Connect documentation before implementing SDK-dependent functionality.
- Use only officially supported APIs and capabilities.
- Check vehicle capability and API availability before relying on vehicle data.
- Verify required permissions and authorization procedures.
- Do not assume that a vehicle signal or API is available on every supported vehicle.
- Handle unavailable data, permission failures, and asynchronous responses.
- Distinguish emulator data from actual vehicle data.
- Do not fabricate vehicle signals or API responses.
- Do not bypass platform restrictions or security requirements.

If the SDK documentation does not confirm that a feature is supported, explicitly report the uncertainty instead of inventing an implementation.

## 7. AI Features and Driving Safety

TIREON provides AI-based driving guidance.

- Treat vehicle data as untrusted input until validated.
- Handle missing, delayed, and abnormal sensor values.
- Clearly distinguish measured data from AI-generated interpretations.
- Do not present uncertain AI predictions as verified facts.
- Avoid recommendations that could distract drivers while driving.
- Prioritize safety over conversational convenience.
- Never assume that an AI-generated recommendation is a substitute for vehicle safety systems.
- Keep API keys, credentials, and private prompts out of source control.

AI responses must be designed to assist drivers without encouraging unsafe driving behavior.

## 8. Error Handling and Logging

- Handle expected failures explicitly.
- Preserve useful error information for debugging.
- Avoid silently swallowing exceptions.
- Do not expose credentials, tokens, personal information, or sensitive vehicle data in logs.
- Avoid excessive logging in production builds.
- Do not use empty catch blocks.

When fixing a bug, identify and address its root cause rather than masking its symptoms.

## 9. Testing and Verification

After implementing changes:

- Run the relevant tests.
- Verify that the project builds successfully when possible.
- Test affected functionality in the Pleos Connect emulator when applicable.
- Check for regressions in related functionality.
- Report tests that could not be executed and explain why.

Never claim that a test or build succeeded unless it was actually executed successfully.

If verification is unavailable, clearly state that the implementation remains unverified.

## 10. Dependencies and Configuration

- Check existing dependencies before adding new ones.
- Prefer existing project libraries when they satisfy the requirement.
- Avoid unnecessary dependency upgrades.
- Explain the reason for introducing a new dependency.
- Never commit API keys, passwords, tokens, signing keys, or private credentials.
- Keep environment-specific configuration separate from shared configuration.
- Do not modify Gradle or SDK versions without a clear reason.

## 11. Git and Collaboration

- Follow the repository's `CONTRIBUTING.md`.
- Use the existing branch naming and commit conventions.
- Do not commit directly to `main`.
- Do not create commits, push branches, or open PRs unless explicitly requested.
- Do not rewrite Git history or force-push without explicit permission.
- Do not modify another team member's work without understanding its purpose.
- Keep changes isolated and reviewable.

## 12. Destructive Operations

Always obtain explicit confirmation before:

- Deleting project files or directories.
- Removing dependencies or major components.
- Rewriting Git history.
- Resetting or force-pushing branches.
- Changing database schemas in ways that could cause data loss.
- Overwriting existing configuration or user data.

Never perform destructive operations based on assumptions.

## 13. Communication and Reporting

When completing a task, provide a concise report containing:

1. What was changed.
2. Why the change was necessary.
3. Important implementation details.
4. Tests and build results.
5. Remaining issues or limitations.

If requirements are ambiguous, identify the ambiguity and ask for clarification when it materially affects the implementation.

Do not claim completion when important requirements remain unimplemented.
