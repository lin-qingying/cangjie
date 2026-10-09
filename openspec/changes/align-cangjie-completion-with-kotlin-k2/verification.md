# 变更文档验证记录

## 2026-10-08 重跑（当前有效）

对象：**文档一致性**，不是实现验收。工具：`openspec-cn`（`/c/Users/lin17/AppData/Local/pnpm/openspec-cn`）+ 两个只读脚本。

| 检查 | 结果 |
|---|---|
| OpenSpec 严格校验 | `openspec-cn validate align-cangjie-completion-with-kotlin-k2` → **验证通过** |
| 产出物状态 | `openspec-cn status` → **4/4**（proposal / design / specs / tasks 均 done） |
| 需求与场景计数 | `analysis-completion-support` 9 需求 / 17 场景；`ide-completion-support` 12 / 20；`lsp-completion-support` 9 / 18 ⇒ **30 需求 / 55 场景** |
| 任务计数与勾选态 | **107 项**：已勾选 **59**、未勾选 **48**（2026-10-08 实测计数） |
| 相对链接 | 89 条，重算层级后**全部可定位**（修复前 84 条按当前文档位置解析失败，见下） |
| 文本检查 | 无 U+FFFD、无行尾空白（各修掉一处，见下） |
| 未重跑 | ① `design.md` 的源码行号锚点未逐条核对——本轮实现改动会移动部分行号，且锚点以主检出为准；② `source-baseline.json` 的 SHA-256 漂移核对（其锚定状态只在上游工作树成立） |

### 本轮修掉的三处文档缺陷

1. **相对链接层级错误（84 条）**。文档里的链接有两族深度，分别是在别的位置写成的：
   7 层上跳（`../../../../../../../`）按「文档位于**另一个工作树**的 `openspec/changes/<name>/`」写成，
   3 层上跳的 `modules/`、`product/`、`gradle/`、`settings.gradle.kts` 按「文档位于 **intellij-ide 仓内**」写成。
   文档现在住在 `cangjie/openspec/changes/<name>/`，两族都对不上。已按当前位置统一重算
   （7 层 → 3 层；IDE 目标补 `intellij-ide/` 前缀），重算后 89 条全部可定位。
2. **`tasks.md` 3.8 行有一处损坏字符**（`fragmentMemberScopeMatchesScopeContextProjection` 前的「用例」被写成三个 U+FFFD），已还原。
3. **`implementation-log.md` 一处行尾空白**（表格行内），已去除。

### 实现验证不在本文件范围

编译/测试的执行记录在 `implementation-log.md`（按套件列出用例数与失败数）。
本文件只回答「文档自身是否自洽、是否可定位」。

> 下方 2026-10-02 的记录已被本轮取代，仅作历史留存。

---

# 本轮文档验证记录（2026-10-02，已被 2026-10-08 重跑取代）

日期：2026-10-02。对象：Kotlin K2 补全对齐计划，不是实现验收。

> ## ⚠️ 本记录已过期（2026-10-04）
>
> 下列结论**全部作废**，因为计划此后发生了实质变更：
>
> - LSP 接入被**纳入**范围（新增 `specs/lsp-completion-support/spec.md`，含 9 项需求）；「需求 21 项 / 场景 35 个」已失效。
> - 新增**代码片段作用域**与**持久化符号索引**两项交付物及其需求场景。
> - 新增第 15 组 LSP 任务 15.1–15.12 与 0.1（G0 前置）；`10.1` 与 `10.4` 被**取消勾选**——因此「全部未勾选」已不成立。
> - 三个出货宿主（`intellij-ide` / `deveco` / `:lsp`）的接线面被拆开登记；DevEco 的装配机制与 IDE 不同。
> - `design.md`、`tasks.md`、`proposal.md` 均已重写；`source-baseline.json` 所锚定的源码状态只在**上游工作树**成立，主检出与两个工作树的 HEAD 均未变。
>
> **重跑要求**（见设计 §12.11）：OpenSpec 严格校验、需求→任务映射重算、任务计数与勾选态重算、源码依据与链接重跑、文本检查。**在重跑之前，不得引用本文件的任何数字作为当前状态。**
>
> 上方 2026-10-08 的记录即该重跑的结果。

## 已完成的检查（2026-10-02，已过期）

| 检查 | 结果 |
|---|---|
| OpenSpec artifact 状态 | proposal、design、specs、tasks 均为 done |
| OpenSpec 严格校验 | valid: true，issues 为空 |
| 需求与任务映射 | 21 项需求、35 个场景均有任务覆盖 |
| 实现任务格式 | 82 项任务，编号无重复，全部未勾选 |
| 源码依据 | 85 个文件/行号锚点，涉及 84 个文件，均存在且行号有效 |
| 源码内容基线 | 与 source-baseline.json 的 SHA-256 一致，未发现漂移 |
| Markdown 链接 | 92 个相对链接经路径归一化验证可定位 |
| 文档文本 | UTF-8，无替换字符和尾随空白 |
| 源码改动 | 宿主状态仅新增 openspec 计划目录；主仓库与宿主 HEAD 未改变 |

OpenSpec 的 done 只表示规划产物完成，不代表补全实现或测试通过。

## 技能与重试记录

- 直接读取主仓库 openspec-propose/SKILL.md，并按其 new change、status、instructions 的依赖顺序生成产物。该技能未列于当前 Skill 工具目录，没有声称通过 Skill 工具调用。
- 当前工具集没有 TodoWrite，使用 OpenSpec CLI 的 artifact 状态跟踪产物，未将未来实现任务标为已完成。
- 只读子任务曾遇到服务端 503；重试后相关报告均已返回。
- 长设计文档经 shell 写入失败且未创建 design.md；已改用文件写入工具成功保存。
- 首轮相对链接检查未先归一化路径，在 Windows 上将过长的含父目录路径误判为不存在。已对比原始、归一化和绝对目标，确认一致，并对全部链接重跑通过。

## 明确未执行

未执行 cjc、Gradle 编译/测试、Kotlin 测试、生成器、插件打包、兼容性验证或 IDE 沙箱。未修改功能实现，未启用 LSP/native/SMART/并行开关，未提交或推送。

后续任务必须用对应源码版本下的新运行结果验收。历史测试 XML、源码中的测试函数和本记录都不能替代实现验证。
