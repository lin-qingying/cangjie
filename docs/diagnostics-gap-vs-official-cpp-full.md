# 仓颉官方 C++ 编译器 vs 本项目 全场景诊断差距总览

## 1. 文档目的

本文档用于从“全场景、全代码域”的角度，对比：

1. 官方仓颉 C++ 编译器已经定义并实现的诊断域；
2. 本项目当前已经具备的诊断定义、producer、映射与测试覆盖；
3. 本项目相对官方仍存在的诊断定义缺口、producer 缺口、映射缺口、渲染缺口与回归覆盖缺口。

这份文档是全局总览。

- 继承 / extend / super 的已落地细节，可继续参考：
  `cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md`
- 本文档的目标不是只列几个未实现的诊断名，而是按“诊断域 + 实现链路 + 测试覆盖”来给出全景差距。

---

## 2. 对照范围与方法

### 2.1 官方侧源码依据

官方诊断定义入口：

- `external/cangjie_compiler/include/cangjie/Basic/DiagnosticsAll.def`
- `external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def`
- `external/cangjie_compiler/include/cangjie/Basic/DiagnosticCHIR.def`
- `external/cangjie_compiler/include/cangjie/Basic/DiagnosticMacroExpand.def`
- `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticAll.def`
- `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/*.def`

官方 producer 主体分布：

- `external/cangjie_compiler/src/Sema/TypeCheck*.cpp`
- `external/cangjie_compiler/src/Sema/TypeCheckExpr/*.cpp`
- `external/cangjie_compiler/src/Sema/InheritanceChecker/*.cpp`
- `external/cangjie_compiler/src/Sema/LegalityOfUsage/*.cpp`
- `external/cangjie_compiler/src/Sema/NativeFFI/**/*.cpp`
- `external/cangjie_compiler/src/Sema/Desugar/**/*.cpp`
- `external/cangjie_compiler/src/Sema/CalcConstExpr.cpp`

### 2.2 本项目源码依据

本项目当前可见的诊断入口主要分为三层：

1. 解析 / 语法层
   - `psi/resources/messages/CangJieParsingBundle.properties`
2. CFIR checker / cone 映射层
   - `cfir/checkers/checkers-component-generator/src/.../CfirDiagnosticsList.kt`
   - `cfir/checkers/src/.../analysis/checkers/**/*`
   - `cfir/checkers/src/.../analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`
   - `cfir/checkers/src/.../analysis/collectors/components/ErrorNodeDiagnosticCollectorComponent.kt`
3. 回归测试层
   - `cfir/analysis-tests/testData/diagnostics/**/*`

### 2.3 数量级观察

官方诊断定义文件条目数（按 `ERROR/WARNING/NOTE` 粗略计数）：

| 官方定义文件 | 条目数 |
|---|---:|
| `DiagnosticSema.def` | 234 |
| `DiagnosticCHIR.def` | 16 |
| `DiagnosticMacroExpand.def` | 42 |
| `DiagRefactor/DiagnosticSema.def` | 289 |
| `DiagRefactor/DiagnosticParser.def` | 235 |
| `DiagRefactor/DiagnosticLexer.def` | 39 |
| `DiagRefactor/DiagnosticDriver.def` | 63 |
| `DiagRefactor/DiagnosticPackage.def` | 25 |
| `DiagRefactor/DiagnosticModule.def` | 11 |
| `DiagRefactor/DiagnosticConditionalCompilation.def` | 11 |
| `DiagRefactor/DiagnosticParserQuery.def` | 17 |
| `DiagRefactor/DiagnosticFrontend.def` | 5 |
| `DiagRefactor/DiagnosticIncrementalCompilation.def` | 3 |
| `DiagRefactor/DiagnosticChir.def` | 32 |

本项目当前数量级：

- `psi/resources/messages/CangJieParsingBundle.properties` 约 96 条解析消息键；
- `CfirDiagnosticsList.kt` 当前约 71 个 CFIR 诊断工厂；
- `cfir/analysis-tests/testData/diagnostics` 当前约 120 个 `.cj` 诊断用例。

结论很直接：

- 官方诊断版图已经覆盖“词法 + 语法 + 前端环境 + 模块/包 + 语义 + 宏展开 + CHIR + 条件编译 + 增量编译 + FFI”；
- 本项目当前真正成体系的部分，仍以“CFIR 前端语义子集 + 解析消息 + 部分回归样例”为主；
- 差距不是零散诊断名，而是整块诊断域的缺席或薄实现。

---

## 3. 本项目当前诊断体系现状

### 3.1 已经比较成型的部分

#### A. CFIR 语义检查主链

当前已有较明确的 checker / 映射 / 测试闭环，主要集中在：

- import / redeclaration
- supertype 基础规则
- extend 规则
- modifier / declaration-status
- override 基础规则
- type mismatch 基础规则
- const-eval 的一小部分
- operator 调用失败与基础 unresolved
- match exhaustiveness

代表性入口：

- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt`
- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirSupertypesChecker.kt`
- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendCheckers.kt`
- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt`
- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirArgumentTypeMismatchChecker.kt`
- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstEvalArithmeticChecker.kt`
- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMatchExhaustivenessChecker.kt`
- `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`

#### B. 解析消息层

项目已经有一套自己的解析消息资源：

- `psi/resources/messages/CangJieParsingBundle.properties`

它能支撑 IDE / PSI 恢复场景下的语法错误提示，但它与官方 `DiagnosticParser.def` / `DiagnosticLexer.def` 不是一套一一对齐的建模系统。

#### C. 继承 / extend / super 一批刚补齐

本轮已补齐或接通的诊断链路：

- `OVERRIDING_RETURN_TYPE_MISMATCH`
- `STRUCT_SUPER_NOT_ALLOWED`
- `ENUM_SUPER_NOT_ALLOWED`
- `EXTEND_C_TYPE_NOT_ALLOWED`
- `INVISIBLE_MEMBER`
- `INVISIBLE_REFERENCE`
- `CANNOT_OVERRIDE_INVISIBLE_MEMBER`
- `CLASS_NOT_OPEN_FOR_INHERITANCE`

这说明本项目已经有能力按“checker 层 + cone 映射层 + renderer + tests”方式补完整个语义域。

---

## 4. 全场景差距矩阵

下面按官方诊断域给出全景判断。

### 4.1 Lexer 诊断

官方入口：

- `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticLexer.def`

官方能力特征：

- 非法 token 起始
- 数字字面量基数与后缀
- 插值/字符串终止
- raw string / unicode escape
- block comment / backquote / rune literal

本项目现状：

- 有解析消息资源，但没有看到与官方 `lex_*` 一一对齐的统一诊断定义层；
- 也没有独立的 `lexer` 诊断测试目录与覆盖矩阵；
- 当前更像“解析器 / PSI 恢复报错”，而不是“官方 lexer 诊断域对齐实现”。

判断：

- 状态：`部分实现，系统性缺口明显`
- 主要不足：
  - 缺少官方词法诊断定义映射表
  - 缺少逐条 producer 对照
  - 缺少专门 lexer regression suite

### 4.2 Parser 诊断

官方入口：

- `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticParser.def`
- `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticParserQuery.def`

本项目现状：

- `CangJieParsingBundle.properties` 已有较多语法消息；
- 但这套消息是“本地资源键”模型，不是“官方 parser diag id + note + highlight”模型；
- 缺少一份“官方 parser 诊断 -> 本项目 parser message / PSI 入口”的完整映射。

判断：

- 状态：`部分实现`
- 主要不足：
  - 解析消息存在，但不是官方诊断定义对齐实现
  - 缺少 parser query 诊断分层
  - 缺少 parser/lexer 与 CFIR 诊断的边界文档

### 4.3 Frontend / Driver / Module / Package / ConditionalCompilation / IncrementalCompilation

官方入口：

- `DiagRefactor/DiagnosticFrontend.def`
- `DiagRefactor/DiagnosticDriver.def`
- `DiagRefactor/DiagnosticModule.def`
- `DiagRefactor/DiagnosticPackage.def`
- `DiagRefactor/DiagnosticConditionalCompilation.def`
- `DiagRefactor/DiagnosticIncrementalCompilation.def`

本项目现状：

- 仓库里存在 module / package / session / CLI / incremental 相关实现；
- 但没有看到与这些官方诊断域对齐的统一诊断定义文件、渲染层和系统测试目录；
- 当前诊断主战场仍在 CFIR checker，而不是 frontend/driver/module/package 层。

判断：

- 状态：`大面积缺席`
- 主要不足：
  - 基本没有形成同等级的诊断定义体系
  - 缺少模块依赖、包装配、驱动参数、条件编译、增量编译的回归样例
  - 缺少“非 CFIR 语义诊断”的统一出入口

### 4.4 Sema 核心：声明 / 继承 / extend / imports / redeclaration

官方入口：

- `DiagnosticSema.def`
- `DiagRefactor/DiagnosticSema.def`
- `src/Sema/InheritanceChecker/*`
- `src/Sema/TypeCheckExtend.cpp`
- `src/Sema/TypeCheckDecl.cpp`
- `src/Sema/CheckUnusedImportImpl.cpp`

本项目现状：

- 这是当前最接近官方的区域之一；
- import / redeclaration / supertype / extend / override / declaration-status 都已有一定 producer；
- 但仍远未覆盖官方 sema 的全部声明类诊断。

已较明确覆盖：

- import 冲突 / alias 冲突 / import target not found
- redeclaration / conflicting overloads
- self super / duplicate super / multiple class supers
- interface cannot inherit class
- extend orphan / duplicate / not-interface / immutable / specialization / default impl conflict
- override static / redef instance / nothing to override
- overriding return type mismatch
- cannot override invisible member
- class not open for inheritance

仍明显不足：

- generic type without type arguments
- 大量访问控制细分
- 构造器规则
- 初始化合法性
- FFI 继承 / 注解 / native 互操作细节

判断：

- 状态：`部分实现，已具备扩展框架`

### 4.5 Sema 核心：构造器 / 调用绑定 / named arguments / ambiguity

官方入口：

- `DiagnosticSema.def` 中 `no_match_constructor / ambiguous_constructor_match / unknown_named_argument / multiple_named_argument` 等
- `src/Sema/TypeCheckCall.cpp`
- `src/Sema/TypeCheckExpr/NameReferenceExpr.cpp`

本项目现状：

- 有 `NO_CONSTRUCTOR`、`UNRESOLVED_REFERENCE`、`ARGUMENT_TYPE_MISMATCH`、`NO_MATCHING_OPERATOR_INVOKE` 等基础能力；
- 但“调用绑定层”的错误粒度仍然偏粗；
- 目前大量错误仍收敛成 `UNRESOLVED_REFERENCE` 或通用 inapplicable，而不是官方那种参数绑定级诊断。

明显不足：

- `constructor/` 目录整体缺失
- `call/` 目录整体缺失
- named arguments / argument ordering / ambiguous constructor 还没有成体系落地

判断：

- 状态：`弱实现`

### 4.6 Initialization / LegalityOfUsage

官方入口：

- `src/Sema/LegalityOfUsage/InitializationChecker.cpp`
- `DiagnosticSema.def` 中 used-before-init / class-uninitialized-field 等

本项目现状：

- 当前 `diagnostics` 测试目录中没有 `initialization/` 域；
- checker 侧也没有形成对应的专门诊断域；
- 这是官方有明确规则但本项目几乎没建模的一大块。

判断：

- 状态：`基本缺席`

### 4.7 Generic 语义

官方入口：

- `src/Sema/TypeCheckGeneric.cpp`
- `src/Sema/TypeCheckType.cpp`
- `src/Sema/TypeCheckExpr/NameReferenceExpr.cpp`
- `src/Sema/TypeArgumentInference.cpp`

本项目现状：

- 已有一部分约束类诊断：
  - `ONLY_ONE_CLASS_BOUND_ALLOWED`
  - `REPEATED_BOUND`
  - `CONFLICTING_UPPER_BOUNDS`
  - 一批 inference contradiction / mismatch
- 但 generic 诊断仍偏“类型系统基础错误”，没有形成官方那种“泛型命名语义 + 上界解析语义 + type argument 使用语义”的全链路。

明确缺口：

- `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT` 仍缺 producer 与测试
- upper-bound member access negative 仍缺
- 约束环境中的成员解析失败仍常退化成普通 unresolved / mismatch

判断：

- 状态：`部分实现，但泛型语义面明显不足`

### 4.8 Access Control / Visibility

官方入口：

- `src/Sema/TypeCheckAccess.cpp`
- `DiagnosticSema.def` 中 access function / field / package internal obtain 等

本项目现状：

- 本轮刚接通一部分继承与成员访问的可见性错误：
  - `INVISIBLE_MEMBER`
  - `INVISIBLE_REFERENCE`
  - `CANNOT_OVERRIDE_INVISIBLE_MEMBER`
- 但目前仍是“继承链 / 调用解析局部接通”，不是完整 access-control 子系统。

明显不足：

- package/internal/protected/private 的全矩阵覆盖仍不存在
- 类型可见性和引用可见性的边界仍未完整建模
- 访问控制的跨模块 / 跨包 / 导入态差异仍缺系统文档与 tests

判断：

- 状态：`部分实现`

### 4.9 Match / Pattern

官方入口：

- `src/Sema/TypeCheckMatchExpr.cpp`
- `src/Sema/TypeCheckPattern.cpp`
- `src/Sema/PatternUsefulness.cpp`

本项目现状：

- `coverage/match/` 目前主要覆盖 exhaustiveness；
- `CfirMatchExhaustivenessChecker` 很明确；
- 但 pattern legality 自身的诊断基本没展开。

明显不足：

- tuple pattern mismatch
- enum pattern parameter count
- pattern not match
- match case has no type
- overload resolution in match case

判断：

- 状态：`穷尽性已实现，pattern 语义薄弱`

### 4.10 Immutable / mut

官方入口：

- `DiagnosticSema.def` 中 mut/immutable 相关错误
- `src/Sema/LegalityOfUsage/*`
- 以及成员调用路径中的 mut 约束

本项目现状：

- extend 方向已有一批实现：
  - `EXTEND_IMMUTABLE_MUT_INTERFACE`
  - `EXTEND_IMMUTABLE_MUT_PROPERTY`
  - `EXTEND_IMMUTABLE_INDEX_ASSIGNMENT`
- 但对象成员级、函数体级 immutable 规则尚未建立。

判断：

- 状态：`extend 子域有实现，语言整体语义未覆盖`

### 4.11 Const-eval / overflow

官方入口：

- `src/Sema/CalcConstExpr.cpp`
- `src/Sema/ConstEvaluationChecker.cpp`
- `src/Sema/TypeCheckOverflow.cpp`

本项目现状：

- 已有：
  - `LITERAL_NUMERIC_OVERFLOW`
  - `CONST_EVAL_DIVIDE_BY_ZERO`
  - `CONST_EVAL_ARITHMETIC_OVERFLOW`
- 但缺少官方明确列出的 `mod zero / shift overflow / negative shift count` 等边界项回归。

判断：

- 状态：`基础实现存在，边界覆盖不足`

### 4.12 Macro expansion 诊断

官方入口：

- `DiagnosticMacroExpand.def`

本项目现状：

- 仓库有 macro 相关语法/声明；
- 但没有看到与官方宏展开诊断域等量级对齐的定义、渲染和测试矩阵；
- 当前诊断主链并未覆盖宏展开特有错误域。

判断：

- 状态：`基本缺席`

### 4.13 CHIR / Lowered IR / Backend 诊断

官方入口：

- `DiagnosticCHIR.def`
- `DiagRefactor/DiagnosticChir.def`

本项目现状：

- 有 `compiler/chir`、`compiler/codegen`；
- 但没有看到对应 CHIR 诊断域的本地统一定义、对齐表和回归测试体系；
- 当前 diagnostics test 基本停留在前端 CFIR。

判断：

- 状态：`基本缺席`

### 4.14 Native FFI（C / Java / ObjC）

官方入口：

- `src/Sema/FFI/*`
- `src/Sema/NativeFFI/**/*`

本项目现状：

- 目前只在 extend 目标合法性上接通了很小一部分：
  - `EXTEND_C_TYPE_NOT_ALLOWED`
- 没有形成 Java / ObjC mirror / impl / interop annotation / escape checks / inheritance checks 的诊断域对齐。

判断：

- 状态：`大面积缺席`

---

## 5. 差距类型总结

相对官方，本项目当前诊断不足不是单一问题，而是五类问题并存：

### 5.1 定义缺口

官方已经有诊断定义，但本项目连对应 factory 都没有，或没有同语义建模。

典型域：

- constructor
- call binding / named arguments
- initialization
- pattern legality
- macro expand
- CHIR
- frontend/driver/module/package/incremental

### 5.2 producer 缺口

本项目已有 factory，但没有稳定 reporter / producer。

典型例子：

- `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`

### 5.3 映射缺口

解析阶段已经产生错误，但 `ConeDiagnostic -> CfirDiagnostic` 没有稳定落点。

本轮之前的典型例子：

- `INVISIBLE_MEMBER`
- `INVISIBLE_REFERENCE`

### 5.4 渲染与定位缺口

factory 存在，producer 存在，但 message / renderer / positioning strategy 不完整，导致测试和用户体验不稳定。

### 5.5 覆盖缺口

逻辑存在，但没有 `.cj` 回归样例，导致后续重构极易回归。

典型例子：

- `OVERRIDING_RETURN_TYPE_MISMATCH`
- `STRUCT_SUPER_NOT_ALLOWED`
- `ENUM_SUPER_NOT_ALLOWED`
- `EXTEND_C_TYPE_NOT_ALLOWED`

---

## 6. 当前最关键的实现不足

如果从“影响语义正确性 + 与官方差距最大 + 最容易被重构破坏”的角度排优先级，当前最关键的不是继续补零散诊断名，而是下面几块：

### P0：应优先建模的语义域

1. `constructor/`
   - no matching constructor
   - ambiguous constructor
   - recursive this/super call
   - illegal place of calling this/super
2. `initialization/`
   - used before initialization
   - class field not initialized
3. `call/`
   - named arguments
   - argument binding mismatch
   - ambiguity
4. `generic-access/`
   - generic type without arguments
   - upper-bound member resolution negative cases
5. `match/pattern`
   - pattern legality，而不只是 exhaustiveness

### P1：基础设施级补齐

1. 建一份“官方诊断域 -> 本项目入口”的正式映射表
2. 建立非 CFIR 语义诊断域的专门测试目录
3. 统一 parser / lexer / frontend / module / package 诊断的本地建模方式

### P2：平台与后端扩展域

1. Native FFI Java / ObjC / C
2. Macro expansion
3. CHIR / lowered diagnostics
4. driver / incremental / conditional compilation

---

## 7. 建议的后续文档拆分

这份文档是总览，不适合继续无限追加实现细节。建议后续按语义域继续拆分为专题文档：

- `docs/diagnostics-gap-vs-official-cpp-parser-lexer.md`
- `docs/diagnostics-gap-vs-official-cpp-call-constructor.md`
- `docs/diagnostics-gap-vs-official-cpp-initialization-legality.md`
- `docs/diagnostics-gap-vs-official-cpp-generic.md`
- `docs/diagnostics-gap-vs-official-cpp-match-pattern.md`
- `docs/diagnostics-gap-vs-official-cpp-ffi.md`
- `docs/diagnostics-gap-vs-official-cpp-backend-chir.md`

这样能避免把“总览”写成一份难以维护的超长流水账。

---

## 8. 结论

从全场景看，本项目当前并不是“诊断系统缺几个错误码”，而是处于下面这个阶段：

- `CFIR 前端语义子集`：已经形成了可以持续扩张的 checker + cone 映射 + 测试框架；
- `parser 消息层`：已经存在，但尚未与官方 parser/lexer 诊断模型一一对齐；
- `非 CFIR 诊断域`：大部分仍缺统一定义与系统测试；
- `官方全景诊断版图` 相比本项目，仍明显领先于：
  - constructor / call binding
  - initialization / legality of usage
  - generic semantics
  - pattern legality
  - frontend / driver / module / package / incremental
  - macro expansion
  - CHIR / backend
  - Native FFI 全域

因此，后续路线不应再只靠补几个 `.cj` 文件推进，而应转为：

1. 先按语义域建立差距专题；
2. 每个语义域内部按“定义 -> producer -> 映射 -> renderer -> tests”完整收口；
3. 最后再回到总览文档更新状态。

