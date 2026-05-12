# prepare/ — 发布工件门面

将一方模块按用途**聚合为发布工件**的门面模块。每个门面模块本身不承载新逻辑，只做依赖聚合 + maven 坐标 + POM 元数据。

## 子模块

### 前端发布工件

| 子模块 | 工件坐标 | 内容 |
|---|---|---|
| `frontend` | `cangjie-frontend` | 完整前端（含 PSI / CFIR / Analysis API） |
| `frontend-embeddable` | `cangjie-frontend-embeddable` | 内嵌型前端（重定位依赖、减少冲突） |
| `test-infrastructure` | `cangjie-frontend-test-infrastructure` | 测试基建（`:tests:test-infrastructure`） |
| `analysis-test-framework` | `cangjie-frontend-analysis-test-framework` | Analysis 测试框架 |

### IDE 插件依赖（fat jar）

`ide-plugin-dependencies/*` 把上游能力按功能分组打包为 fat jar，对齐 Kotlin `prepare/ide-plugin-dependencies`：

- `cangjie-frontend-common-for-ide`
- `cangjie-frontend-psi-for-ide`
- `cangjie-frontend-cfir-for-ide`
- `cangjie-frontend-analysis-api-for-ide`
- `cangjie-frontend-analysis-api-cfir-for-ide`
- `cangjie-frontend-analysis-api-standalone-for-ide`

### IDE 插件依赖（module 形态）

`ide-plugin-dependencies-module/*` 与上述同名同结构，但产物为 IntelliJ 平台 module 形态（用于 Gradle Plugin 2.x 的 module dependencies）。

## 发布命令

```bash
./gradlew publishPublicArtifacts                # 发布到配置的 Maven 仓库
./gradlew installPublicArtifacts                # 安装到 Maven Local
```

注入发布目标与凭据：

```bash
./gradlew publishPublicArtifacts \
  -Pcangjie.build.deploy-url=https://maven.pkg.github.com/<OWNER>/<REPO> \
  -Pcangjie.build.deploy-username=<GITHUB_USERNAME> \
  -Pcangjie.build.deploy-password=<GITHUB_TOKEN>
```

仓库已内置 GitHub Packages workflow：`.github/workflows/publish-github-packages.yml`，支持 `workflow_dispatch` 与推送 `v*` tag 自动发布。

## IDE 子项目联动

`intellij-ide/` 通过 `includeBuild("../")` + dependency substitution 直接消费 `ide-plugin-dependencies-module:*` 的源码产物，**无需先发布工件**。详见 `../intellij-ide/PROJECT_STRUCTURE.md`。

## 相关文档

- `../README.md` 「发布」章节 — 发布命令与公开工件列表
- `../intellij-ide/CLAUDE.md` 「主仓库接入」 — IDE 子项目如何消费
