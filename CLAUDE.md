# Cangjie 编译器项目

## 项目定位

基于 Kotlin/JVM 的仓颉编程语言编译器实现，架构参考 Kotlin K2，功能对齐官方仓颉编译器（C++），覆盖从源码解析到代码生成的完整 12 阶段编译管线（详见 `cjfir-compiler-stages.md`）。

## 编译管线（12 阶段）

```
LOAD_PLUGINS → PARSE → CONDITION_COMPILE → IMPORT_PACKAGE → MACRO_EXPAND
→ CFIR_BUILD → CFIR_RESOLVE → FINALIZE → MANGLING → SAVE_CJO → CFIR2CHIR → CODEGEN
```

## 模块结构

当前已实现模块：
- `cfir` — CFIR 数据模型（类型系统、IR 树、访问者）

按编译器阶段规划的模块：
- `cfir-build` — 阶段 6: PSI/LightTree → Raw CFIR
- `cfir-resolve` — 阶段 7: 多 Phase 语义解析
- `cfir-serialization` — 阶段 10: .cjo 序列化
- `chir` — 阶段 11: CHIR 定义和 CFIR→CHIR 转换

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
- **接口优先**：所有独立模块和功能必须通过接口（interface）对外暴露高级抽象，实现细节不对外泄露。模块间依赖接口而非具体类，为未来扩展和替换实现留出空间