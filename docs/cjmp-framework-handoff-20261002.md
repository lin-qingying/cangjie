# CJMP 框架实施交接（2026-10-02）

> 后续实施与当前工作区状态请先读 [第二份 1002 交接](cjmp-framework-handoff-20261002-2.md)。
> 本文件保留此前检查点；其中“无未提交源码、无后台作业”等状态已由第二份交接取代。

上一份交接是 `docs/cjmp-framework-handoff-20261001.md`（基线 `ff7b4061f`）。本文件覆盖其后
20 个提交的进展，并**取代**它作为当前执行上下文；旧文件中仍然有效的深度语义边界在 §8 保留引用。

> 目标仍未完成。**不得**把目标缩成"测试全绿"，也**不得**把"纯算法已写好"等同于"已接入运行时"。
> 本会话最重要的教训正是后者：算法早就在，接线才暴露真缺陷（见 §4.3）。

## 1. 工作位置与保护边界

- 唯一实施工作树：`C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie`
- 分支：`codex/cjmp-framework-implementation`
- 本交接写作时 HEAD：`294eafb7d`；目标基线 `ae4126657`（自基线起 **19** 个提交）
- 原仓 `D:\code\intellij\cangjie` **只读**：不修改、不 reset/checkout/stash/清理。只读其
  `external/` 参考源码与使用其 gradle-queue-cli JAR。
- **不要创建第二个工作树，不要重做恢复。** 曾有工作树的 `.git` 与管理目录被误删并已修复
  （30 609 个文件零丢失）；恢复工作已完成，重复恢复会覆盖新进展。
- 保留的工作区噪声（**不要动**）：
  - 已删除未提交：根目录 `测试结果 -Run_Cfir_Test_All.html`
  - 未跟踪：`.workbuddy/tmp/cjmp_probe113/review-20260928/ll-completeness-20260930/`
- 官方参考必须用 tag `v1.1.3`（原仓 `external/cangjie_compiler` 当前 checkout 是 v1.0.0）：
  `git -C 'D:\code\intellij\cangjie\external\cangjie_compiler' show v1.1.3:<path>`。
  Kotlin 参考：`D:\code\intellij\cangjie\external\kotlin`。
- Git 一律 `git -c core.safecrlf=false ...`。

### 唯一的 Gradle 入口

```powershell
java -jar 'D:\code\intellij\cangjie\gradle-queue-cli\build\libs\gradle-queue-cli.jar' --project-dir 'C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie' <TASKS> --continue --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8'
```

工作树自己的 queue bootstrap 源不完整，**不能**改回直接 `gradlew`。

## 2. 红线：什么算"降级"，什么不算

禁止：fixture 特判、兼容层/兜底路径、为让测试变绿而简化语义、全模块 BODY 预热、
把 CJMP getter 塞进 STATUS/TYPES buildIndex、保留并行的第二真相（旧存储与新 aggregate 并存当数据源）。

必须：框架级修复；每条结论以官方 cjc 1.1.3 与 Kotlin K2 为语义依据；重要接口与代码用中文注释。

本会话实际执行过一次"拒绝降级"：实例成员变量本可标成 `STATIC_MISMATCH` 直接接上 plan，
但那曲解官方分工（实例成员应在**容器合并**阶段配对），故**不迁**并写明前置条件（§5.3）。

## 3. 验证体系与当前基线结果

| 入口 | 覆盖 | 结果 | 本会话复核 |
|---|---|---|---|
| `:analysis:low-level-api-cfir:test` | LL 按需解析、CJMP 匹配/诊断/家族完备 | **147 / 0 失败** | ✅ 亲自跑 |
| `:cfir:analysis-tests:test` | CFIR eager 全量（含 common-specific e2e 语料） | **8966 / 0 失败 / 397 跳过** | ✅ 亲自跑 |
| `:compiler:frontend:test --tests '*CjmpTwoPhaseCompilationTest*'` | CLI 两段式入口 | **19 / 0 失败** | ✅ 亲自跑 |
| `:cfir:resolve:test` | resolve 单元 | 148 / **5 失败** | ✅ 与干净基线工作树逐项对照，确认**基线同样存在** |
| `:resolution.common:test` | 纯合并器/匹配器 | 57 / 57 | 承自旧交接，未复核 |

**这 5 个既有失败**（勿误判为回归）：`CfirMapArgumentsTest.Defaults`、
`CfirMapTypeArgumentsTest` ×3、`CfirBuiltinSymbolProviderTest`。

两条权威套件（LL + CFIR）是**每次改动后的回归闸门**，两个都要跑、都要绿。

### 验证陷阱（本会话踩过）

1. **缓存不是验证**：`FROM-CACHE` 的绿不代表跑过。优先非缓存运行，并核对用例计数。
2. **新增 LL 夹具必须重新生成测试类**，否则 `--tests` 与 `testAllFilesPresentIn*` 都匹配不到：
   `:analysis:low-level-api-cfir:generateTests`。
3. **`--tests` 模式大小写敏感**：`*propertyMemberMatches*` **不**匹配 `testPropertyMemberMatches`。
4. **不要用 `--rerun-tasks`**：在 `-Xmx1g` 下会引发 Gradle daemon "disappeared unexpectedly"（infrastructure OOM），已发生两次。
5. PowerShell 用 `pwsh`；读文件用 `Get-Content -Encoding UTF8`。

## 4. 本会话（`ae4126657` → `294eafb7d`，19 提交）做了什么

### 4.1 读取侧迁移（P1/P3 的读取面）——已完成

| 提交 | 内容 |
|---|---|
| `ca88fa548` | common-side 读取前先聚合**完整** family |
| `bbf7bafe8` | family 聚合纳入声明成员（此前只到容器级） |
| `18e152d96` | 以声明为键的 family 访问器 + 记录 shadow 读取约束 |
| `59cf40e10` | 区分"已定案"与"解析中当前态"两种读取，迁移 checker 的 counterpart 读取 |
| `5ef6ec701` | 补全 family 读取面，**`CfirCommonSpecificChecker` 零 `storage.*`** |
| `853f5acd9` | 发布 settled shadow 事实（`settledCjmpCommonShadowed`），解除作用域路径的读取阻塞 |

要点（见 `docs/cjmp-family-read-migration-ledger.md`）：

- **双读取模式契约**：checker 阶段用"阶段完备读取"（可推进解析）；resolver/scope/实例化阶段
  用"解析中当前态读取"（**绝不**触发完备）。混淆二者会造成实测回归
  （`testCjmpMultipleCommonExtends`、`testCjmpGenericOuterTypeParameterMembers`）。
- `settledCjmpCommonShadowed`：`null` = 未定案 → 回落直读（不触发完备）；`true/false` = 定案后纯查表。
  8 个调用点（泛型实例化 5 处、继承深度 2 处、extend 成员 scope 1 处）**已通过谓词间接受益**，
  无需逐个改动。
- `TowerLevelHandler` / `CfirActualizingScope`：经 scope 消费，无直读，**无需改动**。

### 4.2 声明级不可变 plan **真正接入运行时**（P1 核心，3/5 类）

| 提交 | 内容 |
|---|---|
| `3d695e35a` | 工厂改为可生产：资格判定收敛为**独立入参**（唯一来源） |
| `8170a51d6` | **函数/构造器按同名组一次性求值**接入生产 |
| `8fed6ba83` | property（本体 + getter/setter）接入生产 |
| `d7bd9c36b` | static/global 变量接入生产 |

覆盖率：

| 声明种类 | 状态 | 说明 |
|---|---|---|
| `CfirNamedFunction` / `CfirConstructor` | ✅ | 同名组一次性求值 |
| `CfirProperty`（含访问器） | ✅ | `buildPropertyMatchPlan`；访问器有独立断言用例 |
| `CfirVariable`（static/global） | ✅ | `VariableAssignment(STATIC_OR_GLOBAL)` |
| `CfirVariable`（实例成员） | ⬜ | 官方在**合并**阶段配对；需 `initialBindings` 作 seed |
| `CfirEnumConstructor` | ⬜ | 仍走 `matchEnumConstructor`；其判据已是共享纯比较器 |
| `CfirPatternVariable` | ⬜ | 求值器已有 `TuplePattern`/`NonTuplePattern` 分支 |

### 4.3 接线暴露并修掉的真缺陷（**最重要的经验**）

1. **参数事实在签名失败时被丢弃**（`ec4f83736`）。工厂原在 `signature != Matched` 时把
   `parameters` 置空，于是"签名不匹配但参数名已可判失败"的候选不产生 `ParameterMismatch`
   事件。官方次序（`MatchCJMPFunction`，`CheckCJMP.cpp:915-991`）与此相悖；探针
   `p1-first-fit-20260929` §1.4/1.5 实测"早期诊断不因后来找到匹配者而消失"。
   改为：只要两侧形参**按位置对齐**就产出参数事实。修后 `testFirstFitCandidateDiagnostic` 通过。

2. **访问器绑定从未落库**（`a66a9a07d`）。访问器的绑定发生在 property 绑定规则**内部、
   不产生 `CjmpCandidateVisit`**，而翻译层只翻译 visits。由本会话新加的断言用例抓出：
   ```
   expected: <<element: CfirPropertyAccessor>> but was: <null>
   ```
   > **一般化规则：求值器产出有两类——`visits`（候选级）与 `acceptedBindings`
   > （含绑定规则内部的访问器等）。翻译层必须覆盖两者，只翻译 visits 会静默丢绑定。**
   > 接入 variable/enum/pattern 时必须按此检查。

3. **组聚合的必要性**。官方占用状态（`common.specificImplementation`）在**整个包内共享**，
   所以同名 specific 必须**按组一次性求值**：首个成功者绑定、其余报 `SECOND_BINDING`。
   逐个 specific 独立求值无法表达（每份计划有独立占用表）。这是上一轮接线失败的真因。

### 4.4 文档与清理

- `4ca617c81` 逐声明种类的 plan 覆盖表；`1a81be699` 访问器缺陷与两类产出教训；
  `259e544dd` **删除一条虚假约束注释**——`matchAgainstCandidatesDirectly` 的 KDoc 仍称
  "plan 会丢参数诊断、故生产保持直连"，该约束在 `ec4f83736`/`8170a51d6` 已解除。
  **声称虚假约束的注释比没有注释更坏。**
- `294eafb7d` 验证入口表 + 两段式在接线后复跑结论。

## 5. 必须传给接手者的架构认知

### 5.1 共享 matcher 上下文以**符号**为键，工厂以**声明**为键

两者必须各自转换，**不能混用**。混用会让父容器类型参数映射失效，成员直接被判不兼容
（本会话踩到并修掉）。对照 `matchFunctionGroupWithPlan` 里 `parentMappingSymbols` /
`parentMappingDeclarations` 的成对转换。

### 5.2 翻译层的三条不变量（已由测试固定）

1. 配对统一经 `storage.bind`：它既建正向映射，也把**被拒绝的第二绑定**留在反向表里
   （`MULTIPLE_COMMON_IMPLEMENTATIONS` 判据依赖这一点），并自行登记 `SECOND_BINDING`。
2. `Occupied` 且尚未绑定的 specific 需显式 `bind` 一次，补上反向表条目
   （`assertFirstFit` 要求 `specificBindingsFor(common)` **含被拒者**）。
3. `storage.bind` 对同一 `(specific, common)` 对**幂等**（命中即返回 true，不产生 SECOND_BINDING），
   所以按 `acceptedBindings` 补齐写入是安全的。

### 5.3 语义分工（官方 `CheckCJMP.cpp:589` 注释）

- **实例成员变量**在**容器合并**阶段配对 → 求值器规则 `MergedMemberVariable`
  （断言绑定已由合并 seed 建立）。合并路径的 `commonDecl.doNotExport = true` 在 `:293`。
- **global / static** 在 **Match 阶段**配对 → `VariableAssignment(STATIC_OR_GLOBAL)`，
  官方成功时置 `doNotExport = true`（`MatchCJMPVar` 定义于 `:1050`，该分支在 `:1077`）。

函数定义位置备查：`MatchCJMPFunction :915`、`MatchCJMPProp :993`、
`MatchCJMPEnumConstructor :1031`、`MatchCJMPVar :1050`。

因此：**不要**用 `STATIC_MISMATCH` 顶替 `MergedMemberVariable`——那是语义降级。

### 5.4 property 的三键事实

官方 `MatchCJMPProp`（`CheckCJMP.cpp:993-1012`）对本体与两侧访问器**分别** `TrySetSpecificImpl`
（本体 `:1001`、getter `:1003`、setter `:1007`；`ret &=` 串联，任一步失败即整体不匹配）。
求值器 `requireRootBinding` **只作用于本体**，所以 getter/setter 可带自己的 specific/common 符号——
在占用表里是**三个不同的键**，不会互相判成第二绑定。

## 6. 分阶段进度（加权估计 ≈ 51%）

权重按主计划 §8 自己的话设定（"P1–P3 是其他大部分补丁的根源"）。

| 阶段 | 权重 | 完成 | 依据 | 置信度 |
|---|---|---|---|---|
| P0 取证与红测试 | 10% | 80% | 红例已修；探针目录齐备 | 中（未重跑探针） |
| P1 配对阶段与读取入口 | 20% | 70% | 阶段位置 + 只读 aggregate + plan 接入 3/5 类 | **高** |
| P2 反序列化 common 语义 | 8% | 75% | `CjmpDeserializedCommonSideReporter` 已消费快照 | 中 |
| P3 合并视图 | 15% | 40% | 读取侧已通；**合并成员图未接线** | 中高 |
| P4 声明级实现关系 | 12% | 12% | 未开始，需合并 seed | 高 |
| P5 匹配器共享判据 | 8% | 70% | 资格为唯一来源 | 高 |
| P6 检查器对齐与分层 | 10% | 45% | common 侧零 storage；E 组无证据 | 中低 |
| P7 生产 LL/IDE 入口 | 7% | 55% | LL 4 处注册、懒解析、阶段门、两段式绿 | 中高 |
| P8 序列化/CLI/诊断通道 | 6% | 25% | `doNotExport` 未建模；CJO 写入侧在补齐 | 中 |
| P9 resolve 顺带修复 | 2% | 10% | 无本会话证据 | 低 |
| P10 收口 | 2% | 10% | 验证体系就绪 | 中 |

**不确定性最大的是 P2/P8/P9**——完成度主要承自旧交接与台账，本会话未逐条复核。

## 7. 剩余工作与推荐顺序

| # | 事项 | 体量 | 风险 | 备注 |
|---|---|---|---|---|
| 1 | **B：enum 构造器接 plan**（第 4 类） | 中 | 低 | 5+ 夹具家族可回归；需保留 `recordUnmatched` 分支 |
| 2 | pattern 接 plan（第 5 类） | 中 | 中 | 不能用扁平 `bindingVariables` 抹掉非变量项的停止边界 |
| 3 | **A：CFIR → `CjmpMergeContainer` 事实构建 + `initialBindings` 作 seed** | 大（多轮） | 高 | 解锁 P3/P4 与实例成员变量 |
| 4 | 删除旧可变存储写 API（`bind`/`record*`/`clear`） | 中 | 依赖 1–3 | 现审计：所有写 API 仍被未迁移种类使用，**暂无死代码可删** |
| 5 | `doNotExport` 建模（P8） | 中 | 中 | 见 §9 |
| 6 | P6 的 E 组、P7 平台 provider、P9、P10 收口 | 多 | 未知 | |

**利好**：`CjmpContainerMergeEvaluator` **已有 15 个纯单测**
（`resolution.common/test/.../CjmpContainerMergeEvaluatorTest.kt`）——A 的纯算法已验证，
缺的只是 CFIR 事实构建与 seed 接线。这显著降低了 A 的风险等级。

**推荐**：先做 **B**（1 轮内可完成并提交，覆盖率推到 4/5），**再专门开 A**（多轮 + 独立验证计划）。
理由：A 一旦半途而止，会让成员查找处于两套语义并存的状态，**比不做更糟**。

> ⚠️ 此决策**尚未得到用户确认**。上一轮已向用户提出 A/B 二选一并建议"先 B 后 A"，
> 用户未答即要求写本交接。接手者可直接按"先 B 后 A"推进，或先向用户确认。

## 8. 仍然有效的深度语义边界（引自旧交接 §9–§10，未变）

- **nominal 不是 first specific wins**：保留 identifier 首个角色声明，后来的对侧可多次合并；
  generic/non-generic 不同则跳过；N/M 都 generic 时仍合并，数量诊断在后。
- common 全部成员并入 specific（含普通/private）；logical owner 改为 specific，成员 `doNotExport` 重置 false。
- 只有 COMMON instance VarDecl（含真实成员参数字段）按名 seed；specific 字段换入原 slot，
  旧节点保留在 common 供 initializer 分析；普通 member-param 只去重、无实现 seed。
- 重复 specific 字段绑定当前 slot，可形成 common→specific1→specific2 **指针链**，不能压成按名取最后值。
- common-only 成员必须留在最终成员视图；property、function、constructor **不**在字段 seed 阶段预绑定。
- 普通 enum variant 即使 native 是 VarDecl 也走 enum TryBind，**不能**当 global variable assignment。
- `CJO HAS_INITIAL` 与实际 desugar helper 是独立事实；library 默认 marker 不证明 helper 存在。
- `extendProvider.getAllExtends` 会触发 TYPES/索引更新，不是纯 raw 枚举入口。
- 模块身份用 `CfirModuleData.equals`（`LLCfirModuleData` 已按 caModule 实现），不能用 `moduleData.name`。
- `CfirDeclarationAttributes.copy` 是浅复制；新 plan 不能从旧符号照抄。
- accessor 的 LL root 是 property；value parameter root 是包含 callable；
  pattern binding 经 `provider.getCfirPatternVariableForBinding` 找真实 root，不能按名猜。
- StateKeeper 不会自动保护新 attributes：成功后一次发布，或显式纳入 rollback。

## 9. 新发现的未建模项（本会话）

**`doNotExport` 在存储与 CJO 侧完全未建模。** 官方在 Match/合并阶段对"被 specific 实现的
common 声明"置 `doNotExport = true`：

| 位置 | 场景 |
|---|---|
| `CheckCJMP.cpp:911` | `TrySetSpecificImpl` 内——**所有**配对成功路径共用 |
| `CheckCJMP.cpp:293` | 合并路径（实例成员） |
| `CheckCJMP.cpp:1077` | `MatchCJMPVar` 的 static/global 分支 |

另有 `:238` 在合并时把成员 `doNotExport` 重置为 `false`。
本仓库除本会话新增的注释外**没有任何对应概念**。
属 **P8（CJO 导出）** 范畴，不是读取侧问题；plan 覆盖率补齐后应单独处理。

## 10. 交接时的运行状态

- 工作区仅剩 §1 列出的两项保留噪声，**无未提交的源码改动**。
- **没有需要等待的后台 Gradle 作业**；本会话所有构建均已终止。
- 工作树 `.git` 健康（`git status` 正常，索引已重建）。
- 已用技能：`kotlin-cangjie-frontend-parity`、`cangjie-cfir-llt-repair`（新会话按要求读官方语义对位）。

## 11. 接手后的第一步

1. 进入上述工作树，读 `AGENTS.md`、`CLAUDE.md`、本交接、主计划、
   `docs/cjmp-family-read-migration-ledger.md`；核实 HEAD 与真实 `git diff`；
   保护 §1 的两项噪声。**不要**创建第二个工作树、**不要**重做恢复。
2. **不要**为"热身"重跑刚完成的全部验证——§3 的计数是可信基线。
3. 按 §7 的推荐顺序推进（先 B 后 A，或先与用户确认）。每完成一类/一步：
   **跑 LL + CFIR 两条权威套件并核对计数 → 提交**。
4. A 的三步走建议：**CFIR 事实构建 → 单测验证 → seed 接线**，每步单独提交，
   避免成员查找出现两套语义并存。
5. 只有在逐项证据证明整个目标（P0–P10 / A1–J1、消费者迁移、旧写 API 删除）完成时，
   才可标记 goal complete。
