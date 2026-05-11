# Cangjie

基于 Kotlin/JVM 的仓颉编程语言前端实现，架构参考 Kotlin K2，功能对齐官方仓颉编译器前端。

## 前端管线

```
源码 (.cj)
  → LOAD_PLUGINS      插件加载
  → PARSE             源码解析（PSI/LightTree）
  → CONDITION_COMPILE 条件编译（@When 裁剪）
  → IMPORT_PACKAGE    包导入（.cjo 外部依赖）
  → MACRO_EXPAND      宏展开
  → CFIR_BUILD        PSI → Raw CFIR
  → CFIR_RESOLVE      多 Phase 语义解析 + 诊断检查
  → FINALIZE          脱糖 → 泛型实例化 → 溢出策略
  → MANGLING          名称修饰
  → SAVE_CJO          .cjo 序列化
```

## 模块说明

### 基础设施

| 模块 | 职责 | 状态 |
|------|------|------|
| `:common` | 编译器基础设施（名称系统、内置类型、描述符、消息收集） | ✅ 已实现 |
| `:common:diagnostics` | 诊断框架核心（DiagnosticFactory、Reporter、Severity、Collector、PositioningStrategy） | ✅ 已实现 |
| `:util` | 工具库（打印机、异常处理、集合扩展） | ✅ 已实现 |
| `:generators` | 代码生成框架（树生成器、访问者生成器） | ✅ 已实现 |
| `:flatbuffers-gen` | FlatBuffers schema 与生成产物（宏协议序列化） | ✅ 已实现 |
| `:dependencies:intellij-core` | 上游 IntelliJ 平台依赖打包 | ✅ 已实现 |
| `:compiler:config` | 编译器配置（CompilerConfiguration、ContentRoots、环境设置） | ✅ 已实现 |
| `:compiler:phaser` | 编译阶段管理框架（CompilerPhase、PhaseSet、PhaserState） | ✅ 已实现 |
| `:compiler:arguments` | 编译器命令行参数定义 | ✅ 已实现 |
| `:compiler:frontend-arguments-generator` | 参数描述生成器 | ✅ 已实现 |
| `:compiler:plugin` | 插件加载占位（阶段 1） | 📋 占位 |
| `:resolution.common` | 类型推断/约束公共层（对齐 Kotlin `resolution.common`） | ✅ 已实现 |
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
| `:cfir:cfir-tree:tree-generator` | CFIR 树定义与生成器（对齐 Kotlin FIR tree-generator） | ✅ 已实现 |
| `:cfir:semantics` | CFIR 语义工具 | ✅ 已实现 |
| `:cfir:checkers` | 诊断检查器框架（Declaration/Expression/Type checkers） | ✅ 已实现 |
| `:cfir:checkers:checkers-component-generator` | Checkers 组件生成器 | ✅ 已实现 |
| `:cfir:diagnostic-renderers` | 诊断渲染器 | ✅ 已实现 |
| `:cfir:providers` | 符号 / 扩展点 providers | ✅ 已实现 |

- `:cfir:checkers` 已完成一轮针对 Kotlin/K2 API 漂移的主代码收敛：移除了已失配的 `CfirOverrideChecker` 与本地冗余的 `CfirTypeCheckUtils`，统一改为复用项目现有 `AbstractTypeChecker`/`typeContext` 入口，修复了 extend checker 的声明分派层级，并清理了 match/diagnostics 适配层中的过期调用；当前可通过定向编译（`./gradlew.bat :cfir:checkers:compileKotlin`）。
- `:cfir:cfir-tree` 的声明节点现已对齐 Kotlin FIR 的"renderer 统一出字符串"思路：`CfirDeclaration` 由 tree-generator 统一生成 `toString()`，内部委托给 `CfirRenderer.withReadability().renderElementAsString(this)`，避免各声明叶子类重复拼接文本；新增 `cfir/cfir-tree/test/.../CfirDeclarationToStringTest.kt` 覆盖 class / named function / file 三类代表性声明，并通过 `./gradlew.bat :cfir:cfir-tree:generateTree` 与 `./gradlew.bat :cfir:cfir-tree:test --tests "org.cangnova.cangjie.cfir.declarations.CfirDeclarationToStringTest"` 验证行为与 renderer 保持一致。
- `CfirErrorTypeRef` 的错误类型引用接管已进一步对齐 Kotlin FIR：`CfirErrorTypeRefBuilder` 与 `CfirErrorTypeRefImpl` 现均为手写实现，生成器已不再为 `ErrorTypeRef` 产出同名 builder/impl；当前手写版本位于 `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/builder/CfirErrorTypeRefBuilder.kt` 与 `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/CfirErrorTypeRefImpl.kt`，并已通过 `./gradlew.bat :cfir:cfir-tree:generateTree`、`./gradlew.bat :cfir:cfir-tree:compileKotlin` 与 `./gradlew.bat :cfir:raw-cfir:psi2cfir:compileKotlin` 验证不会被重新生成覆盖，且 `buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic(...) }` 调用链可编译通过。
- 与上述迁移配套，`CfirErrorTypeRef` 的主要消费点也已从旧的 `reason` 读取切换到 `diagnostic.reason` / `diagnostic` 模型（如 `CfirTypeResolver`、`CfirTypeRefExtensions`、`CfirResolvedTypesVerifier`），当前 `:cfir:cfir-tree` 与 `:cfir:raw-cfir:psi2cfir` 均可定向编译通过。
- 经过全仓扫尾，剩余 `.reason` 读取点主要已限定在其他错误节点模型（如 `CfirErrorReference`、`CfirInvalidDeclaration`、`CfirErrorExpression`），不再属于 `CfirErrorTypeRef` 迁移残留；`CfirErrorTypeRef` 主链现已统一到 `diagnostic` / `diagnostic.reason`。
- `LightTreeTypeConverter` 中对 `buildErrorTypeRef` 的旧 `reason = ...` 写法也已迁移到 `diagnostic = ConeSimpleDiagnostic(...)`，并已通过 `./gradlew.bat :cfir:raw-cfir:light-tree2cfir:compileKotlin` 复验；其中一次失败来自 Kotlin daemon 增量缓存冲突，清理该模块 `build/kotlin` 缓存后 fresh 编译通过，说明源码层迁移正确。
- `:lsp` 已升级为基于 `lsp4j` 的完整 Language Server 框架模块：提供 `CangjieLanguageServer` / `TextDocumentService` / `WorkspaceService` / `NotebookDocumentService`、服务器能力描述与协商、文档状态仓库、工作区状态管理、请求执行器，以及与真实分析模块解耦的 `AnalysisFacade` 接缝。当前真实语义能力仍通过 `TODO(...)` 占位，便于后续对接 CFIR / Analysis API；框架层已通过 `./gradlew.bat :lsp:compileKotlin` 与 `./gradlew.bat :lsp:test` 验证。

### Raw CFIR 构建

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:raw-cfir:raw-cfir-common` | Raw CFIR 构建基础设施（AbstractRawCfirBuilder、RawCfirBuilderContext） | ✅ 已实现 |
| `:cfir:raw-cfir:psi2cfir` | PSI → Raw CFIR 转换 | 🔄 进行中 |
| `:cfir:raw-cfir:light-tree2cfir` | LightTree → Raw CFIR 转换 | ✅ 已实现 |

### CFIR 语义解析

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
- 当前遗留主要从"模块不可编译"转为"后续精修/验证"性质：仍建议补齐更细粒度的定向测试、进一步审视桥接层是否可以继续内联回核心 API，以及继续清理少量历史日志/兼容调用以降低长期维护成本。

### 宏展开

| 模块 | 职责 | 状态 |
|------|------|------|
| `:macro:macro-common` | 宏展开接口定义、数据模型、FlatBuffers 协议编解码 | ✅ 已实现 |
| `:macro:macro-process` | ProcessMacroExecutor（外部进程 LSPMacroServer 实现） | ✅ 已实现 |
| `:macro:macro-stub` | StubMacroExecutor（测试/IDE 桩实现） | ✅ 已实现 |

### 序列化

| 模块 | 职责 | 状态 |
|------|------|------|
| `:cfir:cfir-serialization` | `.cjo` 文件反序列化、跨模块符号加载（序列化写入仍在补齐） | 🔄 进行中 |

### 前端协调

| 模块 | 职责 | 状态 |
|------|------|------|
| `:compiler:frontend` | 前端基础设施与编译管线协调 | ✅ 已实现 |

### 分析 API

| 模块 | 职责 | 状态 |
|------|------|------|
| `:analysis:analysis-api` | 分析 API 接口层（Session、Lifetime、Permissions） | ✅ 已实现 |
| `:analysis:analysis-api-platform-interface` | 平台接口抽象 | ✅ 已实现 |
| `:analysis:analysis-api-impl-base` | 分析 API 基础实现 | ✅ 已实现 |
| `:analysis:analysis-api-standalone` | Standalone 模式分析 API | ✅ 已实现 |
| `:analysis:analysis-api-cfir` | 分析 API 的 CFIR 实现（对齐 Kotlin analysis-api-fir） | ✅ 已实现 |
| `:analysis:analysis-api-cfir:analysis-api-cfir-generator` | CFIR 分析 API 生成器 | ✅ 已实现 |
| `:analysis:low-level-api-cfir` | 低层分析 API 的 CFIR 实现 | ✅ 已实现 |
| `:analysis:analysis-internal-utils` | 分析模块内部工具 | ✅ 已实现 |
| `:analysis:cj-references` | 跨语言引用支持 | ✅ 已实现 |
| `:analysis:stubs` | Stub 索引与 stub 数据模型 | ✅ 已实现 |
| `:analysis:decompiled` | 反编译聚合（含 4 个子模块：`decompiler-to-file-stubs` / `decompiler-to-stubs` / `decompiler-to-psi` / `light-declarations-for-decompiled`） | ✅ 已实现 |
| `:analysis:light-declarations` | Light declarations 模型 | ✅ 已实现 |
| `:analysis:symbol-light-declarations` | Symbol-based light declarations | ✅ 已实现 |
| `:analysis:analysis-tools` | 分析工具集 | ✅ 已实现 |
| `:analysis:analysis-test-framework` | 分析 API 测试框架 | ✅ 已实现 |

### 测试框架

| 模块 | 职责 | 状态 |
|------|------|------|
| `:tests:test-infrastructure` | Kotlin 风格测试基础设施（Directive/TestServices/配置 DSL） | ✅ 已实现 |
| `:cfir:analysis-tests` | CFIR 分析测试套件 | ✅ 已实现 |

### 可选后端（CHIR / CodeGen / LLVM 互操作）

| 模块 | 职责 | 状态 |
|------|------|------|
| `:compiler:chir` | CHIR 数据模型、context、builder、validator、pass pipeline、analyses、rewrites、serializer、printer/inspector | 🔄 数据模型完整，CFIR→CHIR 转换器待补 |
| `:compiler:codegen` | CHIR → LLVM IR 后端，含 ChirCodegenInput/Output、parity 测试 | 🔄 框架完整 |
| `:llvm-interop:llvm-interop-api` | LLVM 句柄、上下文、模块、Builder 纯 Kotlin API | ✅ 已实现 |
| `:llvm-interop:llvm-interop-jni` | JNI 绑定与原生库加载 | ✅ 已实现 |

### Prepare / 发布工件

| 模块 | 职责 | 状态 |
|------|------|------|
| `:prepare:frontend` / `:prepare:frontend-embeddable` | 前端发布工件 | ✅ 已实现 |
| `:prepare:test-infrastructure` / `:prepare:analysis-test-framework` | 测试基建发布工件 | ✅ 已实现 |
| `:prepare:ide-plugin-dependencies:*` | 按功能分组的 fat jar（common / psi / cfir / analysis-api / analysis-api-cfir / analysis-api-standalone） | ✅ 已实现 |
| `:prepare:ide-plugin-dependencies-module:*` | 与上述对应的 module 形态产物 | ✅ 已实现 |

## 前端管线实现进度

| 阶段 | 标识 | 状态 | 模块 |
|------|------|------|------|
| 1 | LOAD_PLUGINS | 📋 占位 | `:compiler:plugin` |
| 2 | PARSE | ✅ 已实现 | `:psi` |
| 3 | CONDITION_COMPILE | 📋 计划中 | — |
| 4 | IMPORT_PACKAGE | 🔄 进行中 | `:cfir:cfir-serialization`（仅反序列化路径完整） |
| 5 | MACRO_EXPAND | 🔄 进行中 | `:macro:macro-common` / `:macro:macro-process` / `:macro:macro-stub` |
| 6 | CFIR_BUILD | 🔄 进行中 | `:cfir:raw-cfir:psi2cfir` / `:cfir:raw-cfir:light-tree2cfir` |
| 7 | CFIR_RESOLVE | 🔄 进行中 | `:cfir:resolve` + `:cfir:checkers`（多 Phase 渐进） |
| 8 | FINALIZE | 📋 计划中 | — |
| 9 | MANGLING | 📋 计划中 | — |
| 10 | SAVE_CJO | 🔄 进行中 | `:cfir:cfir-serialization`（写入侧待补） |

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

当前公开工件包含：

- `cangjie-frontend`
- `cangjie-frontend-embeddable`
- `cangjie-frontend-arguments-description`
- `cangjie-frontend-test-infrastructure`
- `cangjie-frontend-analysis-test-framework`
- `cangjie-frontend-common`
- `cangjie-frontend-common-diagnostics`
- `cangjie-frontend-psi`
- `cangjie-frontend-analysis-api`
- `cangjie-frontend-analysis-api-platform-interface`
- `cangjie-frontend-analysis-api-impl-base`
- `cangjie-frontend-analysis-api-standalone`
- `cangjie-frontend-analysis-api-cfir`

## 测试约定

全项目测试实现与组织规范见：`TESTING_CONVENTIONS.md`。
 
## 源码输入约定

编译器前端统一使用 `CONTENT_ROOTS` 作为源码输入入口（对齐 Kotlin 的 Content Roots 模型）。`CLI_SOURCE_FILE_PATHS` 已进入弃用周期，仅用于兼容历史脚本。
 
## 开发规范

项目级开发规范与工程治理约定见：`DEVELOPMENT_CONVENTIONS.md`。

 
## 目录结构

```
cangjie/
├── analysis/                  # 分析 API 模块
│   ├── analysis-api/
│   ├── analysis-api-platform-interface/
│   ├── analysis-api-impl-base/
│   ├── analysis-api-standalone/
│   ├── analysis-api-cfir/
│   ├── low-level-api-cfir/
│   ├── analysis-internal-utils/
│   ├── cj-references/
│   ├── stubs/
│   ├── decompiled/
│   │   ├── decompiler-to-file-stubs/
│   │   ├── decompiler-to-stubs/
│   │   ├── decompiler-to-psi/
│   │   └── light-declarations-for-decompiled/
│   ├── light-declarations/
│   ├── symbol-light-declarations/
│   ├── analysis-tools/
│   └── analysis-test-framework/
├── cfir/                      # CFIR 核心模块
│   ├── cfir-common/
│   ├── cfir-cones/
│   ├── cfir-tree/             # 含 tree-generator/
│   ├── semantics/
│   ├── checkers/              # 含 checkers-component-generator/
│   ├── resolve/
│   ├── entrypoint/
│   ├── cfir-serialization/
│   ├── diagnostic-renderers/
│   ├── providers/
│   ├── analysis-tests/
│   └── raw-cfir/
│       ├── raw-cfir-common/
│       ├── psi2cfir/
│       └── light-tree2cfir/
├── compiler/                  # 编译器模块
│   ├── config/
│   ├── phaser/
│   ├── arguments/
│   ├── frontend-arguments-generator/
│   ├── frontend/
│   ├── plugin/
│   ├── chir/                  # 可选后端：CHIR 数据模型与 pass
│   └── codegen/               # 可选后端：CHIR → LLVM IR
├── llvm-interop/              # LLVM 互操作
│   ├── llvm-interop-api/
│   └── llvm-interop-jni/
├── lsp/                       # 基于 lsp4j 的 Language Server 框架
├── macro/                     # 宏展开模块
│   ├── macro-common/
│   ├── macro-process/
│   └── macro-stub/
├── psi/                       # 前端解析（PSI 树）
├── common/                    # 基础设施
│   ├── src/
│   └── diagnostics/
├── util/                      # 工具库
├── generators/                # 代码生成框架
├── flatbuffers-gen/           # FlatBuffers schema 与生成产物
├── resolution.common/         # 类型推断/约束公共层
├── prepare/                   # 发布门面工件
│   ├── frontend/
│   ├── frontend-embeddable/
│   ├── test-infrastructure/
│   ├── analysis-test-framework/
│   ├── ide-plugin-dependencies/
│   └── ide-plugin-dependencies-module/
├── tests/                     # 测试框架
│   └── test-infrastructure/
├── dependencies/              # 上游依赖打包
│   └── intellij-core/
├── docs/                      # 文档（设计、对照、归档等）
├── openspec/                  # 变更提案
│   └── changes/
├── intellij-ide/              # IntelliJ 平台插件（独立子项目）
├── deveco/                    # DevEco Studio 增强插件（独立子项目）
├── external/                  # 外部参考源码（不参与构建）
│   ├── cangjie_compiler/      # 仓颉语言编译器源码（C++ 参考实现）
│   ├── intellij-cangjie/      # IntelliJ 仓颉插件（Kotlin K1，旧）
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
- **序列化**: FlatBuffers（宏协议编解码）