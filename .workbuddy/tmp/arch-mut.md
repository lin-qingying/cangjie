# Architecture Map: `mut` / `this` diagnostics in CFIR

Read-only structural reconnaissance for the compiler-repair pipeline. No semantics decided, no fix proposed, no file edited. All first-party producers live under `cfir/checkers/src/...`; the Kotlin mirror is under `external/kotlin`.

---

## 1. Kotlin counterpart (where the analogous problems are handled)

Kotlin FIR does **not** have a dedicated `FirCapturedVal` element in this snapshot (lambda var/this capture is handled via CFA + `inlineFunctionBodyContext`, not a FIR node). The structurally-analogous machinery is the checker framework + a small set of `this`-receiver / qualified-access checkers.

| Path (absolute) | Line | One-line description |
|---|---|---|
| `D:\code\intellij\cangjie\external\kotlin\compiler\fir\checkers\src\org\jetbrains\kotlin\fir\analysis\checkers\expression\FirExpressionChecker.kt` | 15 | Base `FirExpressionChecker<E : FirStatement>` — mirrors CFIR `CfirBasicExpressionChecker`; `check(expression)` with `context` + `reporter` as context receivers. |
| `D:\code\intellij\cangjie\external\kotlin\compiler\fir\checkers\gen\...\expression\FirExpressionCheckerAliases.kt` | 85 | `typealias FirThisReceiverExpressionChecker = FirExpressionChecker<FirThisReceiverExpression>` — the exact analog of a CFIR basic-expression checker specialized on `CfirThisReceiverExpression`. |
| `D:\code\intellij\cangjie\external\kotlin\compiler\fir\checkers\src\...\expression\FirInlineExposedLessVisibleThisReceiverChecker.kt` | 15-27 | `FirThisReceiverExpressionChecker` that pulls enclosing context from `context.inlineFunctionBodyContext` (the Kotlin analog of CFIR `current*Context`) and reports on `expression.source`. This is the closest analog to CFIR's `CfirFinalizerThisUsageChecker` / `CfirOpenConstructorThisUsageChecker` family. |
| `D:\code\intellij\cangjie\external\kotlin\compiler\fir\checkers\src\...\expression\FirInlineExposedLessVisibleTypeQualifiedAccessChecker.kt` | 25-77 | `FirQualifiedAccessExpressionChecker` that pulls enclosing context (`context.inlineFunctionBodyContext`), resolves the target via `expression.toResolvedCallableSymbol()`, and reports on `expression.source`. Analog of CFIR `CfirQualifiedAccessChecker` producers. |
| `D:\code\intellij\cangjie\external\kotlin\compiler\fir\checkers\src\...\context\CheckerContext.kt` | 38 | `abstract val containingDeclarations: List<FirBasedSymbol<*>>` — exact mirror of CFIR `CheckerContext.containingDeclarations`. |
| `D:\code\intellij\cangjie\external\kotlin\compiler\fir\checkers\src\...\context\CheckerContext.kt` | 41-48 | `callsOrAssignments`, `inlineFunctionBodyContext`, `lambdaBodyContext` — mirror CFIR `context.callsOrAssignments` and the "walk containingDeclarations for the enclosing function boundary" helpers. |
| `D:\code\intellij\cangjie\external\kotlin\compiler\fir\checkers\checkers.wasm\src\...\expression\FirWasmJsCodeCallChecker.kt` | 27-87 | **Key pattern CFIR already mirrors**: a `FirFunctionCallChecker` that reads `context.containingDeclarations` (lines 36-41), casts `containingDeclaration` to `FirNamedFunctionSymbol`, inspects `isSuspend`/`isInline`/`isExtension`, and reports on `expression.calleeReference.source` (line 46). This is the "run over a specific FIR element type, pull the enclosing declaration from a symbol" pattern the CFIR `current*Context` / `findClosestDeclaration` helpers replicate. |

**Takeaway for CFIR**: Kotlin gets the enclosing function by casting `containingDeclarations.lastOrNull()` to a `*Symbol`; it chooses the source range by preferring `calleeReference.source` (or `expression.source`). CFIR already mirrors both (`findClosestDeclaration`, `expression.calleeReference.source ?: expression.source`). The missing piece is *which* enclosing-context predicates CFIR computes, not the mechanism.

---

## 2. Current CFIR owner path — every first-party producer of the 5 diagnostics

### 2.1 `USE_MUTABLE_FUNC_ALONE`

**Producer A — `CfirFunctionReferenceLegalityChecker`** (`CfirFunctionReferenceLegalityChecker.kt`)

- L35-45 (recovered-from-error path): `val recoveredMutFunction = expression.declaredUpperBoundMutFunctionOrNull() ?: return`; reports at L39-43.
  - Guard: unresolved call but a `mut` function was recoverable.
  - Source element: `expression.calleeReference.source ?: expression.source` (L38).
- L47-68 (resolved path): `targetFunction = targetSymbol…cfir as? CfirNamedFunction`; `if (targetFunction.status.isMut) { … }` (L62).
  - Guard: qualified access that is **not** a `CfirFunctionCall` (L33 `return`) AND resolved target `status.isMut`.
  - Source element: `expression.calleeReference.source ?: expression.source` (L49, used at L64).

**Producer B — `CfirMutFuncReferenceChecker`** (`CfirExpressionSemanticsChecker.kt`, object at L244)

- L249-262: skips `CfirFunctionCall` (L250); `function = …cfir as? CfirNamedFunction`; `if (!function.status.isMut) return` (L255).
  - Guard: qualified access is not a call AND target `status.isMut` (same shape as Producer A).
  - Source element: `expression.calleeReference.source ?: expression.source` (L258).
  - **This is a near-duplicate of Producer A (see §4.1).**

### 2.2 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION`

All three producers are in `CfirMutabilityCheckers.kt`.

- **`CfirImmutableFunctionCannotModifyFieldChecker`** (`CfirAssignmentChecker`, L96): L102-131. Guard L109 `property != null && property.status.isMut && !property.status.isStatic` → report L116-121 with `a = currentFunctionName`, `b = "set"`. Source: `expression.source ?: lValue.source` (L117).
- **`CfirImmutableFunctionCannotAccessMutableFunctionChecker`** (`CfirFunctionCallChecker`, L137): L142-167. Guard L145 `!targetFunction.status.isMut || targetFunction.status.isConst` → return; L147 `if (expression.isCurrentStructReceiverAccess())` → report L149-155 (`a = currentFunction.name`, `b = targetFunction.name`). Source: `expression.calleeReference.source ?: expression.source` (L150). (CALL case only.)
- **`CfirImmutableValueCannotAccessMutableFunctionChecker`** (`CfirFunctionCallChecker`, L232): L237-251. Guard L239 `targetFunction = expression.resolvedOrDeclaredUpperBoundMutFunctionOrNull() ?: return`; L240 `!targetFunction.status.isMut || targetFunction.status.isConst`; L241 `!receiver.isImmutableStructValueForMutableFunctionAccess()` → return; report L243-250 (`a = receiver.diagnosticNameOr(...)`, `b = targetFunction.name`). Source: `receiver.source?.includingEnclosingParentheses() ?: expression.source` (L246). (CALL case only.)

> Note: none of the three reports on a bare *reference* (non-call). See §3c.

### 2.3 `THIS_AS_EXPRESSION_IN_FUNC`

- **`CfirOpenConstructorThisUsageChecker`** (`CfirExpressionSemanticsChecker.kt`, L351): L359-374. Guard: `expression !is CfirThisReceiverExpression` → return (L360); `expression.calleeReference.isImplicit` → return (L361); `val owner = context.openClassConstructorOwner() ?: return` (L362, only open/abstract class **constructor**); parent receiver check L365 `if (parent?.explicitReceiver === expression || parent?.dispatchReceiver === expression) return`. Report L368-373.
  - Source element: `expression.calleeReference.source?.firstCharacterDiagnosticSource() ?: expression.source?.firstCharacterDiagnosticSource()` (L369-370).
  - `a = "constructor of $classKind class"`.

### 2.4 `CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC`

- **`CfirInstanceFieldCaptureChecker`** (`CfirQualifiedAccessChecker`, `CfirMutabilityCheckers.kt`, L176): L179-223. Guard: L180 `!expression.isCurrentStructReceiverAccess()` → return; L181-183 `field = expression.resolvedFieldSymbolOrNull()?.cfir as? CfirFieldVariable ?: return` (must resolve to a **field**); L188 `if (field.status.isStatic) return`; L189 `val captureContext = context.currentInstanceFieldCaptureContext(field) ?: return`. Source: `(expression.explicitReceiver as? CfirThisReceiverExpression)?.source ?: expression.calleeReference.source ?: expression.source` (L192-194). Branch on `captureContext.outerFunction` (L196): `is CfirConstructor` → `ILLEGAL_CAPTURE_THIS`; `is CfirNamedFunction` `if (outerFunction.status.isMut)` → **`CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC`** at L213-219 (`a = field.name`, `b = "mutable function '…'"`) (L212). ELSE `Unit` (L221) — i.e. non-mut-function capture is *not* reported here.

### 2.5 `MUT_ONLY_ON_FUNCTION`

- **`CfirFunctionDeclarationStatusChecker.checkMutFunction`** (`CfirFunctionSemanticsChecker.kt`, L138): L139 `if (!function.status.isMut) return`; L140 `if (function.isLocal) return`; L141 `val containingDeclaration = context.closestContainingTypeDeclaration()`.
  - If owner is `CfirStruct`/`CfirInterface` → allowed (L142-143).
  - If owner is `CfirExtend`: target `CfirStruct` → allowed; else report L148-153 (`MUT_ONLY_ON_FUNCTION`, `a = function.name`, source = `mutSource ?: function.functionNameDiagnosticSource()`).
  - Else (class/enum member) L159-163 report `MUT_ONLY_ON_FUNCTION`, source = `function.functionNameDiagnosticSource()`.

---

## 3. Seams that are MISSING (no code at all, not wrong code)

### 3a. bare `this` expression used as a value → `THIS_AS_EXPRESSION_IN_FUNC`
**No code.** The only producer of `THIS_AS_EXPRESSION_IN_FUNC` is `CfirOpenConstructorThisUsageChecker` (§2.3), gated on `context.openClassConstructorOwner()` — i.e. **only open/abstract class constructors**. There is no owner that reports `THIS_AS_EXPRESSION_IN_FUNC` for a bare `this` in a `mut` function body of a `struct` / `extend`-of-`struct` (the `record_mut_invalid_4/5` `return this` / `var a = this` cases).
- Shared-owner shape that would own it: a `CfirBasicExpressionChecker` over `CfirThisReceiverExpression` (the same shape as the three existing `this`-usage checkers: `CfirFinalizerThisUsageChecker` @ `CfirExpressionSemanticsChecker.kt:320`, `CfirOpenConstructorThisUsageChecker` @ :351, `CfirStaticContextThisUsageChecker` @ :384).
- Sub-cases (bare `this` as value vs `this` as member-access receiver) currently flow through the **same** existing owner pattern (each checker guards `parent?.explicitReceiver === expression`), but the **struct/`mut`-function value case is simply absent** from all of them — effectively one family of owners with one missing member.

### 3b. `this` captured inside a lambda (bare `{=> this}`) → `CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC`
**No code for the bare-`this` sub-case.** The single owner `CfirInstanceFieldCaptureChecker` (§2.4) is a `CfirQualifiedAccessChecker`; its guard requires the expression to resolve to a **field** (`resolvedFieldSymbolOrNull()… as? CfirFieldVariable`). A bare `this` is a `CfirThisReceiverExpression`, never a qualified access, so it is never visited. The field-qualified captures (`this.i`, implicit `i`) ARE handled; the bare `this` inside a lambda is not (the `record_mut_invalid_4/5` `let f = {=> this}` / `func goo() { this }` cases).
- The two sub-cases (field-qualified capture vs bare-`this` capture) currently flow through **one** owner (`CfirInstanceFieldCaptureChecker`, qualified-access only). The bare-`this` case falls out entirely — there is **no second owner** for it. A fix would add a parallel `CfirBasicExpressionChecker` over `CfirThisReceiverExpression` reusing `currentInstanceFieldCaptureContext` semantics but on the receiver itself.

### 3c. "mutable function referenced from an immutable (non-`mut`) function" vs "mutable function merely used as a value"
**The distinguishing code is absent.** 
- "Merely used as value" is owned by the two `USE_MUTABLE_FUNC_ALONE` producers (§2.1, A & B); neither consults the enclosing function's `mut`-ness.
- "Referenced from an immutable function" is expected to produce `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION` (fixtures `record_mut_invalid_14` L11 `return foo`; `record_mut_invalid_15` L11 `let a = cc`; `record_extend_mut_invalid_10/12`). But all three `IMMUTABLE_FUNCTION...` producers (§2.2) are **call/assignment** checkers — **none handles a bare reference** (non-call). So the reference-from-immutable-function case currently produces **nothing** for `IMMUTABLE_FUNCTION...`, while the `USE_MUTABLE_FUNC_ALONE` producers (§2.1) would fire instead (wrong diagnostic).
- Shared owner that should branch: the qualified-access reference producer (`CfirFunctionReferenceLegalityChecker` / `CfirMutFuncReferenceChecker`). The two sub-cases currently flow through **one** owner (`USE_MUTABLE_FUNC_ALONE` producers) that does **not** consult `context.findClosestDeclaration<CfirFunction>()?.status?.isMut`; the "from immutable function" routing has **no second owner** and no branch. There is also no guard for "receiver type is a struct value vs an interface" (cf. `record_mut_ok_8` expects no `USE_MUTABLE_FUNC_ALONE` for `obj: I1`).

---

## 4. Duplication / multiple-source-of-truth signals

### 4.1 `USE_MUTABLE_FUNC_ALONE` has TWO producers in TWO files
- `CfirFunctionReferenceLegalityChecker.kt` L41 & L65
- `CfirExpressionSemanticsChecker.kt` L259 (`CfirMutFuncReferenceChecker`)

Identical logic shape: skip `CfirFunctionCall` → resolve calleeReference symbol → `if (target.status.isMut)` → report on `calleeReference.source ?: expression.source`. The fix must land on the shared owner, not just one of these copies (otherwise the other keeps firing).

### 4.2 "walk `containingDeclarations` for owner + first function after it" is re-implemented ≥6 times
- `CfirExpressionSemanticsChecker.kt` L672 `openClassConstructorOwner()`
- `CfirMutabilityCheckers.kt` L333 `currentStructOwnerAndIndex()`
- `CfirMutabilityCheckers.kt` L299 `currentImmutableStructFunction()`
- `CfirMutabilityCheckers.kt` L355 `currentImmutableStructMutationContext()`
- `CfirMutabilityCheckers.kt` L390 `currentInstanceFieldCaptureContext()`
- `CfirMutabilityCheckers.kt` L418 `currentInstanceFieldCaptureOwnerAndIndex()`
- plus `findClosestDeclaration<CfirFunction>()` used at `CfirExpressionSemanticsChecker.kt` L329, L392, L442.

All recompute "find owner symbol, then first function symbol after it." The fix's new branch belongs in the same owner walk these helpers perform.

### 4.3 `isMut` re-read in many places
`CfirFunctionReferenceLegalityChecker.kt` L62; `CfirExpressionSemanticsChecker.kt` L255; `CfirMutabilityCheckers.kt` L145, L240, helper `isMutStructMemberContext` L384; `CfirFunctionDeclarationStatusChecker.kt` L139.

### 4.4 "resolve calleeReference to a symbol" duplicated as private extensions
- `CfirFunctionReferenceLegalityChecker.kt` L85 `resolvedCallableSymbolOrNull`
- `CfirExpressionSemanticsChecker.kt` L267 (`CfirMutFuncReferenceChecker`), L301 (`CfirUnsafeFuncReferenceChecker`), L508 (`CfirStaticContextNonStaticMemberAccessChecker`)
- `CfirMutabilityCheckers.kt` L478 `resolvedFunctionSymbolOrNull`, L467 `resolvedFieldSymbolOrNull`, L489 `resolvedVariableOrPropertySymbolOrNull`, L498 `resolvedPropertySymbolOrNull`

All implement the same `when (calleeReference) { CfirResolvedNamedReference / CfirNamedReferenceWithCandidateBase / … }` dispatch — same fact, many copies.

---

## Appendix: failing-fixture → expected-diagnostic evidence (structural only)

| Fixture | Expected diagnostics (from `<!…!>` markers) |
|---|---|
| `record_mut_invalid_4.cj`, `_5.cj` | `{=> this}` → `CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC`; `this.i`/`i` in lambda → same; `return this`/`var a = this` → `THIS_AS_EXPRESSION_IN_FUNC` |
| `record_mut_invalid_11.cj`, `record_extend_mut_invalid_7.cj` | `obj.foo` (free func) → `USE_MUTABLE_FUNC_ALONE` |
| `record_mut_invalid_14.cj`, `record_extend_mut_invalid_10.cj`, `record_mut_invalid_15.cj`, `record_extend_mut_invalid_12.cj` | `return foo` / `let a = cc` (reference from non-`mut` func) → `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION` |
| `record_extend_mut_invalid_1.cj` | `mut` on non-struct extend → `MUT_ONLY_ON_FUNCTION` |
| `record_extend_mut_invalid_3.cj` | same `THIS_AS_EXPRESSION_IN_FUNC` + `CAPTURE_*` pattern as `_4/_5` |
| `record_extend_mut_invalid_4.cj` | `mut` modifier incompatible between struct/interface extend → `INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE` (out of scope of the 5) |
| `record_mut_ok_7.cj`, `record_mut_ok_8.cj` | **no** `USE_MUTABLE_FUNC_ALONE` expected (interface receiver / `mut` func on `mut` impl) — boundary the current producers do not honor |
