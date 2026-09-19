# Evidence: official `cjc` 1.0.5 semantics for `mut` / `this` constructs

**Role:** Evidence Investigator only. No CFIR source was read; no fix proposed; no file edited; Gradle not run.
**Prober:** `cjc 1.0.5` (`CANGJIE_HOME=C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5`, binary `/c/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc`).
**Command form used for every probe:**
```
/c/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc <name>.cj --diagnostic-format=json --output-type=staticlib -o /tmp/out
```
(`--diagnostic-format=json` emits machine-readable `sema_*` / `parse_*` DiagKind names with `Location` `Line:Column` and a `MainHint.Range` `[Begin, End)`. Columns are 1‑based; the `Range` `End` column is **exclusive** in the sense that `End = lastCharColumn + 1`, i.e. `End - Begin` = number of characters underlined.)
**Raw probe files + full JSON outputs are saved in `D:\code\intellij\cangjie\.workbuddy\tmp\`** (`probe1.cj` … `probe5f.cj` and matching `probeN.json`). Ranges below are transcribed verbatim from those JSON files.

---

## (1) Bare mutable‑function reference inside an immutable function

### Probe `probe1.cj` (struct context)
```cangjie
  1  struct R1 {
  2      public var i = 0
  3      public mut func foo(): Unit { i += 1 }
  4      public func goo1() {
  5          return foo
  6      }
  7      public func goo2() {
  8          return foo()
  9      }
 10  }
 11  main() {
 12      return 1
 13  }
```
**Output (Diags):**
- `sema_immutable_function_cannot_access_mutable_function`, Message `"immutable function 'goo1' cannot access mutable function 'foo'"`, Location **L5:C16**, MainHint range **[L5:C16 → L5:C17)** — single character (the `f` of `foo`).
- `sema_immutable_function_cannot_access_mutable_function`, Message `"immutable function 'goo2' cannot access mutable function 'foo'"`, Location **L8:C16**, MainHint range **[L8:C16 → L8:C17)** — single character.

(2 errors; `Errors: 2`.)

### Probe `probe1b.cj` (interface context)
```cangjie
  1  interface I1 {
  2      mut func foo(): Unit {
  3      }
  4      func goo1() {
  5          return foo
  6      }
  7      func goo2() {
  8          return foo()
  9      }
 10  }
 11  main() { return 1 }
```
**Output:** identical DiagKind/Message/range as the struct case — `sema_immutable_function_cannot_access_mutable_function` at **L5:C16** and **L8:C16**, each range a single character (`[C16→C17)`).

### Conclusion (rule)
> **`sema_immutable_function_cannot_access_mutable_function` is reported iff** a `mut` member function `foo` is *named* (as a value `foo`, or called `foo()`) from within a function that is itself **not** `mut` (and not a constructor / primary constructor), **regardless of whether `foo` is declared on a struct or on an interface**. The diagnostic is anchored on the **first character** of the member name `foo` (range = 1 column), not the whole token.

---

## (2) Mutable function used as a value / accessed through a receiver (struct)

### Probe `probe2.cj`
```cangjie
  1  struct R1 {
  2      public var i = 0
  3      public mut func foo(): Unit {
  4          i += 1
  5      }
  6  }
  7
  8  func goo() {
  9      var obj = R1()
 10      var fn = obj.foo
 11      obj.foo
 12      return obj.foo
 13  }
 14
 15  main() { return 1 }
```
**Output (Diags):** all `sema_use_mutable_func_alone`, Message `"mutable function 'foo' cannot be used alone as reference"`, each with a Note `"'foo' is a mutable funciton"` pointing at the declaration `foo` (L3:C21→C24):
- L10:C18, range **[L10:C18 → L10:C21)** (the `foo` member of `obj.foo`; shifted one column but covers the `foo` member span).
- L11:C9, range **[L11:C9 → L11:C12)** = exactly `foo` (3 chars).
- L12:C16, range **[L12:C16 → L12:C19)** = exactly `foo` (3 chars).

(3 errors; `Errors: 3`.)

### Probe `probe2b.cj` (the `let` / immutable‑value variant)
```cangjie
  1  struct R1 {
  2      public var i = 0
  3      public mut func foo(): Unit { i += 1 }
  4  }
  5
  6  func gooLet() {
  7      let obj = R1()
  8      obj.foo
  9      return obj.foo
 10  }
 11
 12  func gooCall() {
 13      var obj = R1()
 14      obj.foo()
 15      return
 16  }
```
**Output:** `obj.foo` used alone through a **`let obj`** → `sema_immutable_access_mutable_func`, Message `"cannot use mutable function on immutable value"`, each with Note `"'obj' is a variable declared with 'let'"`:
- L8 (standalone `obj.foo`): Location **L8:C5**, range **[L8:C5 → L8:C8)** (the `obj` base, 3 chars).
- L9 (`return obj.foo`): Location **L9:C12**, range **[L9:C12 → L9:C15)** (the `obj` base, 3 chars).

`gooCall` (`var obj; obj.foo()` — called, not taken as value): **no error**.

### Conclusion (rule)
> For `obj.foo` where `obj` has **struct** static type:
> - taken as a value / used alone (`obj.foo`, `var fn = obj.foo`, `return obj.foo`) → **`sema_use_mutable_func_alone`** iff `obj` is a `var` (mutable binding). Anchor: the `foo` member span.
> - taken as a value through a **`let`** (immutable) binding → **`sema_immutable_access_mutable_func`** (`"cannot use mutable function on immutable value"`). Anchor: the `obj` base.
> - actually *called* (`obj.foo()`) on a `var` → **no error**.
>
> Both diagnostics fire **only for struct receivers** (see §3 / citations).

---

## (3) `mut` func declared on an interface, accessed through an interface‑typed variable

### Probe `probe3a.cj` (interface‑typed `let`, called)
```cangjie
  1  interface I1 { mut func foo(): Unit }
  2
  3  struct R1 <: I1 {
  4      public var i = 0
  5      public mut func foo() { i = i + 1 }
  6  }
  7
  8  main() {
  9      let obj: I1 = R1()
 10      obj.foo()
 11      return 0
 12  }
```
**Output:** `Errors: 0`. Only `chir_dce_unused_function_main` (a `-Wunused` warning, not an error). **No `mut`‑related diagnostic.**

### Probe `probe3b.cj` (interface‑typed `var`, taken as value)
```cangjie
  1  interface I1 { mut func foo(): Unit }
  2
  3  struct R1 <: I1 {
  4      public var i = 0
  5      public mut func foo(): Unit { i = i + 1 }
  6  }
  7
  8  main() {
  9      var obj: I1 = R1()
 10      var fn = obj.foo
 11      return 0
 12  }
```
**Output:** `Errors: 0`. Only `chir_dce_unused_function_main` and `chir_dce_unused_variable` warnings. **No `use_mutable_func_alone` / no `immutable_access_mutable_func`.**

### Sub‑question: "different rule for a `mut func` declared on an interface vs on a struct?"
**Answer: NO — the rule does not depend on where the `mut func` is *declared*. It depends on the *static type of the receiver expression* `obj`.** The check that emits `sema_use_mutable_func_alone` / `sema_immutable_access_mutable_func` (the C++ `CheckLetInstanceAccessMutableFunc`) bails out immediately unless `MaybeStruct(*ma.baseExpr->ty)` is true — i.e. it only runs when the receiver is a **struct (value type)**. An `interface` (and any class/reference type) receiver is **not** a struct, so the whole check is skipped and neither diagnostic is produced. (See citations `TypeCheckAccess.cpp:392‑395`.)

Note this is distinct from §1: §1's check (`CheckImmutableFuncAccessMutableFunc`) fires on the *bare* name `foo` inside an immutable function and does **not** consult the receiver type, which is why the interface form in `probe1b` still errors.

### Conclusion (rule)
> For `obj.foo` where `obj` has an **interface (or any class‑like / reference) static type**:
> - called `obj.foo()` → **no error**.
> - taken as a value `var fn = obj.foo` → **no error**.
>
> I.e. **the `use_mutable_func_alone` / `immutable_access_mutable_func` diagnostics are suppressed whenever the receiver's static type is not a struct.** A `mut func` reached through interface dispatch is exempt from these two checks.

---

## (4) Bare `this` used as an expression

### Probe `probe4a.cj` (direct use inside a `mut` func)
```cangjie
  1  struct R1 {
  2      public var i = 0
  3      public mut func foo5(): R1 {
  4          return this
  5      }
  6      public mut func foo6() {
  7          var a = this
  8          return a
  9      }
 10      public func gooPlain() {
 11          this
 12      }
 13  }
 14  main() { return 1 }
```
**Output:**
- `sema_use_this_as_an_expression_in_func`, `"'this' cannot be used as an expression in the mutable function 'foo5'"`, Location **L4:C16**, range **[L4:C16 → L4:C17)** — single char (`t` of `this`).
- `sema_use_this_as_an_expression_in_func`, `"'this' cannot be used as an expression in the mutable function 'foo6'"`, Location **L7:C17**, range **[L7:C17 → L7:C18)** — single char.
- `gooPlain()` (plain/immutable func, bare `this` statement): **no error** (only the 2 diagnostics above; `Errors: 2`).

### Probe `probe4b.cj` (capture in nested func / lambda, inside `mut` funcs)
```cangjie
  1  struct R1 {
  2      public var i = 0
  3      public mut func foo1() {
  4          let f = { => this }
  5          func goo() { this }
  6          return 0
  7      }
  8      public mut func foo2() {
  9          let f = { => this.i = 2 }
 10          func goo() { this.i = 2 }
 11          return 0
 12      }
 13      public mut func foo3() {
 14          let f = { => this.i }
 15          func goo() { this.i }
 16          return 0
 17      }
 18      public mut func foo4() {
 19          let f = { => i }
 20          func goo() { i }
 21          return 0
 22      }
 23  }
 24  main() { return 1 }
```
**Output (8 diagnostics), all `sema_capture_this_or_instance_field_in_func`:**
| # | Message | Location | MainHint range |
|---|---------|----------|----------------|
| foo1 lambda `{=>this}` | `"'this' cannot be captured in the mutable function 'foo1'"` | L4:C22 | [L4:C22→C23) single char `t` |
| foo1 nested `goo(){this}` | `"'this' cannot be captured in the mutable function 'foo1'"` | L6:C13 | [L6:C13→C14) single char `t` |
| foo2 lambda `{=>this.i=2}` | `"'this' cannot be captured in the mutable function 'foo2'"` | L9:C22 | [L9:C22→C23) single char `t` |
| foo2 nested `this.i=2` | `"'this' cannot be captured in the mutable function 'foo2'"` | L10:C13 | [L10:C13→C14) single char `t` |
| foo3 lambda `{=>this.i}` | `"'this' cannot be captured in the mutable function 'foo3'"` | L13:C22 | [L13:C22→C23) single char `t` |
| foo3 nested `this.i` | `"'this' cannot be captured in the mutable function 'foo3'"` | L15:C13 | [L15:C13→C14) single char `t` |
| foo4 lambda `{=>i}` | `"'i' cannot be captured in the mutable function 'foo4'"` | L18:C22 | [L18:C22→C23) single char `i` |
| foo4 nested `i` | `"'i' cannot be captured in the mutable function 'foo4'"` | L20:C13 | [L20:C13→C14) single char `i` |

**Key distinction:** for `this.i` the message names **`'this'`** and the range anchors on the **`t`** of `this` (not on `.i`). For a bare instance field `i`, the message names **`'i'`** and anchors on `i`.

### Probe `probe4c.cj` (direct use inside a *plain* func)
```cangjie
  1  struct R1 {
  2      public var i = 0
  3      public func gooA(): R1 { return this }
  4      public func gooB() { var a = this; return a }
  5      public func gooC() { this.i = 2; return }
  6  }
  7  main() { return 1 }
```
**Output:** `return this` (gooA) and `var a = this` (gooB) → **no error**. `this.i = 2` in the immutable func → `sema_cannot_modify_var` `"instance member variable 'i' cannot be modified in immutable function"`, Location **L5:C9**, range **[L5:C9→C10)** (single char `i`). `Errors: 1`.

### Probe `probe4d.cj` (capture inside a *plain* func)
```cangjie
  1  struct R1 {
  2      public var i = 0
  3      public func plainGoo() {
  4          func inner() { this }
  5          let f = { => this }
  6          return
  7      }
  8  }
  9  main() { return 1 }
```
**Output:** `Errors: 0` — only `chir_dce_unused_function` / `chir_dce_unused_variable` / `chir_dce_unused_function_main` warnings. **No capture diagnostic** (the outermost function is not `mut`).

### Conclusion (rule)
> **`sema_use_this_as_an_expression_in_func`** is reported iff `this` is used *directly as an expression* (`return this`, `var a = this`) **and** the outermost enclosing function is `mut` (or a finalizer, or the constructor of an inheritable class). Anchor: first char `t` of `this` (1 column).
>
> **`sema_capture_this_or_instance_field_in_func`** is reported iff `this` or an instance field is referenced inside a **nested function or lambda**, **and** the outermost enclosing function is `mut`. Anchor: single char — `t` of `this` (message names `'this'`, even for `this.i`), or `i` for a bare instance field (message names `'i'`).
>
> Bare `this` (no member) and `this.member` both reach the **same** capture diagnostic inside a closure; the only difference is the message names the captured entity. In a **plain/immutable** function, neither diagnostic fires (direct `this` is allowed; `this.i = ...` is instead rejected by `sema_cannot_modify_var`).

---

## (5) `mut` applied to a non‑function declaration

> **Important:** the fixture `record_extend_mut_invalid_1.cj` expects a diagnostic named **`MUT_ONLY_ON_FUNCTION`**. That name does **not exist** anywhere in `cjc` 1.0.5 (it is absent from every `*.def` diagnostic table and from the C++ sources). The official diagnostics are different.

### Probe `probe5a.cj` — `mut` on a field in a class
```cangjie
  1  class C1 {
  2      public mut var i: Int64 = 0
  3  }
  4  main() { return 1 }
```
→ `parse_illegal_modifier_in_scope`, `"unexpected modifier 'mut' on variable declaration in class body"`, **L2:C12**, range **[L2:C12→C15)** = `mut` (3 chars).

### Probe `probe5b.cj` — `mut func` inside an `extend` body
```cangjie
  1  class C1 { public var i: Int64 = 0 }
  2  extend C1 {
  3      public mut func foo() { this.i = 1 }
  4  }
  5  main() { return 1 }
```
→ `sema_invalid_mut_modifier_extend_of_struct`, `"'mut' modifier is illegal in extend body of 'C1'"`, **L3:C12**, range **[L3:C12→C15)** = `mut` (3 chars).
*(This is the exact case the `record_extend_mut_invalid_1.cj` fixture tags as `MUT_ONLY_ON_FUNCTION` — the official name is `sema_invalid_mut_modifier_extend_of_struct`, and the anchor `mut` matches.)*

### Probe `probe5c.cj` — `mut` on a local var
```cangjie
  1  func goo() { mut var x = 0; return x }
  2  main() { return 1 }
```
→ `parse_illegal_modifier_in_scope`, `"unexpected modifier 'mut' on variable declaration in function body"`, **L1:C5**, range **[L1:C5→C8)** = `mut`.

### Probe `probe5d.cj` — `mut` on a top‑level function
```cangjie
  1  mut func topLevel(): Unit { return }
  2  main() { return 1 }
```
→ `parse_illegal_modifier_in_scope`, `"unexpected modifier 'mut' on function declaration in 'top-level' scope"`, **L1:C1**, range **[L1:C1→C4)** = `mut`.

### Probe `probe5e.cj` — `mut` on a var in a struct body
```cangjie
  1  struct R1 { public mut var i: Int64 = 0 }
  2  main() { return 1 }
```
→ `parse_illegal_modifier_in_scope`, `"unexpected modifier 'mut' on variable declaration in struct body"`, **L1:C12**, range **[L1:C12→C15)** = `mut`.

### Probe `probe5f.cj` — `mut let`
```cangjie
  1  func goo() { mut let x = 0; return x }
  2  main() { return 1 }
```
→ `parse_illegal_modifier_in_scope`, `"unexpected modifier 'mut' on variable declaration in function body"`, **L1:C5**, range **[L1:C5→C8)** = `mut`.

### Conclusion (rule)
> **`mut` may only legally appear on instance member functions of a struct/class/interface body.** Applied anywhere else it is rejected:
> - on a `var`/`let`/field/property declaration (class body, struct body, or function body) → **`parse_illegal_modifier_in_scope`** ("unexpected modifier 'mut' on variable declaration in `<scope>`"), anchor = the `mut` token.
> - on a function at top level → **`parse_illegal_modifier_in_scope`** ("...on function declaration in 'top-level' scope").
> - on a `func` inside an `extend` body → **`sema_invalid_mut_modifier_extend_of_struct`** ("'mut' modifier is illegal in extend body of 'C1'"), anchor = the `mut` token.
>
> **`MUT_ONLY_ON_FUNCTION` is NOT an official `cjc` diagnostic** — the project fixture's expectation name does not correspond to any real `cjc` 1.0.5 diagnostic.

---

## Official `external/cangjie_compiler` citations

Compiler: `D:\code\intellij\cangjie\external\cangjie_compiler` (read‑only mirror). All paths relative to that root.

**§1 `immutable_function_cannot_access_mutable_function` (bare `foo` access):**
- `src/Sema/TypeCheckExpr.cpp:194` — `CheckImmutableFuncAccessMutableFunc(...)`; deciding condition at `:198‑203`:
  > `bool accessMutableTarget = (destNode.astKind == FUNC_DECL && destNode.TestAttr(MUT)) || (PROP_DECL && isLeftStructValue && isVar);` … fires iff `!fdSrc->TestAttr(MUT) && outerDecl && bothInstance && !constructor && !primary_constructor && accessMutableTarget`.
- Diagnostic text: `include/cangjie/Basic/DiagnosticSema.def:291` — `ERROR(sema_immutable_function_cannot_access_mutable_function, "immutable function '%s' cannot access mutable function '%s'")`.
- Call site for the bare (no‑receiver) path: `src/Sema/TypeCheckReference.cpp:598`; also `src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:1076`.

**§2 / §3 `use_mutable_func_alone` and `immutable_access_mutable_func` (member access `obj.foo`):**
- `src/Sema/TypeCheckAccess.cpp:389` — `CheckLetInstanceAccessMutableFunc(...)`. **The interface‑vs‑struct guard** at `:392‑395`:
  > `if (!target.TestAttr(MUT) || target.astKind != FUNC_DECL || !ma.baseExpr->ty || !MaybeStruct(*ma.baseExpr->ty)) { return; }`
  → for a non‑struct (interface / class) receiver the whole check is skipped, suppressing both diagnostics.
- The `let`/immutable‑value branch at `:411‑413`:
  > `bool immutableAccessMutableFunc = vd && (vd->astKind == PROP_DECL || !vd->isVar) && Ty::IsTyCorrect(vd->ty) && !vd->ty->IsClassLike();`
  → emits `DiagImmutableAccessMutableFunc` (i.e. `sema_immutable_access_mutable_func`).
- The take‑as‑value branch at `:433‑435`:
  > `if (ma.callOrPattern == nullptr) { … diag.DiagnoseRefactor(sema_use_mutable_func_alone, ma, range, ma.field.Val()); }`
- `sema_immutable_access_mutable_func` text emitted at `src/Sema/Diags.cpp:333‑336` (`DiagImmutableAccessMutableFunc` → `DiagKindRefactor::sema_immutable_access_mutable_func`).
- Diagnostic texts: `include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:42` (`sema_use_mutable_func_alone`, "mutable function '%s' cannot be used alone as reference"); `:45` (`sema_immutable_access_mutable_func`, "cannot use mutable function on immutable value").
- Caller: `src/Sema/TypeCheckReference.cpp:717` (`CheckLetInstanceAccessMutableFunc(ctx, ma, target)`).

**§4 `use_this_as_an_expression_in_func` and `capture_this_or_instance_field_in_func`:**
- `src/Sema/TypeCheckReference.cpp:150` — `CheckUsageOfThis(...)`.
- Capture condition at `:170‑176`:
  > `bool lambdaOrNestedFuncInMutFunc = outMostFunc->TestAttr(MUT) && curFuncBody && (curFuncBody->funcDecl != outMostFunc);` → `diag.Diagnose(re, sema_capture_this_or_instance_field_in_func, ...)`.
- Expression condition at `:177‑188`:
  > `bool referenceNotAllowed = re.isAlone && (outMostFunc->TestAttr(MUT) || outMostFunc->IsFinalizer() || (insideConstructor && IsInheritableClass(*outerDecl)));` → `diag.Diagnose(re, sema_use_this_as_an_expression_in_func, info);`
- Bare instance‑field capture (the `i` case) handled in `src/Sema/TypeCheckExpr.cpp:170` — `CanTargetOfRefBeCapturedCaseMutFunc(...)`, condition `:177‑179`:
  > `if (fd->TestAttr(MUT) && curFuncBody.funcDecl != fd && decl.TestAttr(IN_STRUCT) && decl.astKind == VAR_DECL)` → `diag.Diagnose(nre, sema_capture_this_or_instance_field_in_func, decl.identifier.Val(), "mutable function '"+fd->identifier+"'");`
- Diagnostic texts: `include/cangjie/Basic/DiagnosticSema.def:287` (`sema_capture_this_or_instance_field_in_func`, "'%s' cannot be captured in the %s"); `:288` (`sema_use_this_as_an_expression_in_func`, "'this' cannot be used as an expression in the %s").

**§5 `mut` on non‑function:**
- `parse_illegal_modifier_in_scope` (parser): `include/cangjie/Basic/DiagRefactor/DiagnosticParser.def:255` — `ERROR(parse_illegal_modifier_in_scope, "unexpected modifier '%s' on %s%s", "unexpected modifier", {...})`. Raised by `src/Parse/ParserDiag.cpp`.
- `sema_invalid_mut_modifier_extend_of_struct`: `include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:155` — `ERROR(sema_invalid_mut_modifier_extend_of_struct, "'mut' modifier is illegal in extend body of '%s'")`.
- **`MUT_ONLY_ON_FUNCTION`**: not present in any `*.def` table under `include/cangjie/Basic/` nor anywhere in `src/` — confirmed absent.

---

## Ambiguities / unresolved

1. **`MUT_ONLY_ON_FUNCTION` is fictitious w.r.t. official `cjc`.** The fixture `record_extend_mut_invalid_1.cj` tags the `mut` token with `MUT_ONLY_ON_FUNCTION`, but official cjc 1.0.5 emits `sema_invalid_mut_modifier_extend_of_struct` for that exact case (and `parse_illegal_modifier_in_scope` for `mut` on fields/vars). I could not find any `cjc` diagnostic named `MUT_ONLY_ON_FUNCTION`. This is a **project‑side naming mismatch**, not an official semantic. Whether the Kotlin implementation should rename its diagnostic or whether some other `cjc` version/flag produces `MUT_ONLY_ON_FUNCTION` was **not** determined (only cjc 1.0.5 was available).
2. **Exact column arithmetic of the `use_mutable_func_alone` MainHint range** is slightly inconsistent across occurrences (e.g. `probe2` L10 range begins at C18, one column past the `foo` start at C17, while L11/L12 ranges align exactly to `foo`). cjc reports the value as‑is; I report the raw numbers but did not reverse‑engineer why the L10 span is shifted by one. The diagnostic is reliably on the `foo` member; the precise Begin/End columns should be taken from the JSON, not assumed to be the whole `foo` token.
3. **Range granularity mismatch vs project fixtures.** Official cjc anchors `immutable_function_cannot_access_mutable_function` and both `this` diagnostics on a **single character** (Begin→Begin+1), whereas the project fixtures (`record_mut_invalid_14.cj`, `record_mut_invalid_4.cj`, etc.) mark the **whole `foo` / `this` / `i` token**. This is a range‑width discrepancy between official cjc and the project's expectation markers; I did not resolve which the project intends to keep.
4. **`this` in a primary/secondary constructor or finalizer** was not separately probed; the C++ condition (`:178‑180`) shows `use_this_as_an_expression_in_func` also fires for finalizers and constructors of inheritable classes, but no standalone `cjc` probe confirmed those specific messages/anchors. Treated as inferred‑from‑source, not probed.
5. **Calling a struct `mut func` through an immutable (`let`) struct binding** (`let obj = R1(); obj.foo()`) was not probed; only the take‑as‑value `let` case (`sema_immutable_access_mutable_func`) and the `var` call case (no error) were observed. The call‑on‑`let`‑struct variant's behavior is left unresolved (likely `sema_immutable_access_mutable_func` or `sema_cannot_assign_to_immutable`, but unverified).
