# AGENTS.md

本文件是本仓库中编码代理（coding agent）的操作指南。
除非用户有明确的覆盖指令，否则应将其作为默认执行手册。

## 1) 仓库背景

- 项目：基于 Kotlin/JVM 的仓颉（Cangjie）编译器实现。
- 架构：受 Kotlin K2 启发的多阶段编译流水线。
- Java 工具链：JDK 17。
- 构建系统：使用 Kotlin DSL 的 Gradle。
- 编译器共享选项：`-Xjvm-default=all`。
- 测试：全局启用 JUnit Platform；部分模块仍保留 JUnit 4 依赖。

主要文档：
- `CLAUDE.md`
- `README.md`
- `cjfir-compiler-stages.md`

## 2) 规则文件检查（Cursor/Copilot）

在仓库根目录检查结果：
- `.cursor/rules/` -> 不存在
- `.cursorrules` -> 不存在
- `.github/copilot-instructions.md` -> 不存在

因此，项目行为主要由源码约定 + Gradle 脚本 + `CLAUDE.md` 共同约束。

## 3) 项目边界

- `external/` 包含参考实现和大型上游镜像。
- 除非任务明确要求，否则将 `external/` 视为只读。
- 默认修改范围：`settings.gradle.kts` 中包含的一方模块（first-party modules）。

## 4) 重要模块

- 基础设施：`common`、`util`、`dependencies:intellij-core`
- PSI：`psi`
- CFIR：
  - `cfir:cfir-common`
  - `cfir:cfir-cones`
  - `cfir:cfir-tree`
  - `cfir:raw-cfir:*`
- Analysis：
  - `analysis:analysis-api`
  - `analysis:analysis-api-impl-base`
  - `analysis:analysis-api-cfir`
  - `analysis:analysis-test-framework`
- CLI：`compiler:cli`
- 测试支撑：`tests:test-infrastructure`

## 5) 构建命令

请在仓库根目录执行。
Windows 使用 `gradlew.bat`；Unix/macOS 使用 `./gradlew`。

全仓库构建：
```bash
./gradlew clean
./gradlew assemble
./gradlew build
./gradlew check
./gradlew test
```

按模块示例：
```bash
./gradlew :cfir:cfir-cones:assemble
./gradlew :cfir:cfir-tree:build
./gradlew :analysis:analysis-api-cfir:test
./gradlew :compiler:cli:build
```

## 6) Lint / 静态检查

- 在一方模块中未发现独立的顶层 ktlint/detekt/spotless 配置。
- 使用 `./gradlew check` 作为聚合验证入口。
- 即使 `check` 通过，也应为变更模块执行有针对性的测试。

## 7) 测试命令（强调单测粒度）

运行全部测试：
```bash
./gradlew test
```

运行单个模块：
```bash
./gradlew :cfir:cfir-cones:test
./gradlew :cfir:raw-cfir:psi2cfir:test
```

运行单个测试类：
```bash
./gradlew :cfir:cfir-cones:test --tests "org.cangjie.cfir.types.PrimitiveTypeKindTest"
```

运行单个测试方法：
```bash
./gradlew :cfir:cfir-cones:test --tests "org.cangjie.cfir.types.PrimitiveTypeKindTest.typeName matches expected strings"
```

模式匹配：
```bash
./gradlew :cfir:cfir-cones:test --tests "*PrimitiveTypeKindTest"
```

排障命令：
```bash
./gradlew :<module>:test --tests "<pattern>" --info --stacktrace
```

说明：`:cfir:raw-cfir:psi2cfir` 增加了测试 include-pattern 处理，选择外部测试类时可自动包含内部类。

## 8) Source-set 目录约定

由 `buildSrc/src/main/kotlin/sourceSets.kt`（`projectDefault()`）定义：
- `main` -> `src`、`resources`
- `test` -> `test`、`tests`、`testResources`
- `testFixtures` -> `testFixtures`、`testFixturesResources`

新增模块时请遵循该布局，不要自行发明自定义路径。

## 9) 代码风格指南

格式：
- Kotlin 标准风格，4 空格缩进。
- 现有尾随逗号（trailing comma）风格在原处保持一致。
- 简单逻辑优先使用表达式函数体（expression-bodied functions）。

导入：
- 优先遵循本地文件/模块既有约定。
- 在模型密集区域已存在星号导入；无明确收益时不要为此进行无意义改动。
- 删除未使用导入。
- 跨包 API 使用优先显式导入。

类型与 API：
- 优先使用非空类型；仅在表达真实缺失语义时使用可空类型。
- 除非确有可变需求，否则暴露不可变/只读 API 类型（如 `List`）。
- 对 public/protected API，优先显式返回类型。
- 尊重模块边界；依赖抽象而非具体实现细节。

命名：
- 类型/接口/对象：`PascalCase`
- 函数/属性/局部变量：`camelCase`
- 常量/枚举项：`UPPER_SNAKE_CASE`
- 保持既有编译器前缀（`Cfir*`、`Cone*`）。
- 测试类以 `Test` 或 `TestCase` 结尾。

错误处理与诊断：
- 正常流程中优先使用结构化编译器错误表示，而非通用异常。
- 复用既有错误实体（`ConeErrorType`、`CfirErrorExpression`、`CfirErrorReference`）。
- 使用 `require`/`check` 表达不变量与编程错误。
- 在适用场景下，保持诊断位于 collector/reporter 流水线中。

注释与文档：
- 仅为非显而易见逻辑添加注释。
- 对公共抽象与核心编译器结构保留 KDoc。
- 注释保持精确，避免叙事性且易过期的内容。

## 10) 架构约束（重要）

来自 `CLAUDE.md`：
- 独立模块/特性要求采用接口优先（interface-first）设计。
- 通过接口暴露高层抽象。
- 避免跨模块泄露实现细节。
- 设计应支持可扩展与可替换实现。

## 11) 代理工作流建议

- 在满足需求前提下，做最小且安全的改动。
- 先做受影响模块构建 + 定向测试，再决定是否扩大验证范围。
- 若跳过完整验证，需明确说明已执行内容及原因。
- 不要在 feature/bugfix 提交中混入无关重构。

## 12) 快速命令模板

```bash
./gradlew :<module>:assemble
./gradlew :<module>:test --tests "com.example.YourTest"
./gradlew :<module>:test --tests "com.example.YourTest.method name"
./gradlew check
```
