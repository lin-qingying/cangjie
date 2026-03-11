## 上下文

当前项目的所有构建约定代码位于 `buildSrc/`。Kotlin 编译器项目的 `repo/` 目录包含两个独立的组合构建：

1. **`gradle-build-conventions/`** — 构建级约定插件和工具库，包含 15 个子模块（`utilities`、`buildsrc-compat`、`project-tests-convention`、`gradle-plugins-common`、`generators` 等）
2. **`gradle-settings-conventions/`** — settings 级约定插件，包含 6 个子模块（`jvm-toolchain-provisioning`、`cache-redirector`、`develocity` 等）

本项目已完成 `buildsrc-compat` 和 `project-tests-convention` 两个子模块的初步迁移（代码已在 `repo/gradle-build-conventions/` 下），但 `buildSrc/` 尚未删除，且缺少 `utilities`、`gradle-plugins-common`、`gradle-settings-conventions` 等模块。

## 目标 / 非目标

**目标：**
- 在 `repo/gradle-build-conventions/` 下新增 `utilities` 子模块，放置通用 Gradle DSL 工具函数
- 在 `repo/gradle-build-conventions/` 下新增 `gradle-plugins-common` 子模块骨架，为未来插件发布做准备
- 新增 `repo/gradle-settings-conventions/` 组合构建，包含 `jvm-toolchain-provisioning` 子模块
- 将 `sourceSets.kt` 从 `buildsrc-compat` 迁移到 `utilities`，`buildsrc-compat` 依赖 `utilities`
- 删除 `buildSrc/` 目录
- 保持所有现有模块的构建行为不变

**非目标：**
- 不迁移 Kotlin 特有的子模块（`generators`、`prepare-deps`、`android-sdk-provisioner`、`asm-deprecating-transformer`、`binaryen-configuration`、`nodejs-configuration`、`d8-configuration` 等）
- 不迁移 Kotlin 特有的 settings 约定（`cache-redirector`、`kotlin-bootstrap`、`develocity`、`kotlin-daemon-config`）
- 不实现 `gradle-plugins-common` 的完整功能（仅骨架）
- 不修改 Version Catalog（`libs.versions.toml`）结构
- 不调整现有子模块的源集布局或测试配置

## 决策

### D1: 模块体系——5 个核心模块

**选择**：建立以下模块体系：

```
repo/
├── gradle-build-conventions/        （组合构建 1）
│   ├── utilities/                   （通用 Gradle DSL 工具）
│   ├── buildsrc-compat/             （项目特定构建工具）
│   ├── gradle-plugins-common/       （插件发布基础设施骨架）
│   └── project-tests-convention/    （测试约定插件）
└── gradle-settings-conventions/     （组合构建 2）
    └── jvm-toolchain-provisioning/  （JVM 工具链配置）
```

**替代方案**：
- 仅保留现有 2 个模块 → 缺少通用工具层，后续功能无处放置
- 完全复制 Kotlin 的 15+ 子模块 → 大部分无用，过度工程化

**理由**：保留 Kotlin K2 的分层架构（utilities → buildsrc-compat → project-tests-convention），同时只引入当前和近期需要的模块。`gradle-plugins-common` 作为骨架为未来 cjo 包发布做准备。

### D2: utilities 模块内容

**选择**：从 Kotlin 的 `utilities` 中选取通用、非 Kotlin 特有的内容：

| 文件 | 保留 | 理由 |
|------|------|------|
| `sourceSets.kt` | 是 | 从 buildsrc-compat 迁移过来，通用源集 DSL |
| `taskUtils.kt` | 是 | getOrCreateTask 通用工具 |
| `testTaskUtils.kt` | 是 | ideaHomePathForTests、内存计算 |
| `gradleUtils.kt` | 是 | 通用配置/依赖工具函数 |
| `JvmToolchain.kt` | 是 | JDK 版本配置（适配仓颉项目） |
| `capitalize.kt` | 是 | 简单字符串工具 |
| `configurations.kt` | 部分 | 仅保留通用配置名常量和 getOrCreateConfiguration |
| `repoDependencies.kt` | 否 | 高度 Kotlin 特有 |
| `ideaExtKotlinDsl.kt` | 否 | IntelliJ IDEA IDE 配置，非编译器需要 |
| `BuildPropertiesExt.kt` | 否 | 高度 Kotlin 特有 |
| `generatorTask.kt` | 暂缓 | 测试生成器框架，后续需要时再引入 |
| `AbsolutePathArgumentProvider.kt` | 暂缓 | 后续需要时再引入 |
| `SystemPropertyClasspathProvider.kt` | 暂缓 | 后续需要时再引入 |

### D3: gradle-plugins-common 骨架内容

**选择**：仅创建模块骨架（`build.gradle.kts` + 空的 `src/main/kotlin/`），不实现具体功能。

**理由**：当前项目不需要插件发布，但保留模块占位符让后续扩展更自然。骨架零成本。

### D4: gradle-settings-conventions 的 jvm-toolchain-provisioning

**选择**：创建 settings 级组合构建，包含 `jvm-toolchain-provisioning` 子模块，使用 Foojay 解析器实现 JDK 自动下载。

**替代方案**：
- 不创建 settings 约定 → 每个开发者手动管理 JDK 版本
- 在 gradle-build-conventions 中配置 → 违反 Gradle 的 settings/build 分层

**理由**：JVM 工具链是跨项目通用的基础设施。Foojay 解析器是 Gradle 官方推荐的 JDK 自动下载方案。settings 级插件是配置工具链的正确位置。

### D5: 依赖关系链

```
gradle-settings-conventions/
  └── jvm-toolchain-provisioning    （独立，无内部依赖）

gradle-build-conventions/
  ├── utilities                     （无依赖，基础层）
  ├── buildsrc-compat               （依赖 utilities）
  ├── gradle-plugins-common         （依赖 utilities）
  └── project-tests-convention      （依赖 buildsrc-compat）
```

### D6: 引入方式

**选择**：根 `settings.gradle.kts` 中：

```kotlin
pluginManagement {
    includeBuild("repo/gradle-build-conventions")
    includeBuild("repo/gradle-settings-conventions")
}
```

## 风险 / 权衡

- **[风险] sourceSets.kt 迁移到 utilities 后函数可见性变化** → 缓解：`buildsrc-compat` 声明 `api(project(":utilities"))`，确保传递依赖可见
- **[风险] Foojay 解析器需要网络** → 缓解：仅在未配置本地 JDK 时触发下载，有本地 JDK 时无影响
- **[权衡] 多了两个 settings.gradle.kts** → 组合构建的要求，增加配置文件换来模块化和缓存隔离
- **[权衡] gradle-plugins-common 暂时为空骨架** → 零运行时成本，占位符为后续扩展降低摩擦
