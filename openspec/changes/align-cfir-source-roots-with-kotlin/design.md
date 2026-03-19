## 上下文

当前 CFIR 前端在 CLI 与测试中使用自定义 `CLI_SOURCE_FILE_PATHS` 读取源文件，路径来源与 Kotlin 编译器生态不一致，且缺乏统一的内容根(Content Roots)模型。实际问题是测试配置未写入该自定义 key，导致前端 pipeline 生成的 `CjFile` 为空，进而 CHECKERS 阶段无输入。与此同时，仓库内已包含 Kotlin 编译器源码，其前端管线以 `CLIConfigurationKeys.CONTENT_ROOTS` + source roots 收集流程为唯一入口。项目希望以整体架构一致性为目标，统一“源文件输入”模型，降低维护成本并为多模块/多源输入扩展奠基。

约束与相关方：
- 约束：现有 Cangjie 前端管线与测试基础设施已大规模使用 `CompilerConfiguration`；变更需兼容多平台与未来扩展。
- 相关方：CLI 前端管线、测试基础设施、CFIR 解析与诊断团队、文档维护者。

## 目标 / 非目标

**目标：**
- 以 Kotlin 编译器为基线，统一 CFIR 的“源文件输入”模型为 Content Roots。
- 在 CLI 与测试中使用同一套配置路径写入机制，避免分裂。
- 引入与 Kotlin 类似的 source roots 收集行为（去重、平台/公共源拆分、扩展点）。
- 提供清晰的迁移路径，逐步弃用 `CLI_SOURCE_FILE_PATHS`。

**非目标：**
- 不在本变更中引入新的语义诊断或改动解析算法。
- 不以“最快编译”或“最小改动”为主目标。
- 不在本变更中引入新的平台特定编译器插件逻辑。

## 决策

1. **以 Content Roots 作为唯一的源文件输入模型**
   - 方案：新增/引入 Cangjie 侧的 `CONTENT_ROOTS` 配置键（或与 Kotlin 兼容的结构），并在 CLI 前端统一读取。
   - 备选：继续使用 `CLI_SOURCE_FILE_PATHS` 并在测试补写该值。
   - 选择原因：Content Roots 是 Kotlin 生态的事实标准，便于复用流程、减少分支，并承载依赖/模块化扩展能力。

2. **对齐 Kotlin 的 source roots 收集流程**
   - 方案：实现与 `GroupedKtSources.collectSources(...)` 等价的收集逻辑，包含：
     - root 去重与错误提示
     - 平台/公共源拆分
     - 支持扩展点（未来接入额外 source 收集扩展）
   - 备选：在现有逻辑上仅替换配置键，不改变收集流程。
   - 选择原因：避免行为差异导致测试与 CLI 产生隐性偏差。

3. **保留兼容层但标记弃用**
   - 方案：在过渡期允许 `CLI_SOURCE_FILE_PATHS` 作为兜底写入到 Content Roots，并输出弃用提示/文档。
   - 备选：直接移除旧 key（破坏性变更）。
   - 选择原因：减少短期迁移阻力，但明确最终收敛方向。

## 风险 / 权衡

- [迁移复杂度上升] → 提供清晰的迁移指南，先在测试基础设施中落地并验证，再推广到 CLI。
- [行为差异引发测试回归] → 引入对齐的 source roots 去重/排序策略，并在测试报告中提供差异对比。
- [双轨期维护成本增加] → 明确弃用周期，限定兼容层仅保留一个版本周期。

## 迁移计划

1. 在 Cangjie 配置层新增/引入 `CONTENT_ROOTS` 与 source roots 类型定义。
2. 前端 pipeline 改为基于 Content Roots 收集源文件，保留 `CLI_SOURCE_FILE_PATHS` 兼容映射（如存在）。
3. 测试基础设施改为统一写入 Content Roots，并保证生成的源文件列表非空。
4. 更新文档与测试基线，确保行为一致。
5. 后续版本移除 `CLI_SOURCE_FILE_PATHS` 兼容逻辑。

## 待定问题

- Content Roots 类型是否完全复用 Kotlin 的结构，还是在 Cangjie 侧做轻量化适配？
- 对于非 Kotlin 扩展（例如脚本/宏/额外源文件扩展点）是否需要第一期即支持？
