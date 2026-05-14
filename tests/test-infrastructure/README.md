# tests/test-infrastructure/ — Kotlin 风格测试基础设施

为编译器、CFIR、analysis、IDE 等模块提供共享测试基建，组织方式对齐 Kotlin K2 `compiler/test-infrastructure`。

源集采用 `testFixtures`（见根 `AGENTS.md` 第 8 节）。

## 关键包（testFixtures）

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.test.runners` | 测试 runner / facade（`AbstractCangjieCompilerTest`、handler 流水线） |
| `org.cangnova.cangjie.test.frontend` | 前端测试 facade（`CfirFrontendFacade` 等） |
| `org.cangnova.cangjie.test.config` | 测试配置 DSL |
| `org.cangnova.cangjie.test.directives` | Directive 处理（`// FILE:`、`// DIAGNOSTICS:` 等） |
| `org.cangnova.cangjie.test.builders` | TestServices / TestModule / TestFile builder |
| `org.cangnova.cangjie.test.model` | TestModule / TestFile 模型 |
| `org.cangnova.cangjie.test.codeMetaInfo` | 内联标记（diagnostics 位置 / 范围）渲染 |
| `org.cangnova.cangjie.test.impl` | 内部实现 |
| `com.intellij.testFramework` | 上游 IntelliJ 测试框架补丁层 |

## 使用方式

下游模块的测试通过 `testImplementation(testFixtures(project(":tests:test-infrastructure")))` 接入。

典型测试基类：

- `AbstractCangjieCompilerTest` — 编译器全管线测试入口
- 各具体 facade 测试基类（CFIR / analysis 等）由各模块继承

## 全项目测试约定

详见根 `TESTING_CONVENTIONS.md`：

- 文件驱动 / golden 比对 / directive 场景必须接入本模块
- 纯单元测试可使用直接 JUnit
- Golden 文件用 `-Dupdate.test.data=true` 显式更新

## 命令

```bash
./gradlew :tests:test-infrastructure:assemble
```

下游测试示例：

```bash
./gradlew :cfir:cfir-cones:test
./gradlew :cfir:analysis-tests:test
./gradlew :analysis:analysis-api-cfir:test
```

## 上游接入

本模块的 testFixtures 由 `:prepare:test-infrastructure` 聚合为发布工件 `cangjie-frontend-test-infrastructure`。

## 相关文档

- `../../TESTING_CONVENTIONS.md` — 全项目测试约定（强制）
- `../../AGENTS.md` 第 7、8 节 — 测试命令与 source-set 约定
