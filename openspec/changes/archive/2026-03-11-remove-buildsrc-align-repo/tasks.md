## 1. 创建 utilities 子模块

- [x] 1.1 创建 `repo/gradle-build-conventions/utilities/build.gradle.kts`，应用 `kotlin-dsl` 插件，设置 `group = "org.cangnova.cangjie.build"`
- [x] 1.2 创建 `repo/gradle-build-conventions/utilities/src/main/kotlin/sourceSets.kt`，从 `buildsrc-compat` 迁移源集 DSL 代码
- [x] 1.3 创建 `repo/gradle-build-conventions/utilities/src/main/kotlin/taskUtils.kt`，参考 Kotlin 实现 `getOrCreateTask` 工具函数
- [x] 1.4 创建 `repo/gradle-build-conventions/utilities/src/main/kotlin/testTaskUtils.kt`，参考 Kotlin 实现 `ideaHomePathForTests`、`totalMaxMemoryForTestsMb`
- [x] 1.5 创建 `repo/gradle-build-conventions/utilities/src/main/kotlin/gradleUtils.kt`，参考 Kotlin 实现通用配置/依赖工具函数
- [x] 1.6 创建 `repo/gradle-build-conventions/utilities/src/main/kotlin/JvmToolchain.kt`，适配仓颉项目的 JDK 版本配置
- [x] 1.7 创建 `repo/gradle-build-conventions/utilities/src/main/kotlin/capitalize.kt`，字符串首字母大写工具
- [x] 1.8 创建 `repo/gradle-build-conventions/utilities/src/main/kotlin/configurations.kt`，通用 Gradle 配置名常量和 `getOrCreateConfiguration` 函数

## 2. 更新 buildsrc-compat 依赖 utilities

- [x] 2.1 修改 `repo/gradle-build-conventions/buildsrc-compat/build.gradle.kts`，添加 `api(project(":utilities"))` 依赖
- [x] 2.2 删除 `repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/sourceSets.kt`（已迁移到 utilities）
- [x] 2.3 确认 `intellijDependencies.kt` 中引用的函数通过传递依赖仍可用

## 3. 创建 gradle-plugins-common 骨架

- [x] 3.1 创建 `repo/gradle-build-conventions/gradle-plugins-common/build.gradle.kts`，应用 `kotlin-dsl` 插件，设置 `group = "org.cangnova.cangjie.build"`，添加 `implementation(project(":utilities"))` 依赖
- [x] 3.2 创建空的 `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/` 目录（通过 `.gitkeep` 占位）

## 4. 更新 gradle-build-conventions settings

- [x] 4.1 修改 `repo/gradle-build-conventions/settings.gradle.kts`，添加 `include(":utilities")` 和 `include(":gradle-plugins-common")`

## 5. 创建 gradle-settings-conventions 组合构建

- [x] 5.1 创建 `repo/gradle-settings-conventions/settings.gradle.kts`，声明 `rootProject.name` 和 `include(":jvm-toolchain-provisioning")`
- [x] 5.2 创建 `repo/gradle-settings-conventions/build.gradle.kts`（最小化，仅声明 kotlin-dsl apply false 或为空）
- [x] 5.3 创建 `repo/gradle-settings-conventions/jvm-toolchain-provisioning/build.gradle.kts`，应用 `kotlin-dsl`，声明 `gradle-toolchains-foojay-resolver` 依赖
- [x] 5.4 创建 `repo/gradle-settings-conventions/jvm-toolchain-provisioning/src/main/kotlin/jvm-toolchain-provisioning.settings.gradle.kts`，配置 Foojay 工具链解析器

## 6. 更新根项目构建配置

- [x] 6.1 修改根 `settings.gradle.kts`，在 `pluginManagement` 块中添加 `includeBuild("repo/gradle-settings-conventions")`
- [x] 6.2 确认根 `settings.gradle.kts` 已有 `includeBuild("repo/gradle-build-conventions")`

## 7. 删除 buildSrc

- [x] 7.1 删除 `buildSrc/` 目录（源码、构建缓存、`.gradle/` 等全部内容）

## 8. 验证

- [ ] 8.1 执行 `./gradlew projects` 确认项目结构正确
- [ ] 8.2 执行 `./gradlew build` 验证全项目构建成功
- [ ] 8.3 确认 `psi`、`util`、`cfir/raw-cfir/psi2cfir` 模块的构建正常
