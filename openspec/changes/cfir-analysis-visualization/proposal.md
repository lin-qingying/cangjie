## 为什么

开发者需要直接查看 CFIR resolve 各阶段的树状态与 diagnostics。采集必须是单独的 IDE 工具：普通编辑、补全、导航和 Analysis 请求不能承担可视化采集的额外性能开销。

## 变更内容

- 在 IntelliJ Tool Window 中显式分析当前仓颉文件，记录 raw CFIR、逐个 resolve phase 和三类 diagnostics。
- 在每个阶段边界冻结不可变 CFIR 快照，并保存为本地 `.cjtrace` 工件。
- 提供事件时间线、CFIR 树与关系图、阶段差异、诊断列表、源码定位和工件重新打开。
- 所有采集、快照和存储代码位于 `intellij-ide`。不在 CFIR/Analysis 执行路径安装 trace hook。
- 如果文件已有解析缓存，工件标记阶段已解析状态；首版不清除或重置 session/cache。

## 功能 (Capabilities)

### 新增功能
- `cfir-analysis-trace-visualization`: 显式运行并回放当前文件的 CFIR lazy resolve 阶段。

## 影响

- 仅修改独立的 `intellij-ide` 可视化工具代码、插件描述符和 OpenSpec 文档。
- 主仓 CFIR 与 Analysis 代码不包含可视化类型、服务注册或采集调用点。
- 用户主动运行工具时会触发当前文件解析、阶段快照和 diagnostics 查询。
