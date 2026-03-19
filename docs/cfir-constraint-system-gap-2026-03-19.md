# CFIR Constraint System Gap Analysis

Date: 2026-03-19

## References Read

- `external/kotlin/compiler/resolution.common/src/org/jetbrains/kotlin/resolve/calls/inference/model/NewConstraintSystemImpl.kt`
- `external/kotlin/compiler/resolution/src/org/jetbrains/kotlin/resolve/calls/inference/components/KotlinConstraintSystemCompleter.kt`
- `external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp`
- `external/cangjie_compiler/src/Sema/LocalTypeArgumentSynthesis.cpp`

## Current Status

The repository already has a non-trivial constraint system in:

- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirConstraintSystem.kt`
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirConstraintSystemImpl.kt`
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirInferTypeArguments.kt`

It is no longer a placeholder. It supports:

- type variable registration
- subtype and equality constraints
- structural decomposition for class, struct, enum, function, tuple, array and union/intersection cases
- declaration upper bounds
- dependency-aware variable fixation
- expected return type constraints in call inference
- substitutor construction from fixed variables

## Implemented This Round

To keep the framework aligned with Kotlin K2 while preserving Cangjie semantics, this round fixed two framework-level issues:

1. `ConeTypeParameterLookupTag` equality is now name-based rather than identity-based.
   This removes false mismatches when the same generic parameter is resolved through different phases.

2. Applied generic call signatures are now materialized on the resolved reference.
   `CfirResolvedAppliedCallableReference` carries substituted parameter and return types so later diagnostics consume the instantiated signature, not the raw declaration signature.

This mirrors the Kotlin pipeline principle that completed inference results must flow into later checking stages, instead of being recomputed ad hoc.

## Alignment With Kotlin K2

The current implementation roughly matches the *shape* of K2 call inference:

- call stage builds constraints
- constraint system fixes variables
- candidate stores resulting substitutor
- later phases consume substituted types

But it is still much simpler than K2:

- no split between mutable storage, builder transactions, and read-only frozen state
- no incorporation engine comparable to Kotlin's `ConstraintInjector` + `ConstraintIncorporator`
- no postponed argument completion loop comparable to `KotlinConstraintSystemCompleter`
- no builder-inference / postponed lambda completion
- no fork-point contradiction handling
- no structured constraint-system diagnostics model

So the project is framework-aligned, but not yet framework-complete relative to K2.

## Alignment With Official Cangjie Compiler

The official Cangjie solver in `LocalTypeArgumentSynthesis.cpp` does more than the current CFIR solver:

- solves constraints through repeated topological passes over the type-variable dependency graph
- computes join of lower bounds and meet of upper bounds
- validates candidate solutions against language-specific rules
- supports partial solutions and best-solution comparison across candidates
- carries detailed blame information for argument vs return mismatches

The current CFIR solver partially matches this:

- dependency-aware fixation exists
- lower-bound and upper-bound aggregation exists
- ideal numeric types are materialized

But several official behaviors are still missing:

- multiple-solution comparison and best-solution selection
- richer diagnostic blame propagation
- partial-solution ranking
- full official validity rules around `Any`, `Nothing`, ideal types and unsolved variables

## Practical Conclusion

Current CFIR constraint solving should be treated as:

- usable for straightforward generic call inference
- integrated enough to support substituted downstream diagnostics
- not yet equivalent to Kotlin K2 completion machinery
- not yet semantically complete relative to the official Cangjie solver

## Verification

Verified locally:

- `./gradlew :cfir:cfir-cones:test --tests "org.cangnova.cangjie.cfir.types.ConeSubtypeCheckerTest"`
- `./gradlew :cfir:resolve:test --tests "org.cangnova.cangjie.cfir.resolve.calls.stages.CfirInferTypeArgumentsTest" -x :cfir:checkers:compileKotlin`

Added regression coverage for:

- same-name type parameter equality across phases
- expected return type participating in generic return inference
- `identity<T>(x: T): T` style inference

## Remaining Blocker For Full End-to-End Validation

The requested analysis diagnostic regression test cannot currently be rerun end-to-end with fresh checker bytecode because `:cfir:checkers:compileKotlin` is blocked by unrelated unresolved symbols under:

- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/match/...`

The active blocking errors are not in the constraint-system work itself.
