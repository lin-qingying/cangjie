# Resolution Common Inference Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 `resolution.common` 剩余的 Kotlin 时代约束系统迁移为基于仓颉刚性类型模型的推断实现，同时保留必要的求解器机制。

**Architecture:** 迁移按“语义 authority inversion”推进：把仓颉语义判断从旧的 `resolution.common` 文件中抽离出来，只保留求解器编排、事务、fixation、postponed analysis 等机制。实现顺序遵循“先重建 rigid constraint algebra，再重写结果选择与 postponed callable shaping，最后收束 Legacy/K1 兼容壳层”。

**Tech Stack:** Kotlin/JVM, Gradle, `:common`, `:resolution.common`, OpenSpec, local `external/kotlin` and `external/cangjie_compiler` references.

---

## Context Snapshot

- 已完成：`AbstractTypeChecker.RUN_SLOW_ASSERTIONS`、`prepareType(context, type)`、`NewCommonSuperTypeCalculator` 的刚性类型骨架、OpenSpec 1.1/1.2/1.3/5.1。
- 当前阻塞集中在：
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/InferenceUtils.kt`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ConstraintIncorporator.kt`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ConstraintInjector.kt`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ResultTypeResolver.kt`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/TypeCheckerStateForConstraintSystem.kt`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/PostponedArgumentInputTypesResolver.kt`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/TrivialConstraintTypeInferenceOracle.kt`
- 核心原则：
  - **保留机制**：constraint store、variable dependencies、incorporation、fixation、postponed analysis、stub/captured/internal placeholders。
  - **删除 Kotlin 语言语义**：flexible types、Kotlin nullability (`T?` / `T!` / DNN)、star projection、K1/K2 feature-gating、`Any?` / `Nothing?` triviality 规则。
  - **禁止行为**：为了消除编译错误而删除求解器路径；把 Kotlin 语义重新包装后放回 `common`。

## Semantic Split Table

### `TypeCheckerStateForConstraintSystem.kt`

**Keep:**
- subtype → constraint lowering role
- variable extraction and ownership checks
- intersection-driven decomposition hooks
- branch/fork capability if needed by rigid ambiguity

**Rewrite:**
- canonical rigid decomposition rules
- handling of internal captured/stub placeholders
- rules for when relation is undecided and must stay postponed

**Remove:**
- flexible/nullability-specific simplification
- `withNullability`, `isMarkedNullable`, DNN, `FlexibleTypeMarker`
- Kotlin `Exact` / `NoInfer` baggage if unsupported by Cangjie semantics

### `ConstraintInjector.kt`

**Keep:**
- initial subtype/equality injection
- processing queue, dependency recording, fork bookkeeping
- orchestration around `ConstraintIncorporator`

**Rewrite:**
- rigid equality/subtyping normalization
- skip rules and max-depth reasoning only where solver-safe
- context-receiver usage to current `common` APIs

**Remove:**
- nullable/flexible retry logic
- `Any?`/`Nothing?`-driven branch handling
- Kotlin feature switches with no Cangjie equivalent

### `ConstraintIncorporator.kt`

**Keep:**
- direct incorporation and nested incorporation
- `derivedFrom` propagation
- recursion prevention

**Rewrite:**
- replacement-type synthesis when variable occurs inside generic structure
- captured/stub use only as internal inference tools
- “informative constraint” generation under rigid model

**Remove:**
- Kotlin projection/capture recipes using `TypeVariance.IN/OUT`
- nullability-special usefulness heuristics
- flexible-bound approximation as semantic truth

### `ResultTypeResolver.kt`

**Keep:**
- equality / lower / upper candidate pipeline
- lower→common supertype and upper→intersection shape
- proper-constraint satisfaction checks

**Rewrite:**
- fallback result lattice for unresolved variables
- result suitability under rigid Cangjie semantics
- use of internal placeholders during incomplete solving

**Remove:**
- `nullableAnyType` / `Nothing?` defaults
- flexible-nullability propagation
- K1/K2 and Kotlin feature-gated resolution branches

### `TrivialConstraintTypeInferenceOracle.kt`

**Keep:**
- existence of an oracle that filters solver noise

**Rewrite:**
- define triviality by “information value for Cangjie solving”

**Remove:**
- Kotlin `Nothing` / `NullableAny` / flexible-bottom heuristics

### `PostponedArgumentInputTypesResolver.kt`

**Keep:**
- postponed callable/lambda expected-type propagation
- parameter/return position variable creation
- relation with fixation/completion rounds

**Rewrite:**
- callable shape extraction around Cangjie function types
- parameter recovery from rigid callable constraints

**Remove:**
- Kotlin `FunctionTypeKind` assumptions
- extension/suspend/reflect function folklore if absent in Cangjie model

### `InferenceUtils.kt`

**Keep:**
- substitutor construction
- variable registration helpers
- recursive variable discovery

**Rewrite:**
- meaning of `createUninferredType`, `defaultType`, and stub fallback under rigid model

**Remove:**
- residual K1/K2 semantic gates

## Execution Order

### Task 1: Freeze rigid constraint algebra

**Files:**
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/TypeCheckerStateForConstraintSystem.kt`
- Reference: `common/src/org/cangnova/cangjie/type/model/TypeSystemContext.kt`
- Reference: `common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt`
- Reference: `external/kotlin/compiler/resolution.common/src/org/jetbrains/kotlin/resolve/calls/inference/components/TypeCheckerStateForConstraintSystem.kt`

**Step 1:** enumerate every subtype simplification branch and classify it as rigid keep / rewrite / remove.  
**Step 2:** write a small contract block (comment or plan notes) for canonical Cangjie constraint lowering.  
**Step 3:** replace Kotlin nullability/flexible simplification with rigid decomposition and undecided fallthrough.  
**Step 4:** compile `:resolution.common` and inspect new failure surface.  

### Task 2: Rebuild injection and incorporation around rigid rules

**Files:**
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ConstraintInjector.kt`
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ConstraintIncorporator.kt`
- Reference: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintSystemImpl.kt`

**Step 1:** keep queue/transaction/dependency infrastructure intact.  
**Step 2:** replace Kotlin-specific normalization and generated replacement types with rigid equivalents.  
**Step 3:** retain captured/stub paths only when they are internal solver placeholders.  
**Step 4:** compile `:resolution.common` again and confirm errors move downstream rather than re-expand.  

### Task 3: Redefine fixation result selection

**Files:**
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ResultTypeResolver.kt`
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/TrivialConstraintTypeInferenceOracle.kt`
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/InferenceUtils.kt`

**Step 1:** keep equality/lower/upper pipeline structure.  
**Step 2:** replace Kotlin fallback types and triviality rules with rigid Cangjie defaults.  
**Step 3:** make unresolved-variable placeholders explicit and solver-internal.  
**Step 4:** compile and run any focused inference tests available for these classes.  

### Task 4: Retarget postponed callable analysis

**Files:**
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/PostponedArgumentInputTypesResolver.kt`
- Reference: `cfir/resolve/...` callable inference files if a native Cangjie callable model already exists

**Step 1:** define Cangjie callable-shape extraction primitives.  
**Step 2:** keep postponed parameter propagation structure.  
**Step 3:** remove Kotlin-only function-kind and nullability assumptions.  
**Step 4:** compile and run related callable/lambda inference tests if present.  

### Task 5: Remove Legacy/K1 compatibility shells

**Files:**
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/VariableFixationFinder.kt`
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintSystemImpl.kt`
- Modify: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ConstraintSystemUtilContext.kt`
- Search: Legacy/K1 annotations and feature gates across `resolution.common`

**Step 1:** remove `@K1Deprecation`, Legacy readiness calculators, and fake feature gates only after their live Cangjie path exists.  
**Step 2:** collapse to one mainline solver path.  
**Step 3:** wire guarded assertions under `RUN_SLOW_ASSERTIONS`.  

## Verification Strategy

- After each task: run `./gradlew.bat :resolution.common:compileKotlin`
- When a semantic slice stabilizes: run the smallest relevant module tests (search for inference/constraint tests first)
- Before claiming progress on a hotspot:
  - verify failure count moved in the expected direction,
  - verify no Kotlin semantic helper was reintroduced into `common`,
  - verify OpenSpec `tasks.md` checkboxes reflect actual completion.

## Stop Conditions / Escalation

- If a rewrite requires inventing a new public `common` type concept not already evidenced by Cangjie docs or `external/cangjie_compiler`, stop and document it.
- If a mechanism appears both “internal placeholder” and “Kotlin language semantic,” isolate it in a temporary adapter and review before deletion.
- If compile errors spread back into `common`, revert that slice and reassess; `common` remains the semantic boundary.

## Expected Deliverable of Next Execution Session

- `TypeCheckerStateForConstraintSystem.kt` rewritten around rigid constraint lowering
- `ConstraintInjector.kt` / `ConstraintIncorporator.kt` aligned to that algebra
- updated OpenSpec tasks 4.1 / 4.2 and, if justified, 2.1 / 2.2
- fresh compile evidence showing the remaining blocker surface narrowed again
