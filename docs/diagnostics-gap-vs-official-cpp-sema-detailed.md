# 仓颉官方 C++ 编译器 vs 本项目 语义分析诊断差距详表

## 1. 范围

本文档**只讨论语义分析（Sema）**，不再覆盖：

- lexer
- parser
- parser query
- frontend / driver
- module / package
- conditional compilation
- incremental compilation
- macro expansion
- CHIR / backend

本文档覆盖的官方来源仅限：

- `external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def`
- `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def`
- `external/cangjie_compiler/src/Sema/**/*`

本文档关注三件事：

1. 官方具体语义诊断是什么；
2. 什么背景、什么代码会触发；
3. 本项目当前是否已有对应实现，若没有，缺在定义、producer、映射还是回归。

---

## 2. 官方 Sema 版图

官方语义诊断条目规模：

| 文件 | 条目数 |
|---|---:|
| `DiagnosticSema.def` | 231 |
| `DiagRefactor/DiagnosticSema.def` | 285 |

从官方 `DiagRefactor/DiagnosticSema.def` 的组织看，语义分析至少覆盖这些子域：

- General
- Function / Call
- Expression
- Generic
- Inheritance
- Extend
- Property
- Const evaluation
- Annotation
- inout
- Java interoperation
- VArray
- CFFI
- Unit test / mocking
- effects
- common/specific（CJMP）
- java mirror

本项目当前真正接通的主链仍集中在：

- imports / redeclaration
- supertype / override / extend
- declaration-status
- type mismatch / argument mismatch / return mismatch
- const-eval 的一小部分
- match exhaustiveness
- 可见性局部路径

结论：

- 从“只看语义分析”这个范围来说，本项目已经有可扩展的 CFIR 诊断框架；
- 但离官方 Sema 的全景覆盖仍有明显差距，尤其在 `call/constructor`、`initialization`、`generic-access`、`pattern legality`、`mut/immutable`、`interop`、`effects`、`CJMP`。

---

## 3. 本项目当前语义诊断入口

本项目当前 Sema 对应入口主要是：

- 诊断定义源：
  - `cfir/checkers/checkers-component-generator/src/.../CfirDiagnosticsList.kt`
- checker producer：
  - `cfir/checkers/src/.../analysis/checkers/**/*`
- 解析后错误映射：
  - `cfir/checkers/src/.../analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`
- 错误节点收集：
  - `cfir/checkers/src/.../analysis/collectors/components/ErrorNodeDiagnosticCollectorComponent.kt`
- 回归样例：
  - `cfir/analysis-tests/testData/diagnostics/**/*`

现状判断：

- 已有约 71 个 CFIR 诊断工厂；
- 已有约 120 个 `.cj` 诊断样例；
- 但其中大量样例集中在 `operator/`、`coverage/extensions/`、`type-mismatch/`、`coverage/inheritance/`；
- 官方 Sema 中大量语义子域在本项目仍没有独立目录与建模。

---

## 4. General / 基础语义

### 4.1 `sema_undeclared_identifier`

背景：

- 名称未声明。

最小触发示例：

```cj
func f(): Unit {
    missingValue
}
```

本项目对应：

- `UNRESOLVED_REFERENCE`

本项目状态：

- 已实现；
- 有样例：`diagnostics/unresolved/unresolvedReferenceName.cj`

差距：

- 本项目当前大量 unresolved 都收敛为统一诊断；
- 官方会在更多上下文里继续细分为 call / type / package / generic-bound 等专门语义。

### 4.2 `sema_redefinition`

背景：

- 重定义声明。

最小触发示例：

```cj
class A {}
class A {}
```

本项目对应：

- `REDECLARATION`
- `CLASSIFIER_REDECLARATION`

本项目状态：

- 已实现；
- 有样例：`diagnostics/redeclaration/simple.cj`

### 4.3 `sema_mismatched_types`

背景：

- 基础类型不匹配。

最小触发示例：

```cj
func f(): Int64 {
    return true
}
```

本项目对应：

- `TYPE_MISMATCH`
- `RETURN_TYPE_MISMATCH`
- `ARGUMENT_TYPE_MISMATCH`
- `ASSIGNMENT_TYPE_MISMATCH`

本项目状态：

- 已实现；
- 这一域是当前项目最稳定的一块之一。

---

## 5. Function / Call / Constructor

这是本项目相对官方差距最大的语义子域之一。

### 5.1 `sema_no_match_function_declaration_for_call`

背景：

- 调用找不到匹配函数声明。

最小触发示例：

```cj
func foo(a: Int64): Unit {}
foo(true)
```

本项目当前表现：

- 通常会落到：
  - `ARGUMENT_TYPE_MISMATCH`
  - 或 `UNRESOLVED_REFERENCE`
- 没有完全对齐官方“调用绑定层”的细粒度错误组织。

### 5.2 `sema_no_match_constructor`

背景：

- 构造器不匹配。

最小触发示例：

```cj
class Box {
    init(v: Int64) {}
}

func f(): Unit {
    let _ = Box()
}
```

官方 producer：

- `src/Sema/TypeCheckCall.cpp:2598`

本项目对应：

- `NO_CONSTRUCTOR`

本项目状态：

- 基础诊断存在；
- 但没有形成 `constructor/` 诊断域和专门回归套件；
- 目前仅零散出现在 enum/调用场景里。

### 5.3 `sema_ambiguous_constructor_match`

背景：

- 构造器候选歧义。

最小触发示例：

```cj
class Box {
    init(v: Int64) {}
    init(v: UInt64) {}
}

let _ = Box(1)
```

官方 producer：

- `src/Sema/TypeCheckCall.cpp:2608`

本项目状态：

- 当前没有同等级的专门诊断建模；
- 仍属于明显缺口。

### 5.4 `sema_unknown_named_argument`

背景：

- 调用中使用未知 named argument。

最小触发示例：

```cj
func foo(a: Int64, b: Int64): Unit {}
foo(c: 1, b: 2)
```

官方 producer：

- `src/Sema/TypeCheckCall.cpp:77`
- `src/Sema/TypeCheckBuiltinExpr.cpp:386`

本项目状态：

- 没有 `call/` 诊断域；
- 缺专门定义、producer 与样例。

### 5.5 `sema_multiple_named_argument`

背景：

- 同一个 named argument 重复出现。

最小触发示例：

```cj
func foo(a: Int64): Unit {}
foo(a: 1, a: 2)
```

官方 producer：

- `src/Sema/TypeCheckCall.cpp:72`

本项目状态：

- 缺失。

### 5.6 `sema_unordered_arguments`

背景：

- 命名参数后面又出现位置参数。

最小触发示例：

```cj
func foo(a: Int64, b: Int64): Unit {}
foo(a: 1, 2)
```

官方 producer：

- `src/Sema/TypeCheckCall.cpp:517`
- `src/Sema/TypeCheckCall.cpp:643`

本项目状态：

- 缺失。

### 5.7 `sema_recursive_constructor_call`

背景：

- 构造器递归调用自己。

最小触发示例：

```cj
class Loop {
    init() {
        this()
    }
}
```

官方 producer：

- `src/Sema/Utils.cpp:198`

本项目状态：

- 缺失。

### 5.8 `sema_illegal_place_of_calling_this_or_super`

背景：

- `this(...)` / `super(...)` 出现在构造器非法位置。

最小触发示例：

```cj
open class Base {
    init(v: Int64) {}
}

class Child <: Base {
    init() {
        let x = 1
        super(1)
    }
}
```

官方 producer：

- `src/Sema/TypeChecker.cpp:1369`

本项目状态：

- 缺失。

### 本域结论

- 这是本项目最需要优先补齐的语义域之一；
- 官方在“调用绑定级诊断”上远比本项目细。

---

## 6. Initialization / LegalityOfUsage

### 6.1 `sema_used_before_initialization`

背景：

- 变量在初始化前被读取。

最小触发示例：

```cj
func f(): Int64 {
    let v: Int64
    return v
}
```

官方 producer：

- `src/Sema/LegalityOfUsage/InitializationChecker.cpp:812`
- `src/Sema/LegalityOfUsage/GlobalVarChecker.cpp:649`

本项目状态：

- 没有 `initialization/` 诊断域；
- 基本缺席。

### 6.2 `sema_class_uninitialized_field`

背景：

- class 字段在构造结束前未初始化。

最小触发示例：

```cj
class Holder {
    let value: Int64

    init() {
    }
}
```

官方 producer：

- `src/Sema/LegalityOfUsage/InitializationChecker.cpp:387`

本项目状态：

- 基本缺席。

### 本域结论

- 这是官方有专门检查器而本项目当前几乎没有覆盖的一整块。

---

## 7. Inheritance / Override / Super

这是本项目最近补得最完整的一块。

### 7.1 `sema_return_type_invariance`

背景：

- override 返回类型不满足要求。

最小触发示例：

```cj
open class Base {}
class Derived <: Base {}

open class Parent {
    open func value(): Derived {
        return Derived()
    }
}

class Child <: Parent {
    override func value(): Base {
        return Base()
    }
}
```

官方 producer：

- `src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1394`

本项目对应：

- `OVERRIDING_RETURN_TYPE_MISMATCH`

本项目状态：

- 已实现；
- 样例：
  - `coverage/inheritance/overrideReturnType.cj`
  - `coverage/inheritance/overrideReturnTypeMismatchRich.cj`

### 7.2 `sema_use_super_in_interface`

背景：

- interface 中使用 `super`。

最小触发示例：

```cj
interface I {
    func f(): Unit {
        super
    }
}
```

官方 producer：

- `src/Sema/TypeCheckReference.cpp:225`

本项目对应：

- `INTERFACE_SUPER_NOT_ALLOWED`

本项目状态：

- 已实现；
- 有样例：`super/repeated_inheritance.cj`

### 7.3 `sema_super_use_error_inside_non_class`

背景：

- struct / enum 中非法使用 `super`。

最小触发示例：

```cj
struct S {
    func f(): Unit {
        super
    }
}
```

官方 producer：

- `src/Sema/TypeCheckReference.cpp:231`

本项目对应：

- `STRUCT_SUPER_NOT_ALLOWED`
- `ENUM_SUPER_NOT_ALLOWED`

本项目状态：

- 已实现；
- 有样例：`super/illegalSuperInStructAndEnum.cj`

### 7.4 官方 final / sealed / visibility override 语义

官方代表诊断：

- `sema_cannot_override`
- `sema_weak_visibility`
- `sema_cannot_inherit_sealed`
- `sema_return_type_incompatible`

本项目当前对应：

- `NOTHING_TO_OVERRIDE`
- `CANNOT_WEAKEN_ACCESS_PRIVILEGE`
- `CANNOT_OVERRIDE_INVISIBLE_MEMBER`
- `CLASS_NOT_OPEN_FOR_INHERITANCE`
- `OVERRIDING_RETURN_TYPE_MISMATCH`

本项目状态：

- 已覆盖部分；
- `sealed` 继承限制仍未展开成官方同等级语义。

---

## 8. Extend

### 8.1 `sema_illegal_extended_type`

背景：

- 非法扩展目标。

最小触发示例：

```cj
interface I {}
extend I {}
```

本项目对应：

- `ILLEGAL_EXTENDED_TYPE`

本项目状态：

- 已实现；
- 有样例：`coverage/extensions/illegalExtendedTypeRich.cj`

### 8.2 `sema_extend_not_interface`

背景：

- extend 声明右侧不是接口。

最小触发示例：

```cj
class Host {}
class Plain {}
extend Host <: Plain {}
```

本项目对应：

- `EXTEND_NOT_INTERFACE`

本项目状态：

- 已实现；
- 有样例：`coverage/extensions/extendNotInterfaceRich.cj`

### 8.3 `sema_extend_duplicate_interface`

背景：

- extend 重复实现接口。

最小触发示例：

```cj
interface Display {}
class Host {}
extend Host <: Display & Display {}
```

本项目对应：

- `EXTEND_DUPLICATE_INTERFACE`

本项目状态：

- 已实现。

### 8.4 `sema_extend_use_super`

背景：

- extend 体内使用 `super`。

最小触发示例：

```cj
open class Base {
    open func value(): Int64 { return 1 }
}

extend Base {
    func f(): Int64 {
        return super.value()
    }
}
```

本项目对应：

- `EXTEND_SUPER_NOT_ALLOWED`

本项目状态：

- 已实现。

### 8.5 `sema_c_type_cannot_extend_interface`

背景：

- C 类型不能参与 extend interface。

最小触发示例：

```cj
@C
class NativeBox {}

interface Printable {}

extend NativeBox <: Printable {}
```

官方 producer：

- `src/Sema/TypeCheckExtend.cpp:318`

本项目对应：

- `EXTEND_C_TYPE_NOT_ALLOWED`

本项目状态：

- 已实现。

### 本域结论

- extend 是本项目当前最接近官方的子域之一；
- 但官方仍有更多导出顺序、序列决策、shadow 规则未对齐。

---

## 9. Generic

### 9.1 `sema_generic_type_without_type_argument`

背景：

- 裸用泛型类型。

最小触发示例：

```cj
class Box<T> {
    let value: T
}

func f(v: Box): Unit {}
```

官方 producer：

- `src/Sema/TypeCheckType.cpp:241`
- `src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:45`
- `src/Sema/TypeArgumentInference.cpp:520`

本项目对应：

- `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`

本项目状态：

- 已定义；
- 但 producer 与测试仍缺，是当前最明确的遗留项之一。

### 9.2 `sema_generic_no_member_match_in_upper_bounds`

背景：

- 上界里不存在成员。

最小触发示例：

```cj
interface Named {
    func name(): String
}

func bad<T>(v: T): Int64 where T <: Named {
    return v.id
}
```

官方 producer：

- `src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:772`

本项目状态：

- 还没有对应负例与专门诊断；
- 目前大概率退化成 `UNRESOLVED_REFERENCE`。

### 9.3 `sema_generic_no_method_match_in_upper_bounds`

背景：

- 上界里不存在方法。

最小触发示例：

```cj
interface Named {
    func name(): String
}

func bad<T>(v: T): Unit where T <: Named {
    v.reset()
}
```

官方 producer：

- `src/Sema/TypeCheckCall.cpp:2594`

本项目状态：

- 缺失。

### 本域结论

- 泛型约束类 checker 有一些基础；
- 但泛型成员解析语义仍显著弱于官方。

---

## 10. Visibility / Access Control

### 10.1 `sema_invalid_access_control`

背景：

- 访问不可见声明。

最小触发示例：

```cj
// FILE: a.cj
package p
private func hidden(): Int64 { return 1 }

// FILE: b.cj
package p
func use(): Int64 { return hidden() }
```

官方 producer：

- `src/Sema/TypeCheckAccess.cpp:139`
- `src/Sema/TypeCheckType.cpp:306`

本项目对应：

- `INVISIBLE_REFERENCE`
- `INVISIBLE_MEMBER`

本项目状态：

- 已在调用解析链路接通；
- 有样例：`visibility/invisibleReferenceAndMemberRich.cj`

差距：

- package/internal/protected/private 的全矩阵仍未系统展开。

### 10.2 “有候选但不可见”的 override

本项目对应：

- `CANNOT_OVERRIDE_INVISIBLE_MEMBER`

本项目状态：

- 已实现；
- 这是近期新增对齐能力。

---

## 11. Match / Pattern

### 11.1 `sema_tuple_pattern_not_match`

背景：

- tuple pattern 用在非 tuple 值上。

最小触发示例：

```cj
func f(v: Int64): Unit {
    match (v) {
        case (a, b) => ()
    }
}
```

官方 producer：

- `src/Sema/TypeCheckPattern.cpp:485`

本项目状态：

- 未实现同等级诊断。

### 11.2 `sema_pattern_not_match`

背景：

- pattern 与目标不匹配。

最小触发示例：

```cj
enum Option<T> {
    | Some(T)
    | None
}

match (Option<Int64>.None) {
    case 1 => ()
}
```

官方 producer：

- `src/Sema/TypeCheckPattern.cpp:405`

本项目状态：

- 未实现同等级诊断。

### 11.3 `sema_enum_pattern_param_size_error`

背景：

- enum pattern 参数个数错误。

最小触发示例：

```cj
enum Option<T> {
    | Some(T)
    | None
}

match (Option<Int64>.Some(1)) {
    case Some() => ()
}
```

官方 producer：

- `src/Sema/TypeCheckPattern.cpp:554`

本项目状态：

- 未实现。

### 本域结论

- 本项目现在只有 exhaustiveness 比较完整；
- pattern legality 本身仍是明显空白。

---

## 12. Immutable / mut / inout

### 12.1 `sema_cannot_modify_var`

背景：

- 在不允许修改的位置修改变量。

最小触发示例：

```cj
struct Counter {
    var value: Int64 = 0

    func readOnly(): Unit {
        value = 1
    }
}
```

官方 producer：

- `src/Sema/TypeCheckAccess.cpp:73`

本项目状态：

- 缺失成员级规则；
- 仅 extend 子域已有局部 immutable 规则。

### 12.2 `sema_immutable_function_cannot_access_mutable_function`

背景：

- immutable 函数调用 mutable 函数。

最小触发示例：

```cj
struct Counter {
    mut func inc(): Unit {}

    func readOnly(): Unit {
        inc()
    }
}
```

官方 producer：

- `src/Sema/TypeCheckExpr.cpp:206`

本项目状态：

- 缺失。

### 12.3 `sema_inout_must_be_var_variable`

背景：

- `inout` 只能修饰 `var` 变量。

最小触发示例：

```cj
let x = 1
cFunc(inout x)
```

本项目状态：

- `inout` 相关诊断域目前未对齐。

---

## 13. Const-eval / overflow / numeric semantics

### 13.1 `sema_mod_zero`

最小触发示例：

```cj
const let a = 5 % 0
```

本项目状态：

- 当前会和 `/ 0` 一起落到 `CONST_EVAL_DIVIDE_BY_ZERO`；
- 相比官方语义名更粗。

### 13.2 `sema_shift_count_overflow`

最小触发示例：

```cj
const let a = 1 << 999999
```

官方 producer：

- `src/Sema/TypeCheckExpr/AssignExpr.cpp:414`

本项目状态：

- 缺失专门回归。

### 13.3 `sema_negative_shift_count`

最小触发示例：

```cj
const let a = 1 << -1
```

官方 producer：

- `src/Sema/TypeCheckExpr/AssignExpr.cpp:409`

本项目状态：

- 缺失。

---

## 14. Annotation / Java interop / VArray / CFFI

这些仍属于 Sema，只是本项目当前覆盖极薄。

### 14.1 `sema_annotation_no_const_init`

背景：

- `@Annotation` 类缺少 `const` 构造器。

最小触发示例：

```cj
@Annotation
class MyAnno {
    init() {}
}
```

本项目状态：

- 未见同等级规则。

### 14.2 `sema_extend_a_java_type`

背景：

- `@Java` 类型不能被 extend。

最小触发示例：

```cj
@Java
class JavaBox {}

interface Printable {}
extend JavaBox <: Printable {}
```

本项目状态：

- 目前仅通过 `EXTEND_C_TYPE_NOT_ALLOWED` 的 FFI 边界统一处理了一小部分；
- 还没有完整 Java interop 诊断子系统。

### 14.3 `sema_varray_size_match`

背景：

- `VArray` 大小不匹配。

最小触发示例：

```cj
let a: VArray<Int64, 2> = VArray<Int64, 3> { 0 }
```

本项目状态：

- 未见同等级诊断域。

### 14.4 `sema_invalid_cfunc_return_type`

背景：

- `CFunc` 返回类型不满足 `CType`。

最小触发示例：

```cj
let f: CFunc<() -> String>
```

本项目状态：

- CFFI 语义基本未对齐。

---

## 15. Effects / Mock / Common-Specific（CJMP）

这些在官方重构后的 `DiagnosticSema.def` 中已经是正式语义域，本项目当前基本没有对应体系。

### 15.1 `sema_recursive_constructor_call`

已在上文 constructor 子域列出，这里不重复。

### 15.2 effects 代表诊断

- `sema_command_handle_type_error`
- `sema_resumption_handle_type_error`
- `sema_resume_no_with`

最小触发示意：

```cj
handle cmd {
    resume
}
```

本项目状态：

- effects 语义诊断体系基本未见。

### 15.3 mocking 代表诊断

- `sema_mock_disabled`
- `sema_mock_not_in_test_mode`
- `sema_mock_unsupported_type`

最小触发示意：

```cj
createMock<Int64>()
```

本项目状态：

- 未见同等级诊断域。

### 15.4 common/specific 代表诊断

- `sema_not_matched`
- `sema_specific_has_different_type`
- `sema_common_open_class_no_init`

最小触发示意：

```cj
common open class A
specific class A
```

本项目状态：

- 当前仓库没有与官方 `common/specific` 语义诊断相当的实现闭环。

---

## 16. 本项目按语义分析视角的状态归纳

### 16.1 已实现且相对稳定

- imports/redeclaration
- supertype 基础规则
- extend 核心规则
- override 基础规则
- type mismatch / argument mismatch / return mismatch / assignment mismatch
- literal overflow
- divide-by-zero / arithmetic overflow 的一部分 const-eval
- match exhaustiveness
- 继承链上的可见性部分路径

### 16.2 已定义但仍是明显缺口

- `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`
- `NO_CONSTRUCTOR`（基础能力有，语义域没有成体系）

### 16.3 官方已成熟、本项目仍明显缺失

- constructor 全域
- named arguments / call binding
- initialization / legality of usage
- generic upper-bound member resolution
- pattern legality
- immutable/mut 成员级规则
- inout
- annotation 语义
- Java / ObjC / CFFI 语义
- VArray 语义
- effects
- mocking
- common/specific

---

## 17. 结论

如果只看语义分析，那么本项目与官方的真实差距可以概括成：

> 本项目已经具备一条可扩展的 CFIR 语义诊断框架，但当前仍主要覆盖“前端基础类型检查 + 继承/extend + 局部可见性 + 局部 const-eval + 穷尽性”，而官方 C++ Sema 已经把“调用绑定、构造器、初始化、generic-access、pattern legality、mut/immutable、annotation/interop、effects、CJMP”全部建成独立诊断子域。

所以后续如果继续补齐，建议优先顺序是：

1. `call + constructor`
2. `initialization + legality`
3. `generic-access + visibility matrix`
4. `pattern legality`
5. `mut/immutable + inout`
6. `annotation / Java / CFFI / VArray`
7. `effects / mocking / common-specific`

