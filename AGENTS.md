# Repository Guidelines

## Project Structure & Module Organization
- `xueyifang-backend/`: Spring Boot 2.7.x backend (Java 8 syntax, port `8080`, context path `/api`). Code lives in `src/main/java/com/xueyifang/` (`annotation/`, `aop/`, `common/`, `config/`, `constant/`, `controller/`, `domain/{dto,po,vo}/`, `enums/`, `exception/`, `interceptor/`, `mapper/`, `service/`, `strategy/`, `task/`, `utils/`, `websocket/`); configs in `src/main/resources/`; SQL & `.http` tests in `script/`; unit tests in `src/test/java/com/xueyifang/`.
- `xueyifang-frontend/`: Vue 3 + TypeScript + Vite frontend (port `3000`). App code in `src/` (`api/`, `components/`, `composables/`, `layouts/`, `router/`, `stores/`, `styles/`, `types/`, `utils/`, `views/`).
- `.agents/`: Workspace AI rules (`rules/xueyifang-conventions.md`) and Skills (`xueyifang-dev`, `xueyifang-qa`, `xueyifang-release`).
- `deploy/`: Deployment templates and portable runtime (`deploy/docker/`, `deploy/portable/`), assembled from the main repos on demand via `xueyifang-release`.
- `_archive/`: Archived legacy docs, defense PPT materials, old Playwright scripts, and duplicate copies (not part of any build).

> The repository root is a unified full-stack monorepo (`https://github.com/2832599985/xueyifang.git`). Use `git -c safe.directory=* ...` if git reports dubious ownership on Windows. Local private overrides belong in gitignored `xueyifang-backend/src/main/resources/application-local.yml` (template: `application-local.yml.example`).

## Build, Test, and Development Commands
Unified QA runner (from repo root):
- `python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer unit`: runs backend `mvn test` + frontend `npm run build` (`vue-tsc --noEmit && vite build`).
- `python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer api`: runs API integration tests (`trade_integration_test.py`, `test_admin.py`, `test_coverage_gaps.py`) against a live backend (`8080`).

Backend (run from `xueyifang-backend/`):
- `mvn test`: runs JUnit 5 + Mockito unit tests (no DB/Redis required).
- `mvn clean package`: builds the JAR and runs tests.
- `mvn spring-boot:run`: starts the API server (`http://localhost:8080/api/doc.html` for Knife4j).

Frontend (run from `xueyifang-frontend/`):
- `npm install`: install dependencies.
- `npm run dev`: start Vite dev server (`http://localhost:3000`).
- `npm run build`: run `vue-tsc --noEmit` type-check and Vite production build.
- `npm run preview`: preview the production build.
- `npm run lint`: run ESLint autofix.

## Coding Style & Naming Conventions
- Backend: Java 8 syntax only (`javax.*`, no `var`/`record`/text blocks); keep DTOs/POs/VOs in `domain/dto/`, `domain/po/`, `domain/vo/`, controllers in `controller/`, services in `service/`, mappers in `mapper/`. Follow `.agents/rules/xueyifang-conventions.md` for locking, atomic stock, `SerialNoGenerator`, and `TransactionUtils.afterCommit`.
- Frontend: Vue SFCs use `<script setup lang="ts">`, SCSS styles, and BEM-style class names. Reuse `src/utils/format.ts`, `src/utils/auth.ts`, `src/utils/echarts.ts`, and `src/types/*.ts` helpers.
- Naming: Vue components use `PascalCase`, variables/functions use `camelCase`.

## Testing Guidelines
- Backend unit tests live in `xueyifang-backend/src/test/java/com/xueyifang/` (JUnit 5 + Mockito). Keep new unit tests alongside the corresponding module package.
- Live API integration tests and UI acceptance checklists live in `.agents/skills/xueyifang-qa/`.
- **Browser Automation & UI Testing (IMPORTANT)**: Always prioritize **EGO Light (`ego-browser` / `egolite`, `ego-browser nodejs`)** for all browser operations, automated UI testing, page inspection, and screenshots (superior execution speed and token efficiency). Do not default to built-in `preview_*` tools when `ego-browser` is available.

## Commit & Pull Request Guidelines
- Git history in both submodules uses Conventional Commit prefixes (e.g., `feat:`, `fix:`, `refactor:`, `test:`, `perf:`, `docs:`, `chore:`). Follow this pattern.
- PRs should include a short summary, testing notes, and screenshots for UI changes.

## Configuration & Security Notes
- Initialize the database with `xueyifang-backend/script/xueyifang-clean-init.sql`.
- DB/Redis/JWT settings in `xueyifang-backend/src/main/resources/application.yml` and `application-dev.yml` support `DB_*`, `REDIS_*`, and `JWT_SECRET` environment overrides (`application-prod.yml` requires them with no defaults).
- API auth uses JWT; include `Authorization: Bearer <token>` when calling protected endpoints.

## Windows 控制台初始化（重要）

如当前系统为 Windows，请在进行其他操作前执行：

```
[Console]::InputEncoding  = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
chcp 65001 > $null
```
