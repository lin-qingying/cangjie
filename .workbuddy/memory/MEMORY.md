# 项目长期记忆：cangjie（仓颉编译器 Kotlin/JVM 实现）

> **详细架构事实见同目录 `ARCHITECTURE-FACTS.md`**（解析器 / CFIR / checker / 编译驱动 / 跨仓边界 / 测试落点 / 设计文档）。
> 本文件只放"每次会话都必须知道"的内容，保持精简。

## 环境（每次会话都适用）

- **Bash 必须加 PATH 前缀**：`export PATH="/usr/bin:/bin:$PATH"; <命令>`。
  否则 `ls`/`grep`/`find`/`wc`/`head` 全部 `command not found`（shim 破坏）。
- **PowerShell 工具在本工作区不返回 stdout**；探测文件存在性请用 Read / Glob。
- **仓库极大（10 万+ 文件）**：`Glob` 全仓搜索会超时。一律用 `Grep` 并**显式指定子目录 path**
  （`psi`、`cfir/raw-cfir`、`intellij-ide/modules`…）；跨模块搜索拆成多个窄路径并行发。
  搜索排除 `build/`、`bin/`、`tests-gen/`、`docs/cangjie-tutorial/node_modules/`。
- JDK：`JAVA_HOME=C:\Users\lin17\.jdks\corretto-21.0.9`（`javap` 等工具在该 `bin/` 下）。

## 仓库结构要点

- 主构建：`settings.gradle.kts` 内的一方模块；`external/` 是**只读上游镜像**（`cangjie_compiler` C++ 官方实现、
  `cangjie_deveco_plugins` 官方 DevEco 插件），**只作语义取证，绝不修改**。
- 子项目 `intellij-ide/`（IntelliJ 插件）、`deveco/`（DevEco 增强插件）各自独立 Gradle 构建。
- **`psi` 是前端模块，打成 fat jar 供 IDE 使用**：`org.cangnova.cangjie.lang` / `parsing` 的实现在 `psi/`，
  `intellij-ide` 只放 XML 注册；`psi/resources` 里只有 `messages/*.properties`（无 META-INF），
  FileType 注册 XML 在 `intellij-ide/modules/ide/base/src/main/resources/META-INF/cangjie-filetypes.xml`。
- 同名文件可能有 `intellij-ide` / `deveco` 两份副本（如 `CaIdeScopeCangJieFileCollector.kt`），改动需同步。
- 并发跑 Gradle 一律用 `gradlew-queue.bat`（`AGENTS.md` §4.1），不要直连 `gradlew.bat`。

## 当前进行中的特性

- **`.cj.d` 声明文件支持**：设计三稿并存，**以 `docs/cjd-declaration-file-support-v3.md`（v3.1，4299 行，含 P1 实施记录 4.2.8）为准**；
  动手前先读 `docs/cjd-declaration-file-support-v3-review.md`（复核报告，R1–R14 与 13 处官方取证全部成立）。
  实施阶段 P0–P6 见 v3 文档 5.1；核心取向：**框架正确性优先于改动最小化**，允许必要的破坏性改动。
  进度：**P0 已完成**（3 个提交：`fbce2151e` common 契约 / `f6ec382d2` `CjFile.sourceKind` / `470d86192` `-d` 配置；
  注意 `CangJieDeclarationFileType` 也在 P0 交付）、**P1 已完成但未提交**（psi 声明解析模式，
  25/25 测试通过；**提交被工作树的 311 文件无关 WIP 阻塞**，见当日日志）、
  **P2 已完成但未提交**（CFIR 护栏 1/2，10/10 通过；同样被 WIP 阻塞，见当日日志）。
  后续 P3（源收集 + checker 标记）、P4（sidecar 合并，风险最高，含准入条件 C-3/C-4）、P5（IDE 侧）、P6（声明注入）。
