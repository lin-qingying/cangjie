# 仓颉编译器阶段设计（Kotlin 实现版）

> 基于 Kotlin 实现的仓颉语言前端，IR 命名为 **CFIR**（Cangjie Frontend IR），对齐 Kotlin K2 架构，同时覆盖官方仓颉编译器的前端功能。
>
> **核心管线**终止于 `SAVE_CJO`（阶段 10），后续的 `CFIR2CHIR` 和 `CODEGEN` 阶段为**可选扩展**，不在核心管线范围内。

---

## 官方编译器阶段参考

官方 C++ 编译器定义于 `CompilerInstance.h`，完整阶段如下：

```cpp
enum class CompileStage {
    LOAD_PLUGINS,            // 加载编译器插件（仅 CJNATIVE 后端）
    PARSE,                   // 词法分析 + 语法分析 → AST
    CONDITION_COMPILE,       // 条件编译，裁剪 AST
    IMPORT_PACKAGE,          // 导入外部包（加载 .cjo 文件）
    MACRO_EXPAND,            // 宏展开
    AST_DIFF,                // 增量编译作用域分析
    SEMA,                    // 语义分析（含 Desugar + TypeCheck）
    DESUGAR_AFTER_SEMA,      // 语义分析后的脱糖
    GENERIC_INSTANTIATION,   // 泛型实例化（单态化）
    OVERFLOW_STRATEGY,       // 溢出策略设置
    MANGLING,                // 符号名称修饰
    SAVE_CJO,                // 保存 .cjo 编译产物
    CHIR,                    // AST → CHIR 转换 + 优化
    CODEGEN,                 // CHIR → LLVM IR → 机器码
    SAVE_RESULTS,            // 保存 AST/CHIR 结果（CHIR 输出模式）
};
```

---

## 本实现阶段列表

在官方 15 个阶段基础上，按职责相近性合并为 **12 个阶段**：

| #  | 阶段标识       | 中文名       | 产物                                            | 合并来源                                                             |
|----|----------------|--------------|------------------------------------------------|----------------------------------------------------------------------|
| 1  | `LOAD_PLUGINS` | 插件加载     | 插件注册表                                      | —                                                                    |
| 2  | `PARSE`        | 源码解析     | PSI Tree                                        | —                                                                    |
| 3  | `CONDITION_COMPILE` | 条件编译 | 裁剪后 PSI Tree                                 | —                                                                    |
| 4  | `IMPORT_PACKAGE` | 包导入     | 合并后包列表（源码包 + 外部包）                   | —                                                                    |
| 5  | `MACRO_EXPAND` | 宏展开       | 展开后 AST                                      | —                                                                    |
| 6  | `CFIR_BUILD`   | CFIR 构建    | Raw CFIR                                        | 官方 `AST_DIFF` + `SEMA` 前半段                                      |
| 7  | `CFIR_RESOLVE` | CFIR 语义解析 | 完整语义 CFIR（含诊断检查）                      | 官方 `SEMA` 主体 + 校验（CHECKERS 为末尾 Phase）                     |
| 8  | `FINALIZE`     | 语义后处理    | 单态化 CFIR（含溢出策略标注）                    | 官方 `DESUGAR_AFTER_SEMA` + `GENERIC_INSTANTIATION` + `OVERFLOW_STRATEGY` |
| 9  | `MANGLING`     | 名称修饰     | 全局符号修饰名映射                               | —                                                                    |
| 10 | `SAVE_CJO`     | CJO 保存     | `.cjo` 文件                                     | 官方 `SAVE_CJO` + `SAVE_RESULTS`                                     |
| 11 | `CFIR2CHIR`    | CHIR 生成    | 优化后 CHIR                                     | 官方 `CHIR`                                                          |
| 12 | `CODEGEN`      | 代码生成     | `.bc` / `.o`                                    | —                                                                    |

---

## 流程总览

```
源码 (.cj)
  │
  ▼ LOAD_PLUGINS
插件注册（MetaTransform 回调就绪）
  │
  ▼ PARSE（多线程）
PSI Tree（纯语法，按 Package 组织）
  │
  ▼ CONDITION_COMPILE
裁剪后 PSI Tree（@When 条件分支移除）
  │
  ▼ IMPORT_PACKAGE
合并包列表（源码包 + .cjo 外部包 + .cjd 注解）
  │
  ▼ MACRO_EXPAND
展开后 AST（宏调用替换为展开结果）
  │
  ▼ CFIR_BUILD（含增量判断 + 前置脱糖）
Raw CFIR（结构化 IR，无类型信息）
  │
  ▼ CFIR_RESOLVE（多 Phase 渐进式，含末尾 CHECKERS）
完整语义 CFIR（类型推断、重载解析、诊断检查完成）
  │
  ▼ FINALIZE（脱糖 → 泛型实例化 → 溢出策略）
单态化 CFIR（语法糖展开、泛型具体化、溢出策略标注）
  │
  ▼ MANGLING（多线程）
全局符号修饰名就绪
  │
  ▼ SAVE_CJO
.cjo 文件（序列化 AST，供下游包导入）
  │
  ▼ CFIR2CHIR（转换 + 优化 + 插件 + 分析）
优化后 CHIR
  │
  ▼ CODEGEN（CHIR → LLVM IR → .bc）
.bc 字节码 / .o 目标文件
```

---

## 各阶段详细说明

### 1. `LOAD_PLUGINS` — 插件加载

**职责：** 加载编译器插件动态库，验证版本兼容性，注册 `MetaTransform` 回调。

**输入：** 插件路径配置（`pluginPaths`）
**输出：** 插件注册表，`MetaTransformPluginBuilder` 就绪

**官方实现：**
- 通过 `InvokeRuntime::OpenSymbolTable()` 加载 `.so` / `.dll`
- 调用插件导出的 `getMetaTransformPluginInfo()` 获取元信息
- 校验插件版本必须与 `CANGJIE_VERSION` 一致
- 仅 `CJNATIVE` 后端启用（`#ifdef CANGJIE_CODEGEN_CJNATIVE_BACKEND`）

**说明：**
此阶段必须最先执行，否则后续阶段的扩展点无法被插件订阅。官方实现中插件主要用于 MetaTransform（元编程转换），在 CHIR 阶段的 PLUGIN Phase 执行。

---

### 2. `PARSE` — 源码解析

**职责：** 词法分析 + 语法分析，将源码转换为 AST。支持多线程并行解析。

**输入：** `.cj` 源文件
**输出：** AST（按 `Package` 组织的 `File` 节点树）

**官方实现：**
- 使用 `TaskQueue` 多线程解析，每个文件一个任务
- 并行度由 `-j` / `--jobs` 选项控制
- 解析结果包括：AST `File` 节点、TokenMap、行数统计
- 文件按文件名排序归入对应 Package
- 校验宏包与非宏包不能混合

**本实现差异：**
- 使用 IntelliJ PSI 或 LightTree 作为解析前端，而非自研 Parser
- 产物为 PSI Tree（可双向映射到 AST），语义等价

---

### 3. `CONDITION_COMPILE` — 条件编译

**职责：** 根据编译配置（目标平台、版本、Feature Flag）裁剪 AST，移除不参与编译的条件分支。

**输入：** AST
**输出：** 裁剪后 AST

**官方实现：**
- `ConditionalCompilation` 类（Pimpl 模式）
- 逐 Package、逐 File 处理
- 在 AST 层面（非 Token 层）操作，不同于 C/C++ 预处理器

**说明：**
仓颉有 `@When` 条件编译语法，需在语义分析前清理无效分支，否则会引入不存在的符号引用。Kotlin 无此阶段（通过 `expect/actual` 机制处理多平台）。

---

### 4. `IMPORT_PACKAGE` — 包导入

**职责：** 解析 `import` 声明，加载外部包的 `.cjo` 文件（序列化 AST），合并源码包与外部包，解析 `.cjd` 注解文件。

**输入：** 裁剪后 AST
**输出：** 合并后包列表（源码包 + 外部依赖包符号）

**官方实现：**
- `ImportManager` 管理包依赖解析
- `CjoManager` 负责 `.cjo` 文件的加载与缓存
- `PreReadCommonPartCjoFiles()` 预读通用部分
- `ParseAndMergeCjds()` 解析并合并 `.cjd` 自定义注解文件
- 跟踪源码是否被重新解析（`IsSourceCodeImported()`），影响后续增量判断

**说明：**
外部包符号必须在语义分析前就绪，否则跨包类型引用无法正确解析。`.cjo` 使用 FlatBuffers 序列化，包含类型定义、函数签名、依赖关系等完整信息。

---

### 5. `MACRO_EXPAND` — 宏展开

**职责：** 识别并展开所有宏调用，将展开结果替换回 AST。

**输入：** AST + 外部包符号
**输出：** 展开后 AST（所有宏调用已被替换）

**官方实现：**
- `MacroExpansion::Execute()` 主入口
- 四步流程：
  1. `CollectMacros()` — 收集所有宏占位符
  2. `EvaluateMacros()` — 解释执行宏定义
  3. `ProcessMacros()` — 后处理
  4. `ReplaceAST()` — 将宏展开结果替换回 AST
- 支持声明宏、参数宏、枚举成员宏、表达式/语句宏
- 宏展开产生的 Token 记录在 `tokensEvalInMacro` 供调试

**说明：**
宏展开必须在语义分析前完成，因为宏可能引入新的声明和类型引用。Kotlin 无宏系统，使用编译器插件在 FIR Phase 间生成合成节点替代。

**本实现方案概述：**
- 本项目基于 JVM，无法 dlopen 仓颉 native 宏动态库，必须通过外部进程 `LSPMacroServer` 执行宏函数
- 模块化设计：`macro-common`（接口 + 数据模型 + FlatBuffers 编解码）、`macro-process`（进程执行器）、`macro-stub`（测试桩）
- 核心接口链：`MacroCollector`（收集宏调用）→ `MacroExecutor`（执行宏展开）→ `MacroReplacer`（AST 替换）→ `MacroExpander`（编排器）
- 通信协议：FlatBuffers + length-prefixed 帧（8 字节 uint64_le 长度前缀 + payload），通过匿名管道与 LSPMacroServer 子进程通信

---

### 6. `CFIR_BUILD` — CFIR 构建

**职责：** 将 AST 转换为 Raw CFIR 骨架，包含声明节点、作用域树、符号引用占位符，但不做类型推断。

**输入：** 展开后 AST + 外部包符号
**输出：** Raw CFIR（结构完整，类型引用未解析）

**对应官方阶段：** `AST_DIFF` + `SEMA` 前半段
**对齐 Kotlin K2：** `FIR_BUILD`

**内含子步骤：**
1. **增量判断**（内嵌 `AST_DIFF`）：比较当前 AST 与缓存快照，未变更的声明直接从缓存加载 CFIR，跳过 BUILD + RESOLVE。不产出独立产物，作为前置 Guard 逻辑内嵌。
2. **前置脱糖**（内嵌 `DesugarBeforeTypeCheck`）：官方 SEMA 内部先执行语法层脱糖，简化结构以便后续类型检查。本实现在 AST → CFIR 转换时一并完成。
3. **CFIR 构建**：建立声明节点、作用域树、符号引用占位符。

**合并理由：**
- `AST_DIFF` 不产出独立 IR，只是"是否跳过"的判断，作为 CFIR_BUILD 的 Guard 更自然
- `DesugarBeforeTypeCheck` 是 AST → CFIR 转换的预处理步骤，职责同属"构建"

---

### 7. `CFIR_RESOLVE` — CFIR 语义解析

**职责：** 对 Raw CFIR 进行多 Phase 渐进式语义解析，完成类型推断、重载解析、符号绑定，并在末尾运行诊断检查器。

**输入：** Raw CFIR
**输出：** 完整语义 CFIR（所有表达式携带类型信息，所有符号引用已解析，诊断检查通过）

**对应官方阶段：** `SEMA` 主体 + 校验
**对齐 Kotlin K2：** `FIR_RESOLVE` + `FIR_CHECK`

**内部 Phase 顺序：**

```
RAW_CFIR
  → IMPORTS          # 导入符号绑定
  → MACRO_EXPAND     # 宏展开（替换宏调用，可能重建文件）
  → SUPER_TYPES      # 超类/接口层次解析（class <: SuperClass）
  → TYPES            # 显式类型引用解析（参数类型、返回类型、字段类型）
  → STATUS           # 声明状态解析（public/private/open/abstract/mut 等修饰符）
  → EXTENSIONS       # extend 声明解析（仓颉特有：extend Type <: Interface）
  → IMPLICIT_TYPES   # 隐式类型推断
  → BODY_RESOLVE     # 方法体类型推断、重载解析
  → CHECKERS         # 诊断检查器（可见性、类型兼容性、未使用变量等）
```

**关键特性：**
- **惰性分阶段解析**：每个声明独立跟踪已完成的 Phase，同一文件中不同声明可处于不同 Phase，按需推进
- **EXTENSIONS Phase**：仓颉特有，解析 `extend Type <: Interface { ... }` 声明，Kotlin 无此概念
- **IMPLICIT_TYPES Phase**：独立于 BODY_RESOLVE，先推断声明级别的隐式类型（如属性初始化器推导类型），再进入函数体解析
- **CHECKERS Phase**：诊断检查作为最后一个 Phase 统一管理，若报告错误则编译终止
- **插件扩展点**：每个 Phase 边界均可注入编译器插件

**合并理由：**
`CFIR_CHECK` 不再作为独立编译阶段，因为 `CHECKERS` 已是 `CfirResolvePhase` 枚举的最后一个值。诊断检查与语义解析共享同一套 Phase 推进机制，独立成阶段会割裂 Phase 的连续性。

---

### 8. `FINALIZE` — 语义后处理

**职责：** 在语义分析完成后，执行脱糖、泛型实例化、溢出策略标注三项后处理。

**输入：** 完整语义 CFIR
**输出：** 单态化 CFIR（语法糖展开、泛型具体化、溢出策略标注完成）

**对应官方阶段：** `DESUGAR_AFTER_SEMA` + `GENERIC_INSTANTIATION` + `OVERFLOW_STRATEGY`

**内含子步骤（严格顺序执行）：**

**① 语义后脱糖（DESUGAR_AFTER_SEMA）**
- `TypeChecker::PerformDesugarAfterSema()` 主入口
- 展开依赖类型信息的语法糖（如操作符重载展开需要知道操作数类型）
- `TestManager::MarkDeclsForTestIfNeeded()` 标记测试声明

**② 泛型实例化（GENERIC_INSTANTIATION）**
- `GenericInstantiationManager::GenericInstantiatePackage()` 逐包实例化
- 为每个具体类型参数组合生成单态化版本
- 实例化后执行 `TypeChecker::PerformDesugarAfterInstantiation()` 处理：
  - autobox（值类型自动装箱）
  - 递归枚举类型解析
- 增量编译路径可跳过重新实例化
- 仅在源码被导入时执行（`importManager.IsSourceCodeImported()`）

**③ 溢出策略标注（OVERFLOW_STRATEGY）**
- `TypeChecker::SetOverflowStrategy()` 设置策略
- 仅在 `overflowStrategy != NA` 时执行
- 为每个算术节点标注运行时溢出处理方式：
  - `CHECKED` — 运行时检查溢出
  - `WRAPPING` — 环绕溢出
  - `THROWING` — 溢出时抛出异常
  - `SATURATING` — 饱和到最大/最小值

**合并理由：**
三者紧密耦合，形成 `脱糖 → 实例化 → 实例化后脱糖 → 溢出标注` 的串行流水线。官方实现中泛型实例化后还要再做一轮脱糖（`PerformDesugarAfterInstantiation`），三者拆开反而割裂了这条处理链。溢出策略是轻量标注操作，不产出独立产物。

---

### 9. `MANGLING` — 名称修饰

**职责：** 为所有声明生成唯一的修饰名（Mangled Name），供链接器和 CHIR/LLVM 使用。支持多线程并行。

**输入：** 单态化 CFIR
**输出：** 全局符号修饰名映射

**官方实现：**
- 收集所有顶层声明，添加完整包名
- 使用 `TaskQueue` 并行修饰（30 个声明一批）
- `Walker` 遍历每个节点并生成修饰名
- Lambda 表达式收集后串行修饰（使用全局计数器，不适合并发）
- `ManglerContext` 跟踪通配符变量、局部变量/函数/Lambda 的作用域信息
- CJNATIVE 后端使用 `CHIRMangler`（扩展功能），其他后端使用 `BaseMangler`
- 修饰后对泛型实例化声明按修饰名排序（`SortForBep()`），保证确定性

**独立保留理由：**
多线程 + Lambda 串行的特殊并发逻辑，职责清晰且实现复杂度高，不适合与其他阶段混合。

---

### 10. `SAVE_CJO` — CJO 保存

**职责：** 将编译产物序列化为 `.cjo`（Cangjie Object）文件，供下游包作为外部依赖导入。

**输入：** 修饰名就绪的 CFIR/AST
**输出：** `.cjo` 文件 + `.cjo.flag` 标志文件

**对应官方阶段：** `SAVE_CJO` + `SAVE_RESULTS`

**官方实现：**
- `ImportManager::ExportAST()` 执行序列化
- 使用 FlatBuffers 二进制格式
- 启用 `-g`（调试）或 `--coverage`（覆盖率）时包含绝对文件路径
- 写入成功后创建 `.cjo.flag` 标志文件

**合并理由：**
官方 `SAVE_RESULTS` 仅在 `outputMode == CHIR` 时执行延迟的 `SaveCjo()`，其他模式直接跳过。两者本质都是序列化保存，合并为单一阶段，内部根据 outputMode 决定保存时机。

**说明：**
`.cjo` 是仓颉编译的核心产物格式，等价于 Kotlin 的 `.klib`，包含序列化 AST、类型签名、符号信息、依赖关系。本实现的序列化模块为 `cfir-serialization`。

---

### 11. `CFIR2CHIR` — CHIR 生成（可选扩展）

**职责：** 将 CFIR 转换为 CHIR（Cangjie High-level IR），执行 CHIR 级别优化和分析。

**输入：** 完整 CFIR
**输出：** 优化后 CHIR

**官方实现（`ToCHIR` 类）：**

CHIR 是基于 CFG（控制流图）的高级中间表示，独立于后端目标。

**CHIR Phase：**
```
RAW     → 翻译完成，未优化
OPT     → 编译器优化完成
PLUGIN  → 插件转换完成
ANALYSIS_FOR_CJLINT → 静态分析完成
```

**主要优化 Pass：**
- 闭包转换（Closure Conversion）
- 函数内联（Function Inline）
- Lambda 内联（Lambda Inline）
- 去虚化（Devirtualization）
- 常量传播（Const Propagation）
- 死代码消除（Dead Code Elimination）
- 冗余加载消除（Redundant Load Elimination）
- 无副作用标记（No Side Effect Marker）
- 基本块合并（Merge Blocks）
- 无用分配消除（Useless Allocate Elimination）
- 数组优化（Array Lambda Opt）
- 值范围传播（Range Propagation）
- 覆盖率插桩（Sanitizer Coverage）

**CHIR 核心节点类型：**
- **Value 类型**：`Literal`, `GlobalVar`, `GlobalFunc`, `Func`, `LocalVar`, `Parameter`, `Block`, `BlockGroup`
- **Expression 类型**：`Apply`, `ApplyWithException`, `Invoke`（虚调用）, `Binary`, `Unary`, `TypeCast`, `Load`, `Field`, `Allocate`, `Lambda`
- **Terminator**：`Return`, `Branch`, `Throw`
- **Type 定义**：`ClassDef`, `StructDef`, `EnumDef`, `ExtendDef`

---

### 12. `CODEGEN` — 代码生成（可选扩展）

**职责：** 将 CHIR 转换为 LLVM IR，生成目标平台字节码。

**输入：** 优化后 CHIR
**输出：** `.bc`（LLVM 字节码）/ `.o` 目标文件 / 可执行文件

**官方实现：**
- `CodeGen::GenPackageModules()` 创建 LLVM Module
- `CodeGen::SavePackageModule()` 写入 `.bc` 文件
- 支持多模块并行保存（`TaskQueue`）
- 后续由 LLVM 工具链完成：`opt`（优化）→ `llc`（汇编/目标码）→ `ld`（链接）

**输出产物：**
- `.bc` — LLVM 字节码
- `.o` / `.obj` — 目标文件
- `.a` / `.lib` — 静态库
- `.so` / `.dll` — 动态库
- 可执行文件

---

## 与官方仓颉对比

| 官方阶段                | 本实现对应                     | 处理方式  | 说明                                     |
|------------------------|-------------------------------|-----------|------------------------------------------|
| `LOAD_PLUGINS`         | `LOAD_PLUGINS`                | ✅ 保留   | 完全对应                                  |
| `PARSE`                | `PARSE`                       | ✅ 保留   | 本实现使用 PSI/LightTree，产物等价         |
| `CONDITION_COMPILE`    | `CONDITION_COMPILE`           | ✅ 保留   | 仓颉特有，裁剪 @When 条件分支              |
| `IMPORT_PACKAGE`       | `IMPORT_PACKAGE`              | ✅ 保留   | 加载 .cjo 外部依赖                        |
| `MACRO_EXPAND`         | `MACRO_EXPAND`                | ✅ 保留   | 完全对应，四步宏展开流程                    |
| `AST_DIFF`             | → 内嵌 `CFIR_BUILD`           | 🔀 内嵌   | 不产出独立产物，作为构建前置 Guard          |
| `SEMA`（前半）          | → `CFIR_BUILD`                | 🔀 拆分   | 对齐 K2 `FIR_BUILD`，含前置脱糖           |
| `SEMA`（主体 + 校验）   | → `CFIR_RESOLVE`              | 🔀 拆分   | 对齐 K2 `FIR_RESOLVE` + `FIR_CHECK`      |
| `DESUGAR_AFTER_SEMA`   | → 合入 `FINALIZE` ①           | 🔀 合并   | 语义后处理流水线第一步                     |
| `GENERIC_INSTANTIATION`| → 合入 `FINALIZE` ②           | 🔀 合并   | 语义后处理流水线第二步                     |
| `OVERFLOW_STRATEGY`    | → 合入 `FINALIZE` ③           | 🔀 合并   | 语义后处理流水线第三步                     |
| `MANGLING`             | `MANGLING`                    | ✅ 保留   | 多线程修饰，职责独立                       |
| `SAVE_CJO`             | `SAVE_CJO`                    | ✅ 合并   | 吸收 `SAVE_RESULTS` 的延迟保存逻辑        |
| `CHIR`                 | `CFIR2CHIR`                   | ✅ 改名   | 名称更明确（表达转换过程）                  |
| `CODEGEN`              | `CODEGEN`                     | ✅ 保留   | CHIR → LLVM IR → 机器码                  |
| `SAVE_RESULTS`         | → 合入 `SAVE_CJO`             | 🔀 合并   | 仅 CHIR 模式延迟保存，逻辑归入 SAVE_CJO   |

**核心设计决策：**
- 官方 `SEMA` **拆分**为 `CFIR_BUILD` + `CFIR_RESOLVE`，对齐 Kotlin K2 架构
- 官方 `DESUGAR_AFTER_SEMA` + `GENERIC_INSTANTIATION` + `OVERFLOW_STRATEGY` **合并**为 `FINALIZE`，三者紧密耦合

---

## 与 Kotlin K2 对比

| Kotlin K2 阶段                  | 本实现对应              | 处理方式  | 说明                           |
|--------------------------------|------------------------|-----------|-------------------------------|
| *(无)*                          | `CONDITION_COMPILE`    | ➕ 新增   | 仓颉特有条件编译               |
| *(无)*                          | `IMPORT_PACKAGE`       | ➕ 独立   | 仓颉跨包 .cjo 依赖需提前加载   |
| *(无，用插件替代)*                | `MACRO_EXPAND`         | ➕ 新增   | 仓颉有独立宏系统               |
| `FIR_BUILD`                     | `CFIR_BUILD`           | ✅ 对齐   | 含增量判断 + 前置脱糖          |
| `FIR_RESOLVE` + `FIR_CHECK`    | `CFIR_RESOLVE`         | ✅ 对齐   | 多 Phase 渐进，CHECKERS 为末尾  |
| `FIR2IR`（含脱糖）              | `FINALIZE`             | 🔀 独立   | 脱糖 + 实例化 + 溢出策略       |
| *(无，类型擦除)*                 | `FINALIZE`（泛型实例化） | ➕ 新增   | LLVM 后端需显式单态化          |
| *(无)*                          | `MANGLING`             | ➕ 独立   | 仓颉独立名称修饰               |
| *(klib 机制)*                    | `SAVE_CJO`             | 🔀 显式化 | 仓颉 .cjo 格式                |
| `FIR2IR`                        | `CFIR2CHIR`            | ✅ 对齐   | 前端 IR → 后端 IR              |
| `IR_LOWERING`                   | *(内含于 CFIR2CHIR)*    | 🔀 内嵌   | CHIR 内部完成优化和降级         |
| `CODEGEN`                       | `CODEGEN`              | ✅ 对齐   | CHIR → LLVM IR → 机器码       |

---

## CFIR Resolve 内部 Phase 设计

CFIR_RESOLVE 采用 Kotlin K2 风格的分阶段解析，每个声明独立跟踪已完成的 Phase：

| Phase            | 名称       | 职责                                                    |
|------------------|-----------|--------------------------------------------------------|
| `RAW_CFIR`       | 初始状态   | PSI/LightTree → CFIR 转换完成                           |
| `IMPORTS`        | 导入绑定   | 解析 import 语句，绑定导入符号                            |
| `MACRO_EXPAND`   | 宏展开     | 展开宏调用，可能替换整个 CfirFile                          |
| `SUPER_TYPES`    | 超类解析   | 解析 `class <: SuperClass`，构建继承层次                  |
| `TYPES`          | 类型解析   | 解析显式类型（参数、返回值、字段类型引用）                   |
| `STATUS`         | 状态解析   | 解析声明修饰符（public/private/open/abstract/mut/static） |
| `EXTENSIONS`     | 扩展解析   | 解析 `extend Type <: Interface { ... }`（仓颉特有）       |
| `IMPLICIT_TYPES` | 隐式推断   | 推断省略的类型声明（如 `let x = 42`）                     |
| `BODY_RESOLVE`   | 函数体解析 | 方法体类型推断、重载解析、表达式类型计算                     |
| `CHECKERS`       | 诊断检查   | 运行所有诊断检查器（可见性、类型兼容性、未使用变量等）        |

**与 Kotlin K2 FIR Phase 对比：**

| Kotlin K2 Phase                 | CFIR Phase                      | 说明                    |
|---------------------------------|---------------------------------|------------------------|
| `RAW_FIR`                       | `RAW_CFIR`                      | 对应                    |
| `IMPORTS`                       | `IMPORTS`                       | 对应                    |
| *(无，用插件替代)*                | `MACRO_EXPAND`                  | 仓颉特有宏展开，纳入 resolve phase |
| `SUPER_TYPES`                   | `SUPER_TYPES`                   | 对应                    |
| `SEALED_CLASS_INHERITORS`       | *(无)*                           | 仓颉无 sealed 继承者推断 |
| `TYPES`                         | `TYPES`                         | 对应                    |
| `STATUS`                        | `STATUS`                        | 对应                    |
| `CONTRACTS`                     | *(无)*                           | 仓颉无 Kotlin 风格 contract |
| *(无)*                           | `EXTENSIONS`                    | 仓颉特有 extend 声明    |
| `IMPLICIT_TYPES_BODY_RESOLVE`   | `IMPLICIT_TYPES` + `BODY_RESOLVE` | 拆分为两步，粒度更细    |
| *(无)*                           | `CHECKERS`                      | 诊断检查纳入 Phase 管理  |

---

## 合并决策汇总

| 合并项                          | 合并进          | 理由                                                      |
|--------------------------------|----------------|----------------------------------------------------------|
| `AST_DIFF`                     | `CFIR_BUILD`   | 不产出独立产物，只是"是否跳过"的判断，作为前置 Guard         |
| `CFIR_CHECK`                   | `CFIR_RESOLVE` | CHECKERS 已是 CfirResolvePhase 的末尾 Phase，无需独立阶段  |
| `DESUGAR_AFTER_SEMA`           | `FINALIZE` ①   | 语义后处理流水线第一步，与泛型实例化紧密耦合                  |
| `GENERIC_INSTANTIATION`        | `FINALIZE` ②   | 实例化后还需再做一轮脱糖，三者形成不可割裂的串行链            |
| `OVERFLOW_STRATEGY`            | `FINALIZE` ③   | 轻量标注操作，不产出独立产物                                 |
| `SAVE_RESULTS`                 | `SAVE_CJO`     | 仅 CHIR 模式执行延迟保存，逻辑简单，归入统一的保存阶段        |

---

## 序列化格式参考

官方编译器使用 FlatBuffers 序列化，Schema 定义于 `schema/` 目录：

| Schema 文件            | 用途                               |
|-----------------------|-----------------------------------|
| `PackageFormat.fbs`   | CHIR Package 序列化（类型、函数、类） |
| `NodeFormat.fbs`      | AST 节点序列化（表达式、语句）        |
| `ModuleFormat.fbs`    | 模块/包头信息                       |
| `BCHIRFormat.fbs`     | 字节码 IR 序列化                    |
| `CachedASTFormat.fbs` | 增量编译 AST 缓存                   |
| `MacroMsgFormat.fbs`  | 宏元数据序列化                      |

**类型属性（64 位 bitset）：**
- 访问级别：public / private / protected / internal
- 声明修饰：static / abstract / virtual / override / sealed
- 特殊标记：foreign / mutable / const / readonly / final
- 编译器生成：imported / generic instantiated 等