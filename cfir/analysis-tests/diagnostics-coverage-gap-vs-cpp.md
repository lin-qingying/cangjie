# CFIR Diagnostics 对照官方 C++ 语义的测试覆盖缺口

## 结论摘要

当前 `cfir/analysis-tests/testData/diagnostics` 一共约 115 个 `.cj` 用例，覆盖重点明显偏向：

- `operator/*`
- `coverage/extensions/*`
- `type-mismatch/*`
- `coverage/imports/*`
- `coverage/supertypes/*`
- `coverage/match/*`（目前基本只到穷尽性）

和 `external/cangjie_compiler` 的 C++ 语义实现相比，当前缺的不是零散一个两个诊断名，而是几整块语义域：

1. 泛型类型实参缺失、泛型上界成员解析失败
2. 可见性/访问控制的负例覆盖
3. 非 `open` 继承、override 返回类型负例
4. `super/this` 在 struct/enum/constructor 中的非法使用
5. 构造器规则
6. 初始化/先用后初始化
7. 命名参数与调用歧义
8. match/pattern 的错误语义
9. immutable/mut 语义
10. 常量求值的边界错误（尤其 shift/mod）

下面按“本项目已有诊断但没测到”和“官方已有语义但本项目还没形成稳定 coverage”两层来列。

---

## 对照方法

本次对照主要基于三类证据：

1. 当前测试面：`cfir/analysis-tests/testData/diagnostics/**/*`
2. 本项目诊断定义与 producer：
   - `cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrors.kt`
   - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/**/*`
   - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`
3. 官方 C++ 语义基线：
   - `external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def`
   - `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def`
   - `external/cangjie_compiler/src/Sema/**/*`

注意：本项目和官方实现的诊断名不完全一致，下面统一按“语义等价”对齐，而不是按名字生硬一一对应。

---

## A. 已有 CFIR 诊断定义，但当前 analysis-tests 没把语义测透

这一组最值得优先补，因为它们说明“项目侧已经承认该语义”，但 `analysis-tests` 还没有把回归保护建起来。

### 1. 泛型类型缺失实参

- 官方语义：
  - `sema_generic_type_without_type_argument`
  - 定义位置：`external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def:137`
  - 触发路径示例：
    - `external/cangjie_compiler/src/Sema/TypeCheckType.cpp:241`
    - `external/cangjie_compiler/src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:45`
    - `external/cangjie_compiler/src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:624`
- 本项目对应语义：
  - `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`
  - 定义位置：`cfir/checkers/gen/.../CfirErrors.kt:104`
- 当前状态：
  - 诊断名已定义，但 `cfir/analysis-tests/testData/diagnostics` 中没有任何 inline 断言命中它。
  - 这不是小缺口，而是“泛型名在类型位置 / 表达式位置 / 成员访问位置缺实参”整个负例面都没建起来。

建议新增最小语义组：

```cj
// 建议文件：type-mismatch/genericTypeWithoutArgumentsRich.cj
class Box<T> {
    let value: T
}

func takeBox(v: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>): Unit {}

func useTypePosition(): Unit {
    let x: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>
}

func useQualifier(): Unit {
    let _ = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>.value
}
```

说明：这组用例要覆盖三种位置，不要只测变量声明一种，因为官方 C++ 在 type check 和 name reference 两边都会报。

### 2. override 返回类型负例

- 官方语义：
  - `sema_return_type_invariance`
  - 定义位置：`external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def:239`
  - producer：`external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1394`
- 本项目对应语义：
  - `OVERRIDING_RETURN_TYPE_MISMATCH`
  - producer：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt:132`
- 当前状态：
  - 只有正例：`cfir/analysis-tests/testData/diagnostics/coverage/inheritance/overrideReturnType.cj`
  - 没有任何负例断言。

建议新增：

```cj
// 建议文件：coverage/inheritance/overrideReturnTypeMismatchRich.cj
open class Base {}
class Derived <: Base {}

open class Parent {
    open func value(): Derived {
        return Derived()
    }
}

class Child <: Parent {
    <!OVERRIDING_RETURN_TYPE_MISMATCH!>override func value(): Base<!> {
        return Base()
    }
}
```

说明：现在的 suite 只证明“协变成功”，没有证明“不协变时会稳定失败”。

### 3. struct / enum 中非法 `super`

- 官方语义：
  - `sema_use_super_in_interface`
  - `sema_super_use_error_inside_non_class`
  - 定义位置：
    - `external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def:199`
    - `external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def:200`
- 本项目对应语义：
  - `STRUCT_SUPER_NOT_ALLOWED`
  - `ENUM_SUPER_NOT_ALLOWED`
  - producer：
    - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirIllegalSuperReferenceChecker.kt:33`
    - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirIllegalSuperReferenceChecker.kt:38`
- 当前状态：
  - 现在只有 `super/repeated_inheritance.cj` 覆盖了 `INTERFACE_SUPER_NOT_ALLOWED`
  - struct / enum 两条 producer 都没有真正 inline 断言。

建议新增：

```cj
// 建议文件：super/illegalSuperInStructAndEnum.cj
struct S {
    func f(): Unit {
        <!STRUCT_SUPER_NOT_ALLOWED!>super<!>.toString()
    }
}

enum E {
    | A

    func f(): Unit {
        <!ENUM_SUPER_NOT_ALLOWED!>super<!>.toString()
    }
}
```

说明：这里不能再复用 interface 场景，因为 producer 是分支独立实现。

### 4. `extend` 到 C/Java 边界类型

- 官方语义：
  - `sema_c_type_cannot_extend_interface`
  - `sema_extend_a_java_type`
  - 定义位置：
    - `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:156`
    - `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:213`
- 本项目对应语义：
  - `EXTEND_C_TYPE_NOT_ALLOWED`
  - producer：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendCheckers.kt:50`
- 当前状态：
  - 已有占位文件：`cfir/analysis-tests/testData/diagnostics/coverage/extensions/extendCTypeNotAllowed.cj`
  - 但文件里明确写的是 TODO，实际没有 inline 诊断断言。

建议把占位文件升级为真覆盖：

```cj
// 建议直接补现有文件：coverage/extensions/extendCTypeNotAllowed.cj
@C
class NativeBox {}

interface Printable {}

extend <!EXTEND_C_TYPE_NOT_ALLOWED!>NativeBox<!> <: Printable {}
```

说明：这是最明确的“文件已经在，但 coverage 还没真的落地”。

### 5. 已声明但尚未形成 producer 的语义型诊断

这一组要特别标出来，因为它们不是“单纯没写测试”，而是“定义已存在，但在 `cfir/checkers` 下看不到真正 reporter 使用点”，因此后续应先补语义 producer，再补 analysis-tests。

涉及的典型诊断：

- `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`
- `INVISIBLE_MEMBER`
- `INVISIBLE_REFERENCE`
- `CANNOT_OVERRIDE_INVISIBLE_MEMBER`
- `CLASS_NOT_OPEN_FOR_INHERITANCE`

其中：

- `OVERRIDING_RETURN_TYPE_MISMATCH`、`STRUCT_SUPER_NOT_ALLOWED`、`ENUM_SUPER_NOT_ALLOWED` 已经有 producer，只是没测透。
- 上面这五个更偏“诊断名已注册，但当前 `cfir/checkers/src` 中还没有对等 producer”。

这意味着：

1. 这些点依然应该进入 coverage 清单
2. 但它们不能只靠补 `.cj` 文件解决，必须连 producer 一起补

---

## B. 官方 C++ 已有稳定语义，但当前 CFIR analysis-tests 仍是块级空白

这一组不一定已经有同名 CFIR 诊断；有些甚至说明本项目前端诊断实现还没长到这个阶段。但从“对齐官方语义”的角度，这些测试迟早都要补。

### 6. 构造器规则整块缺失

官方语义至少包括：

- `sema_no_match_constructor`：`DiagnosticSema.def:88`
- `sema_ambiguous_constructor_match`：`DiagnosticSema.def:91`
- `sema_recursive_constructor_call`：`DiagRefactor/DiagnosticSema.def:46`
- `sema_no_non_param_constructor_in_super_class`：`DiagRefactor/DiagnosticSema.def:135`
- `sema_invalid_this_call_outside_ctor`：`DiagnosticSema.def:208`
- `sema_illegal_place_of_calling_this_or_super`：`DiagnosticSema.def:217`

当前 suite 的实际情况：

- `enum/errorSimpleEnum.cj` 只零散打到 `NO_CONSTRUCTOR`
- 没有把“构造器调用规则”当作一个语义域来覆盖

建议最少拆成三份：

```cj
// constructor/noMatchingConstructorRich.cj
class Box {
    init(v: Int64) {}
}

func f(): Unit {
    let _ = <!NO_CONSTRUCTOR 或官方对应语义诊断!>Box()<!>
}
```

```cj
// constructor/superCallPlacementRich.cj
open class Base {
    init(v: Int64) {}
}

class Child <: Base {
    init() {
        let x = 1
        <!官方 sema_illegal_place_of_calling_this_or_super 对应语义!>super(1)<!>
    }
}
```

```cj
// constructor/recursiveThisCallRich.cj
class Loop {
    init() {
        <!官方 sema_recursive_constructor_call 对应语义!>this()<!>
    }
}
```

说明：构造器是官方诊断大户，但当前 `analysis-tests` 里没有形成单独目录。

### 7. 初始化与先用后初始化

官方语义：

- `sema_used_before_initialization`：`DiagnosticSema.def:24`
- `sema_class_uninitialized_field`：`DiagnosticSema.def:186`

当前 suite：

- 没有 `initialization/*`
- 没有“字段未初始化”“局部先用后赋值”“构造阶段成员访问早于初始化完成”等用例面

建议：

```cj
// initialization/usedBeforeInitializationRich.cj
func f(): Int64 {
    let v: Int64
    return <!官方 sema_used_before_initialization 对应语义!>v<!>
}
```

```cj
// initialization/classFieldNotInitializedRich.cj
class Holder {
    let value: Int64

    init() {
    }
}
```

说明：这是语义正确性的硬约束，不是锦上添花。

### 8. 命名参数与调用歧义

官方语义：

- `sema_unknown_named_argument`：`DiagnosticSema.def:100`
- `sema_multiple_named_argument`：`DiagnosticSema.def:101`
- `sema_invalid_named_arguments`：`DiagnosticSema.def:102`
- `sema_unsupport_named_argument`：`DiagnosticSema.def:103`
- `sema_unordered_arguments`：`DiagRefactor/DiagnosticSema.def:33`
- `sema_param_named_mismatched`：`DiagRefactor/DiagnosticSema.def:34`
- 以及函数/构造器歧义：`sema_ambiguous_match`、`sema_ambiguous_constructor_match`

当前 suite：

- 主要还是 `ARGUMENT_TYPE_MISMATCH`
- 几乎没有“参数绑定层”的负例

建议：

```cj
// call/namedArgumentsRich.cj
func foo(a: Int64, b: Int64): Unit {}

func test(): Unit {
    <!官方 sema_unknown_named_argument 对应语义!>foo(c: 1, b: 2)<!>
    <!官方 sema_multiple_named_argument 对应语义!>foo(a: 1, a: 2)<!>
    <!官方 sema_unordered_arguments 对应语义!>foo(a: 1, 2)<!>
}
```

说明：这一块如果不补，后续 call resolver 调整很容易回归到“仍有 TYPE_MISMATCH，但参数绑定已经错了”的状态。

### 9. 泛型上界成员访问失败

官方语义：

- `sema_generic_no_member_match_in_upper_bounds`
- `sema_generic_no_method_match_in_upper_bounds`
- producer：
  - `external/cangjie_compiler/src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:772`
  - `external/cangjie_compiler/src/Sema/TypeCheckCall.cpp:2594`

当前 suite：

- 只有正例：`type-mismatch/whereUpperBoundMemberAccess.cj`
- 没有任何负例验证“上界里找不到成员/方法时如何报”

建议：

```cj
// type-mismatch/whereUpperBoundMemberAccessNegative.cj
interface Named {
    func name(): String
}

func badField<T>(value: T): Int64 where T <: Named {
    return <!官方 generic_no_member_match_in_upper_bounds 对应语义!>value.id<!>
}

func badCall<T>(value: T): Unit where T <: Named {
    <!官方 generic_no_method_match_in_upper_bounds 对应语义!>value.reset()<!>
}
```

说明：这和普通 `UNRESOLVED_REFERENCE` 不是一回事；官方是把它当成“约束环境内的成员解析失败”单独建模的。

### 10. match / pattern 错误语义仍然太薄

官方语义：

- `sema_tuple_pattern_not_match`：`DiagnosticSema.def:63`
- `sema_pattern_not_match`：`DiagnosticSema.def:105`
- `sema_not_overload_in_match`：`DiagnosticSema.def:106`
- `sema_enum_pattern_param_size_error`：`DiagnosticSema.def:112`
- `sema_match_case_has_no_type`：`DiagnosticSema.def:116`

当前 suite：

- 只有 `coverage/match/nonExhaustiveMatchRich.cj`
- 以及 `coverage/match/booleanExhaustiveness.cj`

建议：

```cj
// match/patternErrorsRich.cj
enum Option<T> {
    | Some(T)
    | None
}

func test(v: Option<Int64>): Int64 {
    match (v) {
        case Some() => 1 // 官方：enum pattern 参数个数错误
        case (a, b) => 2 // 官方：tuple pattern not match
        case 1 => 3      // 官方：pattern not match
    }
}
```

说明：当前 coverage 只测“是不是穷尽”，没有测“pattern 自身是否合法”。

### 11. immutable / mut 语义缺口

官方语义：

- `sema_cannot_modify_var`：`DiagnosticSema.def:235`
- `sema_incompatible_mut_modifier_between_struct_and_interface`：`DiagnosticSema.def:273`
- `sema_immutable_function_cannot_access_mutable_function`：`DiagnosticSema.def:275`

当前 suite：

- 已覆盖的是 extend 体系下的 `extendImmutableMutInterface.cj`、`extendImmutableMemberRestriction.cj`
- 没覆盖对象方法/属性层面的 immutable 规则

建议：

```cj
// mut/immutableFunctionRulesRich.cj
struct Counter {
    var value: Int64 = 0

    mut func inc(): Unit {
        value = value + 1
    }

    func readOnly(): Unit {
        <!官方 sema_immutable_function_cannot_access_mutable_function 对应语义!>inc()<!>
        <!官方 sema_cannot_modify_var 对应语义!>value = 1<!>
    }
}
```

说明：extend 语义和成员函数语义不是一层规则，不能互相替代。

### 12. 常量求值边界：`mod` / shift 计数

官方语义：

- `sema_mod_zero`：`DiagnosticSema.def:43`
- `sema_shift_count_overflow`：`DiagnosticSema.def:45`
- `sema_negative_shift_count`：`DiagnosticSema.def:46`

当前 suite：

- 只有：
  - `const-eval/constEvalDivideByZero.cj`
  - `const-eval/constEvalArithmeticOverflow.cj`
- `mod by zero`、`负 shift`、`过大 shift` 都没有

建议：

```cj
// const-eval/constEvalShiftAndModRich.cj
const let a = <!官方 sema_mod_zero 对应语义!>5 % 0<!>
const let b = <!官方 sema_negative_shift_count 对应语义!>1 << -1<!>
const let c = <!官方 sema_shift_count_overflow 对应语义!>1 << 999999<!>
```

说明：这类边界值最容易在常量折叠和解释器实现重构时掉回归。

---

## 优先级建议

如果按“最小成本、最大收益”的顺序补，我建议是：

1. 先补已有 producer 但没测试的：
   - `OVERRIDING_RETURN_TYPE_MISMATCH`
   - `STRUCT_SUPER_NOT_ALLOWED`
   - `ENUM_SUPER_NOT_ALLOWED`
   - `EXTEND_C_TYPE_NOT_ALLOWED`
2. 再补已有诊断定义但 producer 未落地的：
   - `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`
   - `INVISIBLE_MEMBER`
   - `INVISIBLE_REFERENCE`
   - `CLASS_NOT_OPEN_FOR_INHERITANCE`
   - `CANNOT_OVERRIDE_INVISIBLE_MEMBER`
3. 最后补官方语义整块空白：
   - constructor
   - initialization
   - named arguments / ambiguity
   - pattern errors
   - immutable rules
   - const-eval 边界

---

## 最终判断

从“对齐官方 C++ 语义”的角度看，当前 `cfir/analysis-tests/testData/diagnostics` 已经不再是“没有测试”，而是“覆盖重心偏在基础算子、extend 规则、基础 type mismatch 上”；真正缺的是：

- 诊断层级更高的 call/constructor 语义
- 初始化与对象生命周期语义
- 模式匹配错误语义
- 泛型约束环境下的成员解析语义
- 可见性与 immutable 的负例语义

如果后续要系统补齐，不建议继续零散加单文件；更合理的做法是按语义域新建目录：

- `constructor/`
- `initialization/`
- `call/`
- `match/`
- `mut/`
- `generic-access/`

这样才能把当前“覆盖有点多，但面仍然不均匀”的问题真正纠正掉。
