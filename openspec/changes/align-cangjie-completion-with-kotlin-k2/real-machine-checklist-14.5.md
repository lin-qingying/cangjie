# 14.5 真机验收清单（真人 GUI 会话用，10 分钟）

机械化的那一半已跑完并入档（见 `verification-14.5-plugin-verifier.md`：制品判定 **Compatible**、撤销项确证为夹具限制）。
**剩下的六项只有真人 GUI 会话能给证据**——宿主仓里没有 UI 测试基建（无 `runIdeForUiTests` / `remote-robot` /
任何 UiTest·Robot·Smoke 类），verifier 与 light fixture 都替代不了。本文件把那半压成一份照着点一遍就行的清单。

## 0. 起沙箱

```bash
cd "D:/code/intellij/cangjie/intellij-ide/.claude/worktrees/musing-bun-5ae793" && ./gradlew.bat -p . :product:idea-plugin:runIde -Pcangjie.upstream.root=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun
```

- 沙箱平台：IU-253.29346.50（与 verifier、声明矩阵同一个）。
- 先开一个有仓颉 SDK 的工程（或新建），等索引结束（右下角进度条消失）。**索引没结束就补全，结论不算数。**

## 1. 加载（沙箱内）

- [ ] IDE 起来后 `Help → About` / 插件列表里能看到 **CangJie 2.1.4** 已启用，且**没有**报依赖缺失（LSP4IJ / TOML / JSON 三件）。
- [ ] 打开 `.cj` 文件，语法高亮正常、无红色报错横幅。
- [ ] `Help → Show Log in Explorer` 看 `idea.log`：搜 `cn.cangnova.cangjie`，确认无 `PluginException` / `ClassNotFoundException`。

## 2. 触发

- [ ] 在表达式位置按 `Ctrl+Space` → BASIC 补全弹窗出现。
- [ ] 按 `Ctrl+Shift+Space` → SMART 补全弹窗出现（候选应含类型匹配项）。
- [ ] 打字自动触发（仓颉插件的 typed handler）→ 弹窗自动出现（若产品未启用自动触发，记下来即可）。
- [ ] 在**没有**配置 LSP 服务的工程里重复一次 → 原生链路照常出候选（14.3 已在 fixture 覆盖，这里做真机复核）。

## 3. 排序

- [ ] 在同一位置对比 BASIC 与 SMART 的候选顺序：SMART 的类型匹配项应排在前面。
- [ ] 局部变量 / 参数 / 同文件顶层 / 库符号四类各点一次，确认相关性顺序符合直觉（记录**具体顺序**，别只写「正常」）。

## 4. 插入

- [ ] `Enter` 接受候选：文本正确插入，光标落在应有位置，**没有**重复前缀或吃掉后续字符。
- [ ] `Tab` 接受候选：同上。
- [ ] 唯一候选时（只可能匹配一个）确认是否自动插入。
- [ ] 在 `xxx.` 之后（成员访问）补全 → 接受后是「补后缀」而不是「补全名」。

## 5. 撤销 ← **这一项夹具验不了，只有真机能给结论**

- [ ] 接受候选后按 `Ctrl+Z`：文本**逐字回到补全前**。
      **若不回退**，那就是真缺陷（不是夹具问题了），请把当时文本、候选名与 `idea.log` 片段记下来。
- [ ] 连按两次 `Ctrl+Z`：不应把补全之前用户的正常输入也一起吞掉（撤销粒度）。

## 6. 错误态

- [ ] 在语法错误的文件里触发补全：弹窗正常出现或正常不出现，**不抛异常**（`idea.log` 无 `Throwable`）。
- [ ] 工程 SDK 未配置 / 路径失效时触发补全：给出降级行为，不卡界面。
- [ ] LSP 服务进程被手动杀掉后再触发：原生链路不受影响。

## 7. 两条「夹具限制」的真机复验

- [ ] **撤销**：见第 5 节。
- [ ] **库侧候选**：在 `import std.collection.*` 的文件里，于 `ArrayList` 前缀处触发补全 → 候选里应出现**来自标准库**的名字
      （14.2② 在 fixture 里因夹具 std 库不提供「按前缀发现声明」而无法验；真机应能看到）。

## 8. 6.6② 的跨会话持久（顺带）

- [ ] 补全覆盖一个**没有打开**的文件里的顶层声明（`CangJieSymbolIndexQueryTest` 的口径）→ 能召回。
- [ ] 重启 IDE（或 `File → Invalidate Caches` 之外直接重启），再补一次 → 仍能召回（持久索引，非会话内快照）。

## 9. 平台矩阵

- [ ] 沙箱平台为 IU-253.29346.50，与 `PlatformMatrixDeclarationTest` 的声明一致（verifier 已复核，这里只确认跑的就是它）。
- [ ] 换平台（如 IC 253 或更高版本）**不在**本项范围：未测平台不声明兼容（12.7 的口径）。

## 10. 记录与勾选

把上表结果逐条追加到 `tasks.md` 的 14.5 行（**写实测到的具体值**，例如第 3 节的实际候选顺序、
第 5 节的撤销结果），然后：

- 十项全过 → 勾选 14.5，并按 14.6 的口径重新评估「是否调整 native/SMART 默认开关」；
- 有任何一项不过 → 保持不勾选，把失败现象写成新的门禁条目（并说明是真缺陷还是环境问题）。
