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

- **`.cj.d` 声明文件支持**：设计三稿并存，**以 `docs/cjd-declaration-file-support-v3.md`（v3.2，4527+ 行，含 4.2.8 / 4.3 P3 / 4.5.2 P2 / 4.8.3 P6 / 5.1 P4 各实施记录 + 附录 C.6/C.7）为准**；
  动手前先读 `docs/cjd-declaration-file-support-v3-review.md`（复核报告，R1–R14 与 13 处官方取证全部成立）。
  核心取向：**框架正确性优先于改动最小化**，允许必要的破坏性改动。

  **进度（2026-09-18 收尾）**：**P0–P4 全部完成且已提交**，
  P5（IDE 侧）已在 `intellij-ide` / `deveco` 两仓提交，P6 已复核（LSP 项无落点，见 4.8.3）。
  三个仓的提交见 v3 文档 **附录 C.7**（5 笔，含 2 处披露）。
  仍缺：**6.6 的 6 项人工 IDE 验收**、`deveco` 的 `cjdFiles` 生产接线（现只有测试消费）。

  **判断进度时用 `git grep <符号> HEAD`，不要依赖记忆或日志** ——
  P1/P2/P3 早于本记录就已随其它提交落地（`reportMissingBody` / `sourceKind.isDeclaration` /
  `requiresImplementation` 都在 HEAD 里）。

  **实测基线（真实 SDK 语料，38 个 `.cj.d`）**：APILevel 6,497 个，
  已合并 5,031 + 匹配失败 140 + 官方顶层门控 1,326（**20.4% 结构上不可达，已登记为已知限制**）。
  语料盲区：`TYPE_ALIAS`/`MACRO` 注解数为 0、`EXTEND` 372 个仅 1 个带注解、无 `FINALIZER`/`MAIN`。

  **复现入口**：`CANGJIE_CJD_SDK_DIR=<DevEco 插件内的 std 目录>`，
  能跑通 build-tools/modules/linux_ohos_aarch64_cjnative/std（38 个 `.cj.d` + 46 个 `.cjo`）。
  两个验收测试：`CjdSdkDeclarationBoundaryAuditTest`（无静默丢失审计）、
  `CjdSidecarAvailabilityDiagnosticTest`（引用点 availability 端到端）。

### 跑 Gradle 测试的可靠姿势（本仓专用，省时间）

- **`gradlew-queue.bat` 无法从 Git Bash 用 `cmd //c` 调用**（会退化成交互式 cmd 直接退出）。
  等价且可用：`java -jar gradle-queue-cli/build/libs/gradle-queue-cli.jar --project-dir 'D:\code\intellij\cangjie' <任务> --console=plain`。
- 环境变量（如 `CANGJIE_CJD_SDK_DIR`）能透传到 test JVM；**一次调用可同时跑多个模块**，
  但**必须加 `--continue`**，否则前一个任务失败会让后面根本不执行：
  `... :a:test --tests '*P1*' :b:test --tests '*P2*' --continue --console=plain`
- 测试里的 `println` 不进控制台，在 `build/test-results/test/TEST-*.xml` 的 `<system-out>` 里；
  用 `sed -e 's/&lt;/</g; s/&gt;/>/g'` 解转义后 grep。
- 首轮编译 3.5–7 分钟，之后 1–3 分钟。落盘 `> /tmp/x.log 2>&1` 再 `grep -E "^e: |FAILED|BUILD"`。

