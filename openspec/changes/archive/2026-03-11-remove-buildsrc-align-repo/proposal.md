## 为什么

当前项目的构建逻辑全部放在 Gradle `buildSrc/` 目录中，存在固有缺陷：每次修改都会使整个项目的配置缓存失效，且无法被复用。Kotlin 编译器项目已将等效逻辑迁移到 `repo/` 下的组合构建模块（`gradle-build-conventions/` 和 `gradle-settings-conventions/`），通过 `includeBuild()` 引入，获得更好的构建缓存、模块化和可维护性。本项目应全面对齐这一架构，不仅迁移现有 buildSrc 代码，还要建立完整的 `repo/` 模块体系，为后续功能扩展（插件发布、工具链管理等）奠定基础。

## 变更内容

- **移除** `buildSrc/` 目录及其所有源码和构建产物
- **扩展** `repo/gradle-build-conventions/` 组合构建，新增 `utilities` 和 `gradle-plugins-common` 两个子模块
- **新增** `repo/gradle-settings-conventions/` 组合构建，包含 `jvm-toolchain-provisioning` 子模块
- **重组** 现有代码：将通用构建工具函数（`sourceSets.kt`）从 `buildsrc-compat` 下沉到 `utilities`，`buildsrc-compat` 改为依赖 `utilities`
- **修改** 根 `settings.gradle.kts`，同时引入两个组合构建

## 功能 (Capabilities)

### 新增功能

- `gradle-build-conventions`: 扩展现有组合构建，新增 `utilities`（通用 Gradle DSL 工具）和 `gradle-plugins-common`（插件发布基础设施骨架）两个子模块
- `gradle-settings-conventions`: 新增 settings 级组合构建，提供 `jvm-toolchain-provisioning`（JVM 工具链自动下载配置）

### 修改功能

（无需求层面的规范变更，仅实现层面的重组织）

## 影响

- **构建配置**：根 `settings.gradle.kts` 需引入两个组合构建
- **模块依赖链**：`utilities` → `buildsrc-compat` → `project-tests-convention`，需更新各模块 `build.gradle.kts` 的依赖声明
- **所有子模块**：依赖 `intellijCore()`、`projectDefault()` 等函数的模块需确认函数仍可用
- **Gradle 缓存**：迁移后 `buildSrc` 不再存在，构建缓存行为改善
- **CI/CD**：无破坏性变更，函数签名保持不变
