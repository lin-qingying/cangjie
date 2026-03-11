## 新增需求

### 需求:组合构建模块结构
`repo/gradle-build-conventions/` 必须是一个独立的 Gradle 组合构建项目，包含 `buildsrc-compat` 和 `project-tests-convention` 两个子模块。

#### 场景:目录结构完整
- **当** 查看 `repo/gradle-build-conventions/` 目录
- **那么** 必须存在 `settings.gradle.kts`、`buildsrc-compat/build.gradle.kts`、`project-tests-convention/build.gradle.kts`

#### 场景:组合构建可独立配置
- **当** `repo/gradle-build-conventions/settings.gradle.kts` 被 Gradle 解析
- **那么** 必须声明 `buildsrc-compat` 和 `project-tests-convention` 两个子项目

### 需求:buildsrc-compat 模块提供构建工具函数
`buildsrc-compat` 模块必须以 Kotlin 库形式导出 `intellijDependencies.kt` 和 `sourceSets.kt` 中的所有公共函数，签名与原 `buildSrc` 版本完全一致。

#### 场景:intellijCore 函数可用
- **当** 子模块的 `build.gradle.kts` 调用 `intellijCore()`
- **那么** 必须返回 `:dependencies:intellij-core` 项目依赖，行为与迁移前一致

#### 场景:projectDefault 函数可用
- **当** 子模块的 `build.gradle.kts` 在 `sourceSets` 块中调用 `projectDefault()`
- **那么** 必须按原有规则设置源目录（main→src/、test→test/+tests/）

#### 场景:buildsrc-compat 使用 kotlin-dsl 插件
- **当** 构建 `buildsrc-compat` 模块
- **那么** 其 `build.gradle.kts` 必须应用 `kotlin-dsl` 插件，且 group 为 `org.cangjie.build`

### 需求:project-tests-convention 插件保持功能不变
`project-tests-convention` 模块必须提供与原 `buildSrc` 版本相同的 Gradle 预编译脚本插件，包含 `ProjectTestsExtension` 和 `JUnitMode`。

#### 场景:插件可通过 ID 应用
- **当** 子模块的 `build.gradle.kts` 使用 `plugins { id("project-tests-convention") }`
- **那么** 必须成功应用插件并注册 `projectTests` 扩展

#### 场景:testTask 和 testGenerator 可用
- **当** 通过 `projectTests { }` DSL 调用 `testTask()` 或 `testGenerator()`
- **那么** 行为必须与迁移前完全一致

#### 场景:project-tests-convention 依赖 buildsrc-compat
- **当** 构建 `project-tests-convention` 模块
- **那么** 其 `build.gradle.kts` 必须声明对 `buildsrc-compat` 的 `implementation` 依赖

### 需求:根项目通过 includeBuild 引入构建约定
根项目的 `settings.gradle.kts` 必须通过 `pluginManagement { includeBuild(...) }` 引入 `repo/gradle-build-conventions`。

#### 场景:pluginManagement 包含 includeBuild
- **当** Gradle 解析根项目的 `settings.gradle.kts`
- **那么** `pluginManagement` 块中必须包含 `includeBuild("repo/gradle-build-conventions")`

#### 场景:buildSrc 目录不存在
- **当** 迁移完成后检查项目根目录
- **那么** `buildSrc/` 目录必须已被删除（包括源码和构建缓存）

### 需求:现有模块构建行为不变
迁移禁止改变任何现有模块的编译、测试或运行行为。

#### 场景:全项目构建成功
- **当** 执行 `./gradlew build`
- **那么** 所有模块必须编译通过，测试结果与迁移前一致

#### 场景:使用 buildSrc 函数的模块无需修改源码
- **当** 检查 `psi/build.gradle.kts`、`util/build.gradle.kts`、`cfir/raw-cfir/psi2cfir/build.gradle.kts`
- **那么** 这些文件中对 `intellijCore()`、`projectDefault()`、`testTask()` 等函数的调用禁止需要修改
