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

## LLT / fixture 修复纪律（用户明确要求）

- **不得为了让失败用例变绿而改写原本通过的期望。** 若语料内部自相矛盾
  （同一诊断、同一构造，两个 fixture 期望不同范围），**先停下问用户**，不要替用户选一个口径。
- **范围（range）改动的唯一证据是 `cjc` 实测 + `external/cangjie_compiler`**，
  不能靠"Policy 应该怎么读"推理出来。Diagnostic Range Policy 的实际含义是
  "把 cjc 的**首字符**锚点展开成**完整 token**"（`func abc` 的 `a` → `abc`）；
  对 `obj.foo` 这类，cjc 锚在成员名 `foo`，展开后仍是 `foo`，**不是** `obj.foo`。
  只有锚点本身不是单个 token 的构造（如 `extend R <: I {`）才去参考 Kotlin 对位的范围选择。
- 改动前先做**同族全量扫描**（grep 诊断名），确认影响面；只改触发调查的那一个 = 局部补丁。
- **并行会话**（另一个 agent 窗口）会同时改同一批文件并抢 Gradle 全局锁。
  开跑前 `find <dirs> -newermt "-5 minutes" -not -path "*/build/*"` 看有没有他人在写；
  若在自己要改的文件上，先问用户是否继续。历史教训：fixture 被并行会话批量回滚过。

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

### 改仓库内 Kotlin 源文件时的硬性注意（踩过坑）

- **不要用 `io.open(p,'w')` 脚本改写**：Windows 上会把 LF 转 CRLF，结果 `git status` 显示 ` M`
  但 `git diff` / `git diff --numstat` 全为空（EOL 被 git 归一化），极易误判"改坏/没改"。
  要脚本改写就传 `newline=''`；能用 Edit 工具就不要用脚本。
- **对非自己本轮新建的文件，禁用 `git checkout -- <file>` 清探针**：
  并行会话会在同一棵树上改文件（本仓已多次发生）。正确做法是**用 Edit 精确删除自己加的探针行**，
  只在 `git diff HEAD --numstat <file>` 确认内容等于 HEAD 时才考虑 checkout。
- 行内探针取变量身份：用 `System.identityHashCode(variable)`；`variable.typeConstructor()`
  在 `resolution.common` 的注入器/存储作用域里需要 `TypeSystemContext` receiver
  （写成 `with(c) { ... }`），`position.from` 在 `ConstraintInjector.addNewIncorporatedConstraint`
  作用域不可用，会编译报"receiver type mismatch"。
- 探针输出通道：`System.err.println` 在 `cfir:*` 与 `resolution.common` 都能被测试 XML
  的 `<system-err>` 采集；**但每个 run 会覆盖** `build/test-results/test/TEST-*.xml`，
  有价值的输出必须当轮落盘到 `/tmp/*.log` 或本地文件，不要指望下一轮再读。

### 已有结论可直接复用（别重复挖）

- `testOptionWithElement01`（`testData/llt/constraint_check/option_with_element_01.cj`）：
  **已修复并提交（2026-09-21 晚，`335281088` + `76d04a8ae`）**。根因两级：
  ① `ConstraintIncorporator.directWithVariable` 把「下界 `Int64 <: T`」×「声明上界
  `T <: Equatable<T>`」派生 `Int64 <: Equatable<T>`，其不变实参检查反手写 `T <: Int64`；
  ② 第二类合并把固定等值 `T == Option<Int64>` 代入自引用上界重录
  `T <: Equatable<Option<Int64>>`，再与历史下界配对硬检失败。修法 = **声明上界约束一律
  不参与合并派生**（directWithVariable 配对 + 第二类替换目标两处 skip，1 文件 +15 行）；
  前置 = `ConeInferenceContext.singleBestRepresentative` 的 core Option 代表选择。
  官方依据：cjc 0 诊断；LTAS 正常路径按下界 join 校验（:377-400），等价仅 deterministic 路径。
  验证：ConstraintCheck+Varray 双切片 172/0 全绿。
  **归因已闭环（2026-09-21 晚，`320f5b50c`）**：A/B 对照证实第一刀整类排除引入
  9 unique（×2）误报回归；精确化为"仅排除**自引用**声明上界"（freshTypeConstructor
  身份比较，hoist 出 lambda）后 8/9 转绿，全量门禁 **8658/114**（-16 恰为回归量）。
  **残留 1 已知漏报已修复（2026-09-21 晚·续 2，`d111b16e4`）**：ResultTypeResolver 解出后
  对自引用声明上界补位校验 `T_sol <: B(T_sol)`（官方 LTAS 交叉校验对位），失败走
  SOLVER_FAILURE_MARKER 统一 UNABLE_TO_INFER 路径。触发判据五项缺一不可（探针实证
  f_bounded_2 解出终态）：排除派生传播（derivedFrom 非空）、FixVariable、
  DeclaredUpperBound 升格等值、显式实参、纯上界解出；Option 包装解出经解包重试对齐
  allowOptionBox。全量门禁 **8658/112**（-2 恰为漏报消除量，零新增）。
  详见 REPAIR_LOG 2026-09-21（晚·续 2）节。
  用户要求：**不用独立 worktree**，A/B 切换用字节级备份/恢复；同文件多处 Edit 必须串行。

## cjc 取证基线口径（2026-09-21 起，长期有效）

- **官方行为判断必须双 SDK 对照**：本机 `/c/Users/lin17/.cangjie/sdks/` 有 0.53.13 / 0.53.18 / **1.0.0** / 1.0.5 / 1.1.3。
  pinned 镜像 `external/cangjie_compiler` 是 **v1.0.0**（`b776b44`），而历史探针一直用 1.0.5——
  它比镜像新（如 `sema_export_same_private_decl` 只在 1.0.5+ 存在，上游注释自述为 PrivateDecl.ti 的临时 workaround）。
  1.0.0 与 1.0.5 解析期行为逐项一致，差异项一律回镜像源码定位引入提交再裁决。
- 跑 1.0.0 必须覆盖 `CANGJIE_HOME`（默认指向 1.0.5；两者 modules 目录不同：
  1.0.0=`windows_x86_64_llvm`，1.0.5=`windows_x86_64_cjnative`）；输出用 `-o <dir>` 定向，
  否则产物落仓库根。cjc 是原生 Windows 程序：输入路径要 `cygpath -w`，**不要 `cd` 进 /tmp 再跑**
  （沙箱会 SIGTERM 整个命令），在 workspace cwd 下用绝对路径执行。

