## 1. 独立工具架构

- [x] 1.1 将轨迹 DTO、采集服务、快照 builder 和 codec 放在 IDE 工具模块。
- [x] 1.2 移除 CFIR/Analysis 常规路径中的 trace 上下文、Analysis 生命周期钩子和快照回调。
- [x] 1.3 通过 IDE Tool Window 按需显式执行当前文件分析，不触发或记录普通用户请求。

## 2. CFIR 与 diagnostics 快照

- [x] 2.1 使用公开 Low-level CFIR API 建立 raw 文件并按 resolve phase 顺序推进。
- [x] 2.2 每个阶段记录时长、缓存阶段状态和不可变 CFIR 快照。
- [x] 2.3 分别采集默认、额外和实验性 diagnostics，并关联到对应事件。
- [x] 2.4 通过节点数和渲染文本限制控制工件大小。

## 3. 工件与可视化

- [x] 3.1 在 IDE 模块维护版本化 `.cjtrace` JSON 格式和本地文件读写。
- [x] 3.2 提供阶段时间线、CFIR 树/有向父子图、阶段差异、diagnostics 和源码导航。
- [x] 3.3 支持打开已有工件，并拒绝损坏或未知版本。

## 4. 验证

- [x] 4.1 验证 `.cjtrace` codec round-trip、未知 schema 和损坏 JSON。
- [x] 4.2 编译主仓受影响 CFIR/Analysis 模块。
- [x] 4.3 编译独立 IDE `:modules:ide:base` 模块。
- [x] 4.4 运行 IDE codec 定向测试，并搜索确认 CFIR/Analysis 无可视化钩子。
- [x] 4.5 在 EDT 只读取当前 VirtualFile，将 PSI 查找放在后台 ReadAction 中。
