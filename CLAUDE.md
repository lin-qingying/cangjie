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

当前已实现模块：
- `cfir` — CFIR 数据模型（类型系统、IR 树、访问者）
- `macro` — 宏展开模块（收集、执行、替换）

按编译器阶段规划的模块：
- `cfir-build` — 阶段 6: PSI/LightTree → Raw CFIR
- `cfir-resolve` — 阶段 7: 多 Phase 语义解析
- `cfir-serialization` — 阶段 10: .cjo 序列化
- `chir` — 阶段 11: CHIR 定义和 CFIR→CHIR 转换（可选扩展）
- `codegen` — 阶段 12: CHIR → LLVM IR → 机器码（可选扩展）

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
