# Cangjie 语言前端项目

## 项目定位

基于 Kotlin/JVM 的仓颉编程语言前端实现，架构参考 Kotlin K2，功能对齐官方仓颉编译器（C++），覆盖从源码解析到 .cjo 序列化的完整前端管线（详见 `cjfir-compiler-stages.md`）。

## 编译管线

### 核心管线（LOAD_PLUGINS → SAVE_CJO）

```
LOAD_PLUGINS → PARSE → CONDITION_COMPILE → IMPORT_PACKAGE → MACRO_EXPAND
→ CFIR_BUILD → CFIR_RESOLVE → FINALIZE → MANGLING → SAVE_CJO
```

### 可选后端（CFIR2CHIR → CODEGEN）

```
CFIR2CHIR → CODEGEN
```

## 模块结构

完整模块清单以 `settings.gradle.kts` 为唯一真相源，下列按子系统分组列出主要一方模块：

**基础设施**
- `:common` / `:common:diagnostics` — 名称系统、内置类型、描述符、消息收集；诊断框架核心
- `:util` — 打印机、异常框架、集合扩展
- `:generators` — 代码生成框架
- `:dependencies:intellij-core` — 上游 IntelliJ 平台依赖
- `:flatbuffers-gen` — FlatBuffers schema 与生成产物（宏协议序列化）

**编译器驱动与配置**
- `:compiler` 聚合
- `:compiler:config` — `CompilerConfiguration`、Content Roots、环境
- `:compiler:phaser` — `CompilerPhase`、`PhaseSet`、`PhaserState`
- `:compiler:arguments` / `:compiler:frontend-arguments-generator` — 命令行参数定义与生成
- `:compiler:frontend` — 前端基础设施与编译管线协调
- `:compiler:plugin` — 插件加载占位
- `:resolution.common` — 类型推断/约束公共层（对齐 Kotlin `resolution.common`）

**前端解析（PSI / Lexer / Parser）**
- `:psi` — 词法、语法、PSI 树、注解器

**CFIR 数据模型**
- `:cfir` 聚合
- `:cfir:cfir-common` — `CfirSession`、`CfirModuleData`、`CfirElement`
- `:cfir:cfir-cones` — 类型系统核心（`ConeCangjieType`、`ConeClassLikeType`、`ConePrimitiveType`）
- `:cfir:cfir-tree` — 生成式 IR 树（声明、表达式、类型引用、访问者）
- `:cfir:cfir-tree:tree-generator` — 上述生成器
- `:cfir:semantics` — 语义工具
- `:cfir:diagnostic-renderers` — 诊断渲染器
- `:cfir:providers` — 符号/扩展点 providers

**Raw CFIR 构建（阶段 6）**
- `:cfir:raw-cfir` 聚合
- `:cfir:raw-cfir:raw-cfir-common`
- `:cfir:raw-cfir:psi2cfir` — PSI → Raw CFIR
- `:cfir:raw-cfir:light-tree2cfir` — LightTree → Raw CFIR

**CFIR 语义解析（阶段 7）**
- `:cfir:resolve` — 多 Phase 语义解析（IMPORTS/SUPER_TYPES/TYPES/STATUS/EXTENSIONS/IMPLICIT_TYPES/BODY_RESOLVE/CHECKERS）
- `:cfir:checkers` / `:cfir:checkers:checkers-component-generator` — 诊断检查器与生成器
- `:cfir:entrypoint` — Session 工厂、Pipeline 配置

**宏展开（阶段 5）**
- `:macro:macro-common` — 接口、数据模型、FlatBuffers 编解码
- `:macro:macro-process` — 外部进程执行器（LSPMacroServer）
- `:macro:macro-stub` — 测试/IDE 桩

**序列化（阶段 4 / 10）**
- `:cfir:cfir-serialization` — `.cjo` 反序列化与跨模块符号加载（写入侧仍在补齐）

**Analysis API（IDE 层，对齐 Kotlin analysis-api）**
- `:analysis:analysis-api` / `:analysis:analysis-api-platform-interface`
- `:analysis:analysis-api-impl-base`
- `:analysis:analysis-api-standalone`
- `:analysis:analysis-api-cfir`（含 generator）/ `:analysis:low-level-api-cfir`
- `:analysis:analysis-internal-utils` / `:analysis:cj-references`
- `:analysis:stubs` / `:analysis:decompiled`（含 `decompiler-to-file-stubs` / `decompiler-to-stubs` / `decompiler-to-psi` / `light-declarations-for-decompiled`）
- `:analysis:light-declarations` / `:analysis:symbol-light-declarations`
- `:analysis:analysis-tools`
- `:analysis:analysis-test-framework`

**LSP / 语言服务**
- `:lsp` — 基于 lsp4j 的 Language Server 框架，能力协商 / 文档同步 / 工作区状态 / 分析桥接接缝

**可选后端（CHIR / CodeGen / LLVM）**
- `:compiler:chir` — CHIR 数据模型与 pass 框架
- `:compiler:codegen` — CHIR → LLVM IR
- `:llvm-interop` 聚合 / `:llvm-interop:llvm-interop-api` / `:llvm-interop:llvm-interop-jni` — JVM 调 LLVM 的 API + JNI

**测试基建与套件**
- `:tests` / `:tests:test-infrastructure` — Kotlin 风格 Directive/TestServices/配置 DSL
- `:cfir:analysis-tests` — CFIR 分析测试套件

**Prepare / 发布工件**
- `:prepare:frontend` / `:prepare:frontend-embeddable`
- `:prepare:test-infrastructure` / `:prepare:analysis-test-framework`
- `:prepare:ide-plugin-dependencies:*` — 按功能分组的 fat jar（cangjie-frontend-{common,psi,cfir,analysis-api,analysis-api-cfir,analysis-api-standalone}-for-ide）
- `:prepare:ide-plugin-dependencies-module:*` — 与上述对应的 module 形态产物

**外部子项目（独立构建）**
- `intellij-ide/` — 基于 IntelliJ Platform Gradle Plugin 2.x 的仓颉 IDE 插件（独立 settings，不在主 `settings.gradle.kts` 内）
- `deveco/` — DevEco Studio 侧的仓颉增强插件，独立产品构建

## external/ 目录

外部参考源码，**不参与 Gradle 构建**：

- `external/cangjie_compiler` — 仓颉语言编译器源码（C++ 参考实现）
- `external/intellij-cangjie` — 基于 Kotlin K1 的 IntelliJ 仓颉插件
- `external/kotlin` — Kotlin 编译器源代码（K2 架构参考）

## 开发约定

- Kotlin/JVM，JDK 17
- 构建工具：Gradle + Kotlin DSL + Version Catalog
- 编译器选项：`-Xjvm-default=all`
- 测试框架：JUnit 5（JUnitPlatform）
- **中文注释优先**：注释使用中文，优先文档注释

- **接口优先**：所有独立模块和功能必须通过接口（interface）对外暴露高级抽象，实现细节不对外泄露。模块间依赖接口而非具体类，为未来扩展和替换实现留出空间
- **规范优先**：项目级开发规范见 `DEVELOPMENT_CONVENTIONS.md`，默认对一方模块强制生效。
  关键约束：可读性优先于炫技、一致性优先于个人习惯、明确优先于隐式、不可变优先于可变、接口隔离优先于大而全、领域建模优先于过程堆砌。
  工程约束：模块边界清晰、依赖方向单向、领域模型稳定、接口契约明确、测试层次完整、可观测性内建、工程治理自动化、变更可控且可回滚。

## Agent Runtime Notes
- Do not create any `.gradle-user-*` directory (for example: `.gradle-user-local`, `.gradle-user-fresh`, `.gradle-user-xxxx`).
- If Gradle cannot be executed for any reason, immediately notify the user.

## Cfir/K2 FIR Alignment

- Resolve framework code should mirror Kotlin K2 FIR structure as closely as practical, except where Cangjie language semantics force deviations.
- Keep alignment targets in priority:
  - folder hierarchy and package/module layout,
  - inheritance chains and processor layering,
  - class/type names and public method names (use `Cfir` prefix),
  - processing flow.
- If a Kotlin API has no direct Cangjie counterpart, add a corresponding Cfir API and document the deviation.
