# Cangjie

基于 Kotlin/JVM 的仓颉编程语言编译器实现，架构参考 Kotlin K2，功能对齐官方仓颉编译器。

## 编译器管线

12 阶段管线设计（详见 `cjfir-compiler-stages.md`）：

```
源码 (.cj)
  → LOAD_PLUGINS     插件加载
  → PARSE            源码解析（PSI/LightTree）
  → CONDITION_COMPILE 条件编译（@When 裁剪）
  → IMPORT_PACKAGE   包导入（.cjo 外部依赖）
  → MACRO_EXPAND     宏展开
  → CFIR_BUILD       PSI → Raw CFIR
  → CFIR_RESOLVE     多 Phase 语义解析 + 诊断检查
  → FINALIZE         脱糖 → 泛型实例化 → 溢出策略
  → MANGLING         名称修饰
  → SAVE_CJO         .cjo 序列化
  → CFIR2CHIR        CFIR → CHIR 转换 + 优化
  → CODEGEN          CHIR → LLVM IR → 机器码
```

## 模块说明

### 基础设施

| 模块 | 职责 | 状态 |
|------|------|------|
| `:common` | 编译器基础设施（名称系统、内置类型、描述符、消息收集） | ✅ 已实现 |
| `:common:diagnostics` | 诊断框架核心（DiagnosticFactory、Reporter、Severity、Collector、PositioningStrategy） | ✅ 已实现 |
| `:util` | 工具库（打印机、异常处理、集合扩展） | ✅ 已实现 |
| `:generators` | 代码生成框架（树生成器、访问者生成器） | ✅ 已实现 |
| `:compiler:config` | 编译器配置（CompilerConfiguration、ContentRoots、环境设置） | ✅ 已实现 |
| `:compiler:phaser` | 编译阶段管理框架（CompilerPhase、PhaseSet、PhaserState） | ✅ 已实现 |
| `:compiler:arguments` | 编译器命令行参数定义 | ✅ 已实现 |

### 前端解析

| 模块 | 职责 | 状态 |
|------|------|------|
| `:psi` | PSI 树（语法树）、词法分析、语言定义、注解器 | ✅ 已实现 |

### CFIR 核心

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:cfir-common` | CFIR 基础设施（CfirSession、CfirModuleData、CfirElement） | ✅ 已实现 |
| `:cfir:cfir-cones` | 类型系统核心（ConeCangjieType、ConeClassLikeType、ConePrimitiveType） | ✅ 已实现 |
| `:cfir:cfir-tree` | IR 树定义（声明、表达式、类型引用、访问者）— 生成式 | ✅ 已实现 |
| `:cfir:symbols` | 符号提供者接口与实现、Scope 管理、内置符号 | ✅ 已实现 |
| `:cfir:checkers` | 诊断检查器框架（Declaration/Expression/Type checkers） | ✅ 已实现 |
| `:cfir:diagnostic-renderers` | 诊断渲染器 | ✅ 已实现 |

### Raw CFIR 构建（阶段 6）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:raw-cfir:raw-cfir-common` | Raw CFIR 构建基础设施（AbstractRawCfirBuilder、RawCfirBuilderContext） | ✅ 已实现 |
| `:cfir:raw-cfir:psi2cfir` | PSI → Raw CFIR 转换 | 🔄 进行中 |
| `:cfir:raw-cfir:light-tree2cfir` | LightTree → Raw CFIR 转换 | ✅ 已实现 |

### CFIR 语义解析（阶段 7）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:resolve` | 多 Phase 语义解析（类型推断、重载解析、诊断检查） | ✅ 已实现 |
| `:cfir:entrypoint` | CFIR 前端入口（Session 工厂、Pipeline 配置） | ✅ 已实现 |

### 序列化（阶段 10）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:cfir-serialization` | .cjo 文件反序列化、跨模块符号加载 | ✅ 已实现 |

### CHIR 与代码生成（阶段 11-12）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:compiler:chir` | CHIR 定义、CFIR→CHIR 转换、数据流分析、验证 | ✅ 已实现 |
| `:compiler:codegen` | CHIR → LLVM IR → 机器码、LLVM 后端集成 | ✅ 已实现 |
| `:compiler:cli` | CLI 入口、编译管线协调 | ✅ 已实现 |

### 分析 API

| 模块 | 职责 | 状态 |
|------|------|------|
| `:analysis:analysis-api` | 分析 API 平台接口（Session、Lifetime、Permissions） | ✅ 已实现 |
| `:analysis:analysis-api-impl-base` | 分析 API 基础实现 | ✅ 已实现 |
| `:analysis:analysis-api-cfir` | 分析 API 的 CFIR 实现（对齐 Kotlin analysis-api-fir） | ✅ 已实现 |
| `:analysis:analysis-test-framework` | 分析 API 测试框架 | ✅ 已实现 |

### 测试框架

| 模块 | 职责 | 状态 |
|------|------|------|
| `:tests:test-infrastructure` | Kotlin 风格测试基础设施（Directive/TestServices/配置 DSL） | ✅ 已实现 |
| `:cfir:analysis-tests` | CFIR 分析测试套件 | ✅ 已实现 |

### LLVM 互操作

| 模块 | 职责 | 状态 |
|------|------|------|
| `:llvm-interop:llvm-interop-api` | LLVM JNI 接口定义 | ✅ 已实现 |
| `:llvm-interop:llvm-interop-jni` | LLVM JNI 本地实现（C++） | ✅ 已实现 |

## 编译管线实现进度

| 阶段 | 标识 | 状态 | 模块 |
|------|------|------|------|
| 1 | LOAD_PLUGINS | 📋 计划中 | `:compiler:plugin` |
| 2 | PARSE | ✅ 已实现 | `:psi` |
| 3 | CONDITION_COMPILE | 📋 计划中 | — |
| 4 | IMPORT_PACKAGE | 📋 计划中 | — |
| 5 | MACRO_EXPAND | 📋 计划中 | — |
| 6 | CFIR_BUILD | 🔄 进行中 | `:cfir:raw-cfir:psi2cfir` |
| 7 | CFIR_RESOLVE | ✅ 已实现 | `:cfir:resolve` |
| 8 | FINALIZE | 📋 计划中 | — |
| 9 | MANGLING | 📋 计划中 | — |
| 10 | SAVE_CJO | ✅ 已实现 | `:cfir:cfir-serialization` |
| 11 | CFIR2CHIR | ✅ 已实现 | `:compiler:chir` |
| 12 | CODEGEN | ✅ 已实现 | `:compiler:codegen` |

## 构建

```bash
./gradlew :cfir:compileKotlin    # 单模块编译
./gradlew compileKotlin          # 全量编译
./gradlew check                  # 运行所有检查和测试
```

## 测试约定

全项目测试实现与组织规范见：`TESTING_CONVENTIONS.md`。

### 测试框架进展

- 已引入 Kotlin 风格的轻量测试配置模型：`TestConfigurationBuilder`、`TestFacade`、`AnalysisHandler`、`AbstractCangjieCompilerTest`。
- 采用树形测试模块结构：测试基础设施归属 `:tests:test-infrastructure`。
- testData 与测试代码按模块归属放置（例如 `psi2cfir` 测试仍放在 `cfir/raw-cfir/psi2cfir` 模块内）。
- Raw CFIR 测试入口已对齐 Kotlin 风格为 **Generated 类**（模块内自洽，不依赖独立 `compiler-tests`）：
  - 生成器：`cfir/raw-cfir/psi2cfir/testFixtures/.../TestGeneratorForPsi2Cfir.kt`
  - 产物：`cfir/raw-cfir/psi2cfir/tests-gen/.../RawCfirBuilderTestCaseGenerated.kt`
  - 抽象基类：`cfir/raw-cfir/psi2cfir/testFixtures/.../AbstractRawCfirBuilderTestCase.kt`
- Raw CFIR testData：`cfir/raw-cfir/psi2cfir/testData/rawBuilder`。
- 已接入 `DUMP_CFIR` 指令与 golden file 对比；默认严格比对模式，可通过 `-Dupdate.test.data=true` 更新期望文件。
- 已新增 4 类测试入口（对齐 Kotlin 分类）：
  - `RawCfirBuilderLazyBodiesByAstTestGenerated`
  - `RawCfirBuilderLazyBodiesByStubTestGenerated`
  - `RawCfirBuilderSourceElementMappingTestGenerated`
  - `RawCfirBuilderTestCaseGenerated`
- `PsiRawCfirBuilder` 已支持 `BodyBuildingMode`（`NORMAL`/`LAZY_BODIES`）。
- 已新增 `CfirBasicTypeRef`（基础类型引用）与 `CfirVArrayTypeRef`（定长数组类型引用）。
- tests-gen 已加入 all-files-present 等效校验，新增 `.cj` 用例将被覆盖检查拦截漏测。

## 源码输入约定

编译器前端统一使用 `CONTENT_ROOTS` 作为源码输入入口（对齐 Kotlin 的 Content Roots 模型）。`CLI_SOURCE_FILE_PATHS` 已进入弃用周期，仅用于兼容历史脚本。

**弃用计划（`CLI_SOURCE_FILE_PATHS`）：**
- 迁移步骤：将原有路径列表改为写入 `CONTENT_ROOTS`（`CangJieSourceRoot`）。
- 兼容期：保留到 2026-06-30，之后移除兼容映射。

## 开发规范

项目级开发规范与工程治理约定见：`DEVELOPMENT_CONVENTIONS.md`。

## 目录结构

```
cangjie/
├── analysis/                  # 分析 API 模块
│   ├── analysis-api/
│   ├── analysis-api-impl-base/
│   ├── analysis-api-cfir/
│   └── analysis-test-framework/
├── cfir/                      # CFIR 核心模块
│   ├── cfir-common/
│   ├── cfir-cones/
│   ├── cfir-tree/
│   ├── symbols/
│   ├── checkers/
│   ├── resolve/
│   ├── entrypoint/
│   ├── cfir-serialization/
│   ├── diagnostic-renderers/
│   └── raw-cfir/
│       ├── raw-cfir-common/
│       ├── psi2cfir/
│       └── light-tree2cfir/
├── compiler/                  # 编译器模块
│   ├── config/
│   ├── phaser/
│   ├── arguments/
│   ├── cli/
│   ├── chir/
│   └── codegen/
├── common/                    # 基础设施
│   ├── src/
│   └── diagnostics/
├── psi/                       # 前端解析（PSI 树）
├── util/                      # 工具库
├── generators/                # 代码生成框架
├── tests/                     # 测试框架
│   └── test-infrastructure/
├── llvm-interop/              # LLVM 互操作
│   ├── llvm-interop-api/
│   └── llvm-interop-jni/
├── flatbuffers-gen/           # FlatBuffers 生成（CHIR 序列化）
├── openspec/                  # 变更提案
│   └── changes/
├── external/                  # 外部参考源码（不参与构建）
│   ├── cangjie_compiler/      # 仓颉语言编译器源码（C++ 参考实现）
│   ├── intellij-cangjie/      # IntelliJ 仓颉插件（Kotlin K1）
│   └── kotlin/                # Kotlin 编译器源代码（K2 架构参考）
├── cjfir-compiler-stages.md   # 编译器阶段设计文档
├── DEVELOPMENT_CONVENTIONS.md # 开发规范
├── TESTING_CONVENTIONS.md     # 测试规范
└── gradle/                    # Gradle 配置
```

## 技术栈

- **语言**: Kotlin/JVM
- **JDK**: 17
- **构建工具**: Gradle (Kotlin DSL)
- **测试框架**: JUnit 5（JUnitPlatform）
- **代码生成**: FlatBuffers（CHIR 序列化）、LLVM（代码生成后端）