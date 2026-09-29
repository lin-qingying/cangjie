# 未提交内容恢复记录

- 原工作目录 `D:/code/intellij/cangjie` 只读；没有在其中执行恢复、切换分支或构建。
- 恢复工作区：`C:/Users/lin17/.codex/worktrees/recover-uncommitted/cangjie`。
- 恢复分支：`codex/recover-uncommitted`；固定基线 `737514ba7e37ffd9e431afc2cc4bfcfb4c933ddb`。
- 范围：import PSI 重构主会话及三个子代、CJMP `/goal cjmp-framework-review-and-fix-plan-20260928.md` 主会话及三个子代。
- 截止：`2026-09-29T00:54:10Z`，只使用此前的成功文件变更事件。

## 已验证的阶段

`events.cjs` 提取已完成的 FileChange 记录，保留动态补丁、成功的部分调用、Add 内容及 Update unified_diff；不执行历史 shell。`snapshots.cjs` 补入有历史原文证明的 dirty 基线。

182 个源码、测试与文档文件已物化；已修正稀疏上下文需要按历史行号定位的问题。LF 规范化后 SHA-256 全部与重放结果一致。逐文件来源见 `verified-files.json`。

另外按 reset 前的 git status 恢复删除了 6 个旧 Analysis API reference-shortener tests-gen 文件。恢复后的 CFIR tree generator 生成 5 个 import 语义字段变化，与历史变更范围一致。

独立 IDE 副本的 6 个 import 文件已恢复并提交 `0fc0c73107f961eb2fc6ca2466b4d46bcd908e94`，原子仓未修改。134 个会话 scratch 文件已按原始 UTF-8 字节校验；见 `scratch-recovery-report.json`。

新工作区执行 `gradlew-queue.bat` 的 PSI import 定向测试 21/21、common 路径组合测试 9/9 通过，相关 compiler/raw/provider 构建通过（2026-09-29）。双源 raw import 与 Analysis API 引用测试正在继续执行。

尚未宣称所有未提交内容完整恢复：原有 dirty golden 自动更新、IDE 配置及少数没有 FileChange 原文的临时产物仍在取证。

`git diff --check` 报告 CjImportDirectiveItem.kt 文件末尾有一个历史空行；本阶段保留恢复原文。
