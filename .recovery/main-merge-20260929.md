# 恢复分支合并核对

- 待整合的原主仓 main：`157b3c282b56a12b46e860305fa7055e2d6604e0`。
- 恢复分支：`codex/recover-uncommitted`，合并前 `367ee77cc`。
- 源码自动合并；引用 helper 同时保留组织感知的导入路径解析和 main 新增的 `toCaTargetSymbols` 过滤规则。
- `.idea/workspace.xml` 保留 main 较新的本机状态；`REPAIR_LOG.md` 保留双方的全部新增记录。
- 本轮定向验证在编译期间因 Gradle daemon 崩溃中断，同时队列日志报告磁盘空间不足；未形成合并后测试通过结论。之前恢复版本的 76 项结果仍仅代表合并前版本。
- 主目录在本轮开始后出现新的 CJO 反序列化未提交修改，与待合并文件重叠。没有暂存、移走或覆盖这些并行修改；主仓最终更新等待协调。

后续验证：减小 Gradle 堆并使用独立进程后，Analysis API 三项 suite 通过。CFIR 测试编译揭示恢复的 CJO 所有权测试直接读取 FlatBuffers 表但未声明其基础库；参照 compiler/frontend 的同类测试，在 cfir/analysis-tests 中补齐 testImplementation(libs.flatbuffers.java)。数值边界的 PSI/LightTree 用例及 CfirDeclarationOwnerTest 随后全部通过，逐用例结果见 merge-validation-20260929.json。

用户已明确选择等待另一个任务提交后再合并主仓。主目录中的并行 CJO 改动保持原状；恢复分支已准备好，主仓尚未快进。

独立 IDE 仓库已从 `681597be` 快进至 `8ea96a596a01d820bd40f65a65174426a621ff32`，分支为原 `cfir-new`。38 个既有修改路径在快进前逐一与恢复提交核对一致，并额外保存为 stash `87307164f4b089fae8cf4db5fa6e8e03fc4082c6`。快进后仅原有的两个 crash 日志保持未跟踪状态。没有推送远端。
