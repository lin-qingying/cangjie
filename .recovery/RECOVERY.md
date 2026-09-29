# 未提交内容恢复记录

- 原工作目录 `D:/code/intellij/cangjie` 只读；没有在其中执行恢复、切换分支或构建。
- 恢复工作区：`C:/Users/lin17/.codex/worktrees/recover-uncommitted/cangjie`。
- 恢复分支：`codex/recover-uncommitted`；固定基线 `737514ba7e37ffd9e431afc2cc4bfcfb4c933ddb`。
- 范围：import PSI 重构主会话及三个子代、CJMP `/goal cjmp-framework-review-and-fix-plan-20260928.md` 主会话及三个子代。
- 截止：`2026-09-29T00:54:10Z`，只使用此前的成功文件变更事件。

## 已验证的阶段

`events.cjs` 提取已完成的 FileChange 记录，保留动态补丁、成功的部分调用、Add 内容及 Update unified_diff；不执行历史 shell。`snapshots.cjs` 补入有历史原文证明的 dirty 基线。

184 个源码、测试与文档文件已按成功 FileChange、原文快照或具有 Git blob 哈希的历史 diff 物化；稀疏上下文按历史行号定位。LF 规范化后 SHA-256 全部与重放结果一致。逐文件来源见 `verified-files.json`。

另外按 reset 前的 git status 恢复删除了 6 个旧 Analysis API reference-shortener tests-gen 文件。恢复后的 CFIR tree generator 生成 5 个 import 语义字段变化，与历史变更范围一致。

独立 IDE 副本的 6 个 import 文件已恢复并提交 `0fc0c73107f961eb2fc6ca2466b4d46bcd908e94`；另保留原 IDE 子仓现存的其余文本工作状态，提交 `8ea96a596a01d820bd40f65a65174426a621ff32`。原子仓未修改。134 个会话 scratch 文件已按原始 UTF-8 字节校验，另恢复 1 个更早的 private-function 探针；见 `scratch-recovery-report.json`、`cross-recovery-report.json`。

新工作区执行 `gradlew-queue.bat` 的新结果（2026-09-29）：PSI import 21/21、common 路径组合 9/9、双源 raw import/reexport 11/11、组织绑定/可见性 5/5、Analysis API 导入引用/目录导航 5/5。相关 compiler/raw/provider、Analysis API 和低层 API 模块编译通过。

独立 IDE 的最终新结果：折叠 4/4、产品导入导航 1/1，构建成功。主仓 71 个定向测试加独立 IDE 5 个，共 76 个通过；这不是全仓全量测试的声明。结果与 testcase-key 见 `fresh-test-results.json`、`fresh-ide-test-results.json`。

此前独立 IDE 首轮因 VFS 初始化时磁盘空间不足失败，重试发现缺少原 IDE 子仓保留的工作状态。补齐该状态后最终两项 suite 全部通过。清理失败缓存的工具调用被自动审批以 `blocked by policy` 拒绝；没有执行清理，也没有换工具绕过拒绝，采用直接重试和补齐已保留文件完成验证。

22 份旧日志没有保留全文的 golden 已由恢复后的实现重新生成，20 个相关低层测试随后在 `update.test.data=false` 模式再次通过。另 1 份 interfaceStatus golden 从历史 Git diff 原样恢复。LocalHistory 对 class.txt、declarations.txt 的真实旧正文与重新生成结果完全一致，其余重新生成文件不宣称获得逐字节历史快照。CFIR tests-gen 已重新生成，补回 9 个新增 CJMP fixture 在三个测试入口中的索引。

LocalHistory 的 ContentChange/DeleteChange 已解码到 VFS 内容记录，正文经过 VFS SHA-1 校验。找回 CjoStubAstConsistencyDiagnosticTest.kt（16,678 字节、原始 SHA-256 完全一致）以及 compiler.xml、kotlinc.xml、project dictionary、Run All Test 四份配置的重置前内容。后两份配置保留了换行规范化后的相同文本，其原始字节以 Base64 附在 `localhistory-recovered.json`。前两份配置本就在恢复基线中，与真实历史字节相同。

不能宣称所有磁盘文件逐字节完整恢复：`.idea/workspace.xml` 被 IntelliJ LocalHistory 明确排除，未找到可信的重置前快照；旧探针的少量临时可执行文件/编译缓存没有可验证正文。它们不被伪造为已恢复源码。新工作区自身生成的 `.idea/workspace.xml`、`.idea/vcs.xml` 属于本机工作状态，未作为历史恢复提交。

`CjImportDirectiveItem.kt` 的末尾已按历史整文件替换的 trimEnd 语义恢复；CfirImportBindingResolver 的原 BOM 也已保留。当前差异通过 `git diff --check`。
