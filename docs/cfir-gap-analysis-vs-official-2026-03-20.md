# CFIR 阶段与官方 C++ 编译器差距分析

> 日期：2026-03-20
>
> 对比对象：当前 CFIR 实现 × `external/cangjie_compiler`（C++ 参考实现）
>
> 范围：语法构造（AST 节点）+ 语义分析（Resolve/Checker）

---

## 一、总体评估

| 维度 | 完备度 | 说明 |
|------|--------|------|
| IR 树节点定义 | **75%** | 核心声明/表达式/类型/模式已覆盖，缺可选类型、一元表达式等 |
| Resolve 管线 | **80%** | 8 阶段框架完整，调用解析/泛型推导可用 |
| Checker 检查器 | **40%** | 框架完整，具体检查规则覆盖面有限 |
| Desugar 管线 | **0%** | C++ 编译器有 30+ 专项脱糖，当前无独立 desugar 阶段 |
| FFI 子系统 | **0%** | C/Java/ObjC 互操作完全缺失 |
| Effect 系统 | **0%** | perform/resume/try-handle 完全缺失 |

---

## 二、语法构造差距

### 2.1 缺失的表达式节点

| 缺失项 | C++ 节点 | 优先级 | 说明 |
|--------|---------|--------|------|
| 一元表达式 | `UNARY_EXPR` | **P0** | `!`、`-`、`~`、`+`，当前无独立节点 |
| 可选值解包 | `OPTIONAL_EXPR` | **P0** | `expr!` 强制解包 |
| 可选链调用 | `OPTIONAL_CHAIN_EXPR` | **P0** | `obj?.method` |
| 递增递减 | `INC_OR_DEC_EXPR` | P1 | `++`、`--` |
| 类型转换表达式 | `TYPE_CONV_EXPR` | P1 | `Int32(x)` 构造函数式转换 |
| 数组构造 | `ARRAY_EXPR` | P1 | `RawArray<T>()`、`VArray<T, $n>()` |
| let 模式解构 | `LET_PATTERN_DESTRUCTOR` | P1 | `if (let Some(v) <- x)` 条件模式绑定 |
| 尾随闭包 | `TRAIL_CLOSURE_EXPR` | P1 | 末尾参数省略括号的闭包语法 |
| 指针构造 | `POINTER_EXPR` | P2 | C 互操作指针构造 |
| perform 表达式 | `PERFORM_EXPR` | P2 | Effect 系统发起 |
| resume 表达式 | `RESUME_EXPR` | P2 | Effect 系统恢复 |
| @IfAvailable | `IF_AVAILABLE_EXPR` | P3 | 符号可用性检查宏 |

### 2.2 二元操作符覆盖不全

当前 `CfirBinaryOp` 仅支持 4 种逻辑/控制流操作符（`&&`、`||`、`??`、`|>`）。

算术和位操作通过 `CfirFunctionCall` + `CfirBuiltinOperatorResolver` 处理，在 AST 层面不做区分。
C++ 编译器在 `BINARY_EXPR` 中统一支持以下全部操作符：

| 类别 | 操作符 |
|------|--------|
| 算术 | `+`、`-`、`*`、`/`、`%` |
| 位操作 | `&`、`\|`、`^`、`<<`、`>>` |
| 复合赋值 | `+=`、`-=`、`*=`、`/=`、`%=`、`&=`、`\|=`、`^=`、`<<=`、`>>=` |

> 当前设计选择将这些视为函数调用，语义上可行，但与 C++ 参考实现的建模方式不同。

### 2.3 缺失的类型节点

| 缺失项 | C++ 节点 | 优先级 | 说明 |
|--------|---------|--------|------|
| 可选类型 | `OPTION_TYPE` | **P0** | `?T` 语法糖（等价 `Option<T>`） |
| This 类型 | `THIS_TYPE` | P1 | 类体内的 `This` 引用类型 |
| 常量类型 | `CONSTANT_TYPE` | P2 | `$n`（VArray 大小参数） |

### 2.4 缺失的模式节点

| 缺失项 | C++ 节点 | 优先级 | 说明 |
|--------|---------|--------|------|
| 异常类型模式 | `EXCEPT_TYPE_PATTERN` | P1 | `catch(e: E1 \| E2)` 中的多异常模式 |
| Effect 类型模式 | `COMMAND_TYPE_PATTERN` | P2 | Effect handler 中的模式 |

---

## 三、语义分析差距

### 3.1 Resolve 阶段对比

| 能力 | C++ 编译器 | CFIR 实现 | 状态 |
|------|----------|----------|------|
| Import 解析（含 star、别名、冲突检测） | ✅ | ✅ | 已对齐 |
| 超类型解析（多继承、菱形、循环检测） | ✅ | ✅ | 已对齐 |
| 类型引用解析（38 种类型） | ✅ | ⚠️ 8 种 TypeRef | 缺 `?T`、`This`、`$n` |
| 声明状态/修饰符（含 FFI 修饰符） | ✅ | ⚠️ 基础修饰符 | 缺 FFI 相关修饰符 |
| 扩展解析（孤儿规则、特化冲突） | ✅ | ✅ | 已对齐 |
| 隐式类型推断 | ✅ | ✅ | 已对齐 |
| 表达式 Body resolve | ✅ | ✅ | 已对齐 |
| 调用解析和重载解决（C++ 136KB） | ✅ | ⚠️ 基本实现 | 覆盖面待评估 |
| 泛型约束求解 | ✅ | ✅ | 已对齐 |
| **Desugar（30+ 专项脱糖）** | ✅ | ❌ | 整体缺失 |
| **Effect 系统（perform/resume/try-handle）** | ✅ | ❌ | 整体缺失 |
| **FFI 检查（C/Java/ObjC）** | ✅ | ❌ | 整体缺失 |

### 3.2 Checker 阶段对比

| 检查项 | C++ 编译器 | CFIR 实现 | 状态 |
|--------|----------|----------|------|
| 类型匹配（赋值、参数、返回值） | ✅ | ✅ | 已实现 |
| Match 完整性（Maranget 算法） | ✅ | ✅ | 已实现 |
| Override 合法性 | ✅ | ✅ | 已实现 |
| 扩展声明检查（7 个检查器） | ✅ | ✅ | 已实现 |
| 数字字面量溢出 | ✅ | ✅ | 已实现 |
| 常量求值除零/溢出 | ✅ | ✅ | 已实现 |
| 访问控制（4 级可见性） | ✅ | ⚠️ 部分 | 有诊断定义，检查不完整 |
| 继承协变/逆变检查 | ✅ | ⚠️ 部分 | 缺协变返回、参数逆变的完整检查 |
| **模式冗余性（不可达分支）** | ✅ | ❌ | 只有完整性，缺冗余性检测 |
| **常量表达式完整求值** | ✅ CalcConstExpr | ❌ | 只有溢出检查，缺编译时求值 |
| **初始化定值分析** | ✅ | ❌ | 变量使用前是否初始化 |
| **未使用 import 检查** | ✅ | ❌ | — |
| **内联函数检查** | ✅ | ❌ | — |
| **递归类型消除** | ✅ | ❌ | — |
| **操作符重载合法性** | ✅ | ❌ | 自定义操作符的语义约束 |

---

## 四、整体缺失子系统

按优先级分层：

### P0 — 核心语义正确性

| 子系统 | 规模 | 说明 |
|--------|------|------|
| **可选类型 `?T`** | 中 | 类型节点 + 脱糖到 `Option<T>` + `?.` 链 + `??` 语义 + `!` 解包 |
| **一元表达式** | 小 | AST 节点 + resolve + 操作符重载支持 |
| **Desugar 管线** | 大 | 至少需覆盖：`for-in`→迭代器调用、`??`→match、`?.`→if-let、字符串插值→concat、范围→Range 构造 |

### P1 — 功能完整性

| 子系统 | 规模 | 说明 |
|--------|------|------|
| **This 类型** | 小 | 类型节点 + 类体内 resolve |
| **尾随闭包** | 小 | 语法识别 + 调用解析适配 |
| **let 模式绑定** | 中 | `if let`/`while let` 条件中的模式解构 |
| **递增递减 `++`/`--`** | 小 | AST 节点 + 操作符 resolve |
| **初始化定值分析** | 中 | 数据流分析，确保变量使用前已初始化 |
| **模式冗余性检查** | 小 | 在现有 Maranget 基础上增加不可达分支检测 |
| **未使用 import 检查** | 小 | 遍历使用记录，报告未引用的 import |

### P2 — 高级特性

| 子系统 | 规模 | 说明 |
|--------|------|------|
| **Effect 系统** | 大 | `perform`/`resume`/`try-handle` 完整语义，含类型检查和 continuation |
| **FFI 子系统** | 大 | C/Java/ObjC 互操作检查，foreign 函数声明和类型映射 |
| **宏展开引擎** | 大 | 当前只有 AST 节点定义，缺实际的宏求值和展开逻辑 |
| **常量表达式完整求值** | 中 | 编译时常量折叠和计算 |

---

## 五、C++ 编译器 Desugar 清单（当前全部缺失）

C++ 编译器在 `src/Sema/DesugarAfterTypeCheck/` 中实现了以下专项脱糖：

| 脱糖项 | 源文件 | 说明 |
|--------|--------|------|
| as 表达式 | `AsExpr.cpp` | 类型转换展开 |
| 二元表达式 | `BinaryExpr.cpp` | `\|\|`、`&&`、位操作展开为方法调用 |
| 函数调用 | `CallExpr.cpp` | 重载解决后的调用重写 |
| `??` 操作符 | `Coalescing.cpp` | 展开为 match 表达式 |
| for-in 循环 | `ForInExpr.cpp` | 展开为迭代器 while 循环 |
| if 表达式 | `IfExpr.cpp` | if-let 等语法糖 |
| is 表达式 | `IsExpr.cpp` | 类型检查展开 |
| 范围表达式 | `RangeExpr.cpp` | 展开为 Range 构造调用 |
| 字符串插值 | `StrInterpolationExpr.cpp` | 展开为 toString + concat |
| try 表达式 | `TryExpr.cpp` | try-with-resources 展开 |
| spawn 表达式 | `SpawnExpr.cpp` | 并发任务包装 |
| Effect handlers | `EffectHandlers.cpp` | perform/resume 展开 |
| 默认参数 | `FuncParam.cpp` | 默认值填充 |
| 包级脱糖 | `Package.cpp` | 包级别的声明重写 |

---

## 六、数据来源

- CFIR 树定义：`cfir/cfir-tree/tree-generator/src/.../CfirTree.kt`（87 个节点定义）
- C++ AST 定义：`external/cangjie_compiler/include/cangjie/AST/ASTKind.inc`（122 种节点）
- C++ 类型系统：`external/cangjie_compiler/include/cangjie/AST/Types.h`（38 种类型）
- C++ 语义分析：`external/cangjie_compiler/src/Sema/`（50+ 个 .cpp 文件）
- CFIR Resolve：`cfir/resolve/src/`（85 个源文件，8 阶段处理器）
- CFIR Checker：`cfir/checkers/src/`（59 个源文件，18 个注册检查器）