# Cangjie

基于 Kotlin/JVM 的仓颉编程语言前端实现，架构参考 Kotlin K2，功能对齐官方仓颉编译器。

## 前端管线

核心管线覆盖从源码解析到 .cjo 序列化（详见 `cjfir-compiler-stages.md`）：

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
  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  可选扩展  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
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
| `:lsp` | 基于 `lsp4j` 的 Language Server 框架模块（能力协商 / 文档同步 / 工作区状态 / 分析桥接接缝） | ✅ 已实现 |

### 前端解析

| 模块 | 职责 | 状态 |
|------|------|------|
| `:psi` | PSI 树（语法树）、词法分析、语言定义、注解器 | ✅ 已实现 |

### CFIR 核心

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:cfir-common` | CFIR 基础设施（CfirSession、CfirModuleData、CfirElement） | ✅ 已实现 |
| `:cfir:cfir-cones` | 类型系统核心（ConeCangJieType、ConeClassLikeType、ConePrimitiveType） | ✅ 已实现 |
| `:cfir:cfir-tree` | IR 树定义（声明、表达式、类型引用、访问者）— 生成式 | ✅ 已实现 |
| `:cfir:symbols` | 符号提供者接口与实现、Scope 管理、内置符号 | ✅ 已实现 |
| `:cfir:checkers` | 诊断检查器框架（Declaration/Expression/Type checkers） | ✅ 已实现 |
| `:cfir:diagnostic-renderers` | 诊断渲染器 | ✅ 已实现 |

- `:cfir:checkers` 已完成一轮针对 Kotlin/K2 API 漂移的主代码收敛：移除了已失配的 `CfirOverrideChecker` 与本地冗余的 `CfirTypeCheckUtils`，统一改为复用项目现有 `AbstractTypeChecker`/`typeContext` 入口，修复了 extend checker 的声明分派层级，并清理了 match/diagnostics 适配层中的过期调用；当前可通过定向编译（`./gradlew.bat :cfir:checkers:compileKotlin`）。
- `:cfir:cfir-tree` 的声明节点现已对齐 Kotlin FIR 的“renderer 统一出字符串”思路：`CfirDeclaration` 由 tree-generator 统一生成 `toString()`，内部委托给 `CfirRenderer.withReadability().renderElementAsString(this)`，避免各声明叶子类重复拼接文本；新增 `cfir/cfir-tree/test/.../CfirDeclarationToStringTest.kt` 覆盖 class / named function / file 三类代表性声明，并通过 `./gradlew.bat :cfir:cfir-tree:generateTree` 与 `./gradlew.bat :cfir:cfir-tree:test --tests "org.cangnova.cangjie.cfir.declarations.CfirDeclarationToStringTest"` 验证行为与 renderer 保持一致。
- `CfirErrorTypeRef` 的错误类型引用接管已进一步对齐 Kotlin FIR：`CfirErrorTypeRefBuilder` 与 `CfirErrorTypeRefImpl` 现均为手写实现，生成器已不再为 `ErrorTypeRef` 产出同名 builder/impl；当前手写版本位于 `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/builder/CfirErrorTypeRefBuilder.kt` 与 `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/CfirErrorTypeRefImpl.kt`，并已通过 `./gradlew.bat :cfir:cfir-tree:generateTree`、`./gradlew.bat :cfir:cfir-tree:compileKotlin` 与 `./gradlew.bat :cfir:raw-cfir:psi2cfir:compileKotlin` 验证不会被重新生成覆盖，且 `buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic(...) }` 调用链可编译通过。
- 与上述迁移配套，`CfirErrorTypeRef` 的主要消费点也已从旧的 `reason` 读取切换到 `diagnostic.reason` / `diagnostic` 模型（如 `CfirTypeResolver`、`CfirTypeRefExtensions`、`CfirResolvedTypesVerifier`），当前 `:cfir:cfir-tree` 与 `:cfir:raw-cfir:psi2cfir` 均可定向编译通过。
- 经过全仓扫尾，剩余 `.reason` 读取点主要已限定在其他错误节点模型（如 `CfirErrorReference`、`CfirInvalidDeclaration`、`CfirErrorExpression`），不再属于 `CfirErrorTypeRef` 迁移残留；`CfirErrorTypeRef` 主链现已统一到 `diagnostic` / `diagnostic.reason`。
- `LightTreeTypeConverter` 中对 `buildErrorTypeRef` 的旧 `reason = ...` 写法也已迁移到 `diagnostic = ConeSimpleDiagnostic(...)`，并已通过 `./gradlew.bat :cfir:raw-cfir:light-tree2cfir:compileKotlin` 复验；其中一次失败来自 Kotlin daemon 增量缓存冲突，清理该模块 `build/kotlin` 缓存后 fresh 编译通过，说明源码层迁移正确。
- `:lsp` 已升级为基于 `lsp4j` 的完整 Language Server 框架模块：提供 `CangjieLanguageServer` / `TextDocumentService` / `WorkspaceService` / `NotebookDocumentService`、服务器能力描述与协商、文档状态仓库、工作区状态管理、请求执行器，以及与真实分析模块解耦的 `AnalysisFacade` 接缝。当前真实语义能力仍通过 `TODO(...)` 占位，便于后续对接 CFIR / Analysis API；框架层已通过 `./gradlew.bat :lsp:compileKotlin` 与 `./gradlew.bat :lsp:test` 验证。

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

- `BodyResolveTransformerComponents` 已补齐对 `CfirBodyResolveContext` 的关键转发实现，包括容器链、tower data、局部作用域、文件导入作用域及调用解析依赖装配。
- `:cfir:resolve` 测试基建已对齐当前调用解析/推断 API：测试 `CfirCandidate` 构造补齐了解析上下文、约束系统与显式接收者参数，旧的 `subtypeChecker` 入口迁移为 `CfirTypeRelations`，并同步修复了约束系统与 extend scope 测试中的过期构造参数。
- `:cfir:resolve` 已修复 `CfirResolutionMode.WithExpectedType` 的 API 漂移：body resolve 与 call completer 统一改为通过 `expectedTypeRef.coneType` 读取期望类型，并在 lambda 期望类型传播时补齐 `CfirResolvedTypeRef` 包装，恢复模块编译通过。
- `:resolution.common` 的类型系统对齐已完成主要迁移：`AbstractTypeChecker.RUN_SLOW_ASSERTIONS` 与 `prepareType` 契约入口已补齐，`NewCommonSuperTypeCalculator`、`TypeApproximatorConfiguration`、`AbstractTypeApproximator`、`TypeCheckerStateForConstraintSystem`、`ConstraintInjector`、`ConstraintIncorporator`、`ResultTypeResolver`、`TrivialConstraintTypeInferenceOracle`、`PostponedArgumentInputTypesResolver` 等核心文件已切换到仓颉刚性类型模型。
- `:resolution.common` 当前可通过定向编译（`./gradlew.bat :resolution.common:compileKotlin`），并新增了 `resolution.common/src/.../type/model/TypeSystemContextBridge.kt` 作为显式 context-argument 桥接层，用于消除历史 Kotlin 风格扩展调用在仓颉 `TypeSystemInferenceExtensionContext` 下的歧义。
- 编译器测试入口 `AbstractCangjieCompilerTest` 已接入 `-Dcangjie.slow.assertions=true` 的 slow assertions 开关：默认关闭，不影响正常编译路径；测试/调试时可显式开启，以执行 `resolution.common` 中已迁移的 guarded invariants。
- 当前遗留主要从“模块不可编译”转为“后续精修/验证”性质：仍建议补齐更细粒度的定向测试、进一步审视桥接层是否可以继续内联回核心 API，以及继续清理少量历史日志/兼容调用以降低长期维护成本。

### 宏展开（阶段 5）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:macro:macro-common` | 宏展开接口定义、数据模型、FlatBuffers 协议编解码 | ✅ 已实现 |
| `:macro:macro-process` | ProcessMacroExecutor（外部进程 LSPMacroServer 实现） | ✅ 已实现 |
| `:macro:macro-stub` | StubMacroExecutor（测试/IDE 桩实现） | ✅ 已实现 |

### 序列化（阶段 10）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:cfir-serialization` | .cjo 文件反序列化、跨模块符号加载 | ✅ 已实现 |

### CHIR 与代码生成（阶段 11-12，可选扩展）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:compiler:chir` | CHIR 定义、CFIR→CHIR 转换、数据流分析、验证 | ✅ 已实现 |
| `:compiler:codegen` | CHIR → LLVM IR → 机器码、LLVM 后端集成 | ✅ 已实现 |
| `:compiler:frontend` | 前端基础设施与编译管线协调 | ✅ 已实现 |

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
| 5 | MACRO_EXPAND | 🔄 进行中 | `:macro:macro-common` |
| 6 | CFIR_BUILD | 🔄 进行中 | `:cfir:raw-cfir:psi2cfir` |
| 7 | CFIR_RESOLVE | ✅ 已实现 | `:cfir:resolve` |
| 8 | FINALIZE | 📋 计划中 | — |
| 9 | MANGLING | 📋 计划中 | — |
| 10 | SAVE_CJO | ✅ 已实现 | `:cfir:cfir-serialization` |
| 11 | CFIR2CHIR | ✅ 已实现 | `:compiler:chir`（可选扩展） |
| 12 | CODEGEN | ✅ 已实现 | `:compiler:codegen`（可选扩展） |

## 构建

```bash
./gradlew :cfir:compileKotlin    # 单模块编译
./gradlew compileKotlin          # 全量编译
./gradlew check                  # 运行所有检查和测试
```

## 发布

公开工件通过根级聚合任务发布：

```bash
./gradlew publishPublicArtifacts   # 发布到配置的 Maven 仓库
./gradlew installPublicArtifacts   # 安装到 Maven Local
```

仓库支持通过 Gradle 属性注入发布目标与凭据：

```bash
./gradlew publishPublicArtifacts \
  -Pcangjie.build.deploy-url=https://maven.pkg.github.com/<OWNER>/<REPO> \
  -Pcangjie.build.deploy-username=<GITHUB_USERNAME> \
  -Pcangjie.build.deploy-password=<GITHUB_TOKEN>
```

仓库已内置 GitHub Packages workflow：

- `.github/workflows/publish-github-packages.yml`
- 支持 `workflow_dispatch`
- 支持推送 `v*` tag 时自动发布
- 手动触发时可显式输入 `version`

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
- `CfirInferenceLogsHandler`、`CfirResolvedTypesVerifier`、`CfirScopeDumpHandler` 已按 Kotlin FIR 测试 handler 模式重写：移除了本地 force-write/probe 旁路逻辑，保留 directive 驱动的 side-file/golden 验证，并将 scope dump 收敛为类/成员级 dump + 现有 `DUMP_SCOPE` 文本契约兼容层。

## 源码输入约定

编译器前端统一使用 `CONTENT_ROOTS` 作为源码输入入口（对齐 Kotlin 的 Content Roots 模型）。`CLI_SOURCE_FILE_PATHS` 已进入弃用周期，仅用于兼容历史脚本。

**弃用计划（`CLI_SOURCE_FILE_PATHS`）：**
- 迁移步骤：将原有路径列表改为写入 `CONTENT_ROOTS`（`CangJieSourceRoot`）。
- 兼容期：保留到 2026-06-30，之后移除兼容映射。

## 开发规范

项目级开发规范与工程治理约定见：`DEVELOPMENT_CONVENTIONS.md`。

## 设计与对照文档

- 四套类型推断 / 约束系统对照：`docs/type-inference-four-systems-comparison.md`
- 当前 CFIR 语义分析相对官方实现程度评估：`docs/cfir-semantic-analysis-maturity-vs-official-2026-04-08.md`

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
│   ├── frontend/
│   ├── frontend-arguments-generator/
│   ├── chir/                  # （可选扩展）
│   └── codegen/               # （可选扩展）
├── prepare/                   # 发布门面工件
│   ├── frontend/
│   └── frontend-embeddable/
├── macro/                     # 宏展开模块
│   ├── macro-common/          # 接口、数据模型、协议编解码
│   ├── macro-process/         # 外部进程执行器
│   └── macro-stub/            # 测试桩实现
├── common/                    # 基础设施
│   ├── src/
│   └── diagnostics/
├── psi/                       # 前端解析（PSI 树）
├── util/                      # 工具库
├── lsp/                       # 基于 lsp4j 的 Language Server 框架模块
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
