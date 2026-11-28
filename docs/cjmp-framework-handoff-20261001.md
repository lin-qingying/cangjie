# CJMP 框架实施交接（2026-10-01）

这是给接手会话的执行上下文。原目标是完成 `docs/cjmp-framework-review-and-fix-plan-20260928.md` 的全部 P0–P10 / A1–J1，在独立工作树实施、及时提交。目标仍未完成；不得将目标缩成测试全绿，或把已写纯算法等同于已接入运行时。

## 1. 工作位置与保护边界

- 唯一实施工作树：`C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie`
- 分支：`codex/cjmp-framework-implementation`
- 本交接与本轮字段模型迁移一起保存；提交前基线为 `ff7b4061f`。用 `git log -5 --oneline` 获取交接提交及随后状态。
- 原仓 `D:\code\intellij\cangjie` 有其他工作，不能在原仓修改、reset、checkout、stash 或清理文件。只读其 external 参考源码，以及使用已有的 gradle-queue-cli JAR。
- 存在一项不属于本任务的工作树删除：根目录 `测试结果 -Run_Cfir_Test_All.html`。未纳入本次提交，不要恢复或删除来整理状态。
- gen/tests-gen 文件经生成任务后可能显示 M，但没有实际 Git 内容差异。按 `git -c core.safecrlf=false diff` 判断，不要批量回退。
- `.workbuddy/tmp/cjmp_probe113/review-20260928/ll-completeness-20260930/` 留有未提交的官方探针文本，不要清理。
- 交接时所有本任务 Gradle 执行都已终止，没有需要继续等待的活动构建。最后 session 79322 已退出 1，仅因已知 unused 红例。77120、5473、21603、61846 也均已终止；不要重新轮询或重启它们。其他项目/IDE 的 Gradle daemon 不属于本任务，不要结束它们。

## 2. 用户规则与命令

- 框架级实现，禁止 fixture 特判、兼容/兜底路径、为了通过测试简化语义；重要代码与接口用中文注释。
- PowerShell 必须用 `pwsh`（PowerShell 7），`Get-Content -Encoding UTF8`。
- 用 apply_patch 写文件。若工具不在当前工具表，已验证可直接调用其引擎；不要通过 apply_patch.bat 传多行参数（会损坏参数边界）：

```powershell
$patch = @'
*** Begin Patch
...patch...
*** End Patch
'@
& 'C:\Users\lin17\AppData\Local\OpenAI\Codex\bin\d23520d1e41bfb24\codex.exe' --codex-run-as-apply-patch $patch
```

上面程序路径可能随 Codex 升级变化；先读 `(Get-Command apply_patch).Source` 指向的 bat，解析其实际 codex.exe 路径，不要猜路径。所有文本保持 UTF-8。

- 主线程负责生产代码和 Gradle；子代理可以取证、复核或编写明确分配的独立测试。不要让两个会话同时修改此工作树。
- 已应用 skills：`kotlin-cangjie-frontend-parity`、`cangjie-cfir-llt-repair`。新会话按技能要求读官方语义与 Kotlin 对位，不能把本仓当前实现当语义依据。
- 官方仓颉参考必须使用 tag `v1.1.3`：原仓 external/cangjie_compiler 当前 checkout 是 v1.0.0。用 `git show v1.1.3:路径`。Kotlin 参考在原仓 `external/kotlin`。
- 所有 Gradle 使用下面的队列命令。工作树自己的 queue bootstrap 源不完整，不能改回直接 gradlew：

```powershell
java -jar 'D:\code\intellij\cangjie\gradle-queue-cli\build\libs\gradle-queue-cli.jar' --project-dir 'C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie' <TASKS> --continue --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8'
```

- 观察超时不等于进程结束。先确认同一 native handle 的终态，不能因没输出重启 Gradle。
- Git 使用 `git -c core.safecrlf=false ...`。曾反复出现零字节残留锁 `D:\code\intellij\cangjie\.git\worktrees\cangjie1\index.lock`。只有核实没有 git 写进程、锁已陈旧且为零字节后才能删；fsmonitor--daemon 单独存在不是写进程，活跃 git 操作不能清锁。

## 3. 整体实施比例

按 P0–P10 共 11 个阶段、完整满足验收条件的阶段等权计算，保守约 **9%（P2，1/11）**。这是阶段完成率，不是代码量或工时比例。P1 的旧“4/8”缺乏逐项验收台账，不再作为精确比例。

| 阶段 | 当前实况 | 尚未闭合的主要内容 |
|---|---|---|
| P0 取证 | 官方探针、46 个本地 tag 的版本表、高严重度复现索引已建立 | 早期预发布细节、部分宿主 harness/精确诊断证据 |
| P1 匹配时机与读取 | A1、Quest、锁外 common 签名准备、纯计划/求值器、调用侧惰性读取已推进 | 声明自身发布不可变计划、完整 family 聚合、旧可变存储/整组重跑删除；unused 红例 |
| P2 common 反序列化语义 | 已满足校正后的验收条件 | 不把与其独立的 P8 导出缺口算成 P2 未完成 |
| P3 合并视图 | 非成员/成员 tower 过滤、纯 nominal/extend 合并器、真实成员字段模型已实现 | 纯合并器接 CFIR、完整有效成员图、配对前后独立缓存、构造器/default 消费贯通 |
| P4 实现关系/实例化 | 有研究与旧实现 | 声明 owner 组、实例化成员图、继承与扩展规则整体修复 |
| P5 匹配判据 | 共享纯 matcher、泛型约束计算、成员参数字段/位保真已推进 | 完整类型等价、各声明种类 CFIR 计划工厂、模式/enum/primary 规则接入 |
| P6 检查器 | 部分基础事实已具备 | 官方条件、注解版本表、分层、全部有序事件消费与去重 |
| P7 生产 LL/IDE | 相位门和通用字段 Analysis 链路已有修复 | CJMP 模式生产装配、dependsOn/provider、真实 IDE/standalone；CJMP LL 测试仍注入模式 |
| P8 CJO/CLI | 角色/来源分离、真实默认辅助函数引用/owner/cache、成员参数位往返已修复 | Source 默认 helper 生成、非字面量/default 代码、跨包/common-only 导出、版本门与 CLI/诊断完整链路 |
| P9 resolve 附带修复 | CRLF、空块 Unit 等必要问题已修复 | 原计划 I1–I5 的整体闭合 |
| P10 收口 | 有多轮全量结果 | 全部实现后完整要求审计、parity 终审、最终文档；尚不能验收 |

## 4. 重要已提交工作

| 提交 | 内容 |
|---|---|
| `2ff6a879d` | 有序候选兼容性纯化，删除 context 写回回调 |
| `a815f2312` | 纯 CjmpMatchEvaluator，有序事件/部分默认转移/绑定规则 |
| `e0bffcfe7` | P2：COMMON 角色与 FROM_COMMON_PART 来源分离，共享候选资格 |
| `0d007400d` / `8459420c1` | 完整 CRLF lexer 支持及回归记录，孤立 CR 仍非法 |
| `915e19b23` / `1537a5cc9` | PSI/stub 空函数块事实、raw Unit、隐式签名修改失效 |
| `5971114f2` / `a9f12c83e` | A1 阶段前移、真正 Quest、LL common 签名锁外准备、全量记录 |
| `3fc3ac633` | matchSignature 与参数顺序检查分离 |
| `6d65d28ac` | CFIR 函数头到不可变计划的工厂与测试，尚未 runtime 集成 |
| `a76017b9b` | CJO desugars 指向 FuncDecl；helper owner/cache 发布顺序修复 |
| `52e580bcc` | CfirGenericConstraintCompatibility 共享纯泛型约束计算 |
| `93c33c78f` | 正向实际化 callable 查询、原 4 caller-first 修复、extend 成员两例与 unused 新红例 |
| `ff7b4061f` | CjmpContainerMergeEvaluator，nominal/extend 纯合并、真实字段 seed，15 新测试 |
| 本交接提交 | 下节完整字段模型迁移及验证记录；不是完成整个 CJMP 目标 |

## 5. 本轮字段迁移具体做了什么

此前 PSI/LightTree 主构造 let/var 被生成为 CfirProperty 和空 getter/setter；官方与 CJO 却是 VarDecl。不能在 CJMP 中按 fake source 把 property 临时当变量。本轮已整体改为 CfirFieldVariable。

- `cfir/cfir-tree/.../ClassMembers.kt`：参数 `correspondingField`、字段 `correspondingValueParameter`、两节点独立 `isMemberParameter`、一次绑定 API `bindPrimaryConstructorParameterField`。
- PSI 与 LightTree 的 `add/convertPrimaryConstructorParameterFields`：真实字段、无虚构 accessor、注解复制保持来源，字段 initializer 留空，默认实参仍在参数。
- `CjoPackageMetadataProducer` / `CfirDeclDeserializer`：独立写读 ParamInfo/VarInfo.isMemberParam，不能用 raw 引用是否存在推导二进制标志。字段仍为 VarDecl，不是 PropDecl。
- `CfirDeclaredValueMemberIndex`：从原 declared scope 提取唯一有效值成员规则；原始声明留给重定义诊断，普通 lookup 与初始化均使用同一有效存储。修复 primaryConstructor8 两入口，移除旧名字赋值封禁。
- 初始化/修饰符/注解/继承/赋值检查：删除主构造 property 的兼容分支和重复存储收集；当前 primary 的参数初始化对应实际 lookup 目标；secondary 不无条件预初始化。
- LL：字段/参数直接双向定位，不再按 ClassId 扫描反查；记录/诊断访问 field；`FieldFromParameter` 保留独立参数 PSI 身份，字段 owner 是 nominal。
- Analysis：完整 callable 通道包含字段；CaFieldSymbol 的源可以是 CjFieldVariable 或 CjParameter，mutability 取真实 isVar；FIELD pointer 按任意 nominal 的声明成员恢复。统一函数 builder 复用 named/constructor 的归一化。nominal equals/hash 对位 Kotlin KaFirNamedClassSymbolBase。
- 新测试：`PrimaryConstructorMemberFieldRawCfirTest`（4 入口），更新 `CompileTimeVisibleParameterRawCfirTest`（2 入口，保持 4 annotation slots/3 surfaces），`CfirPrimaryConstructorMemberFieldSerializationTest`（2 项），`CfirPrimaryConstructorFieldTest`（class/struct/annotation 3 项，字段 BODY 是首个语义请求，含 public pointer）。

对应源码的完整列表可由本交接提交的 `git show --stat` 获得。改动没有恢复 property 伪装、跳过测试或削弱既有期望。

## 6. 最新验证：注意分清通过率与目标完成度

| 验证 | 结果 |
|---|---|
| `:resolution.common:test`，纯合并器提交 | 57/57（含新 15 项），0 skipped，0 failed |
| Raw 主构造成员字段 | 6/6 |
| `:cfir:cfir-serialization:test` | 121 项：116 passed、5 skipped、0 failed |
| PrimaryConstructor + CommonSpecific 切片 | 313 项：229 passed、84 skipped、0 failed |
| 完整 CFIR（本轮） | 8963 项：8567 passed、396 skipped、0 failed |
| `CjmpTwoPhaseCompilationTest` | 19/19 |
| 最新完整 LL，末次 API 复核之后 | 145 项：144 passed、0 skipped、1 failed；字段新 3 项全过 |

完整 CFIR 快照：`cfir/analysis-tests/build/ffi-annotation-verification/20261001-member-field-full`。对比 `20260930-cjmp-a1-full`：0 regressed、0 new、0 removed。

首轮/第二轮字段切片：`20261001-member-field-first-slice`、`20261001-member-field-second-slice`；第二轮修复了 primaryConstructor8 的两项失败。

最后完整 LL 命令用时 3m30s（session 79322，已终止），XML 直接在 `analysis/low-level-api-cfir/build/test-results/test`。字段 suite 时间为 `2026-10-01T07:38:42.713Z`，3/3。失败 suite 是 `CfirCjmpCallerFirstDiagnosticsTest` 中唯一一个 unused 用例。组合全量命令用时 20m26s，退出码同样为 1；不能报告 BUILD SUCCESSFUL。

常用归档：

```powershell
.\tools\verification\cfir-test-result-ledger.ps1 -SnapshotName NAME -CompareTo ABS_SNAPSHOT
```

该脚本固定归档 CFIR，包含原始 XML zip、cases.json 和比较。后续定向测试会覆盖 test-results，先保存所需全量证据。

84 skipped 的已核实原因：无类型别名 fixture 被 `OnlyTestsWithTypeAliasesMetaConfigurator` 排除，不需要额外执行禁用 alias expansion 的配置。对应 84 个用例在正常 PSI/LightTree 均通过（168 次），另有目录覆盖检查；没有新增 skip 规则。

## 7. 下一个必须解决的红例

`analysis/low-level-api-cfir/testData/diagnostics/cjmpCallerFirst/uncalledImplementationCallerFirst.cj`

- common 声明 used()/unused()。
- caller.cj 只调用 used()；callee.cj 实现 used()；uncalled.cj 实现 unused()。
- 第一个语义请求仅收集 caller 文件诊断。
- 正确：零诊断，unused 也完成 CJMP 匹配，unused.body 仍是 CfirLazyBlock。
- 当前：`expected: <[]> but was: <[CFIR_NOT_MATCHED]>`，在 `CfirCjmpCallerFirstDiagnosticsTest.kt:113`。
- 官方 1.1.3 两段式编译已验证零诊断。官方 CheckCJMP.cpp:668–693 收集整个 package，1193–1200 完成全部 specific 匹配，1203–1214 才检查 common。

根因：common-side checker 直接读尚未完备的 `specificBindingsFor(unused)`，caller lookup 只触发 used 组。不能通过全模块 BODY 预热、测试预解析 callee 或跳过 unused 检查来消除失败。

## 8. 最终运行时架构：下一阶段的主线

现有 `CfirCjmpMappingStorage` 仍有正反向 ConcurrentHashMap、bind/record 写 API 和 beforePhase.clear；transformer 仍在单目标锁内重跑 sibling group。新增 getter 虽先 lazy resolve，最后仍读取旧 storage。这不是最终架构。

必须一起完成：

1. tree/declarations 上保存不可变、只属于当前声明的 plan/data；getter 推进 CJMP_MATCHING 后读取。substitution override 回到原声明。
2. matcher transformer 只计算/发布当前声明，不跨 sibling 写状态。LL common 依赖准备留在读写锁之外。
3. tree/session 仅暴露只读 aggregate 契约；resolve/cjmp 通过完整有序 family 的计划纯求值并缓存不可变结果。先解析全部计划，再进 cache 锁；key 包含 family、use-site session、版本策略及计划身份。
4. common 方向也要通过 provider 找到完整 family；明确 Complete(empty) 与尚未计算的区别。不能只从已知 specific 出发或枚举缓存。
5. 删除旧前向映射、写 API、可空组件/吞异常路径；不要保留并行第二真相。旧方法名可作为新单一 aggregate 的只读投影，但不保留旧数据源。
6. 默认参数读取 aggregate.defaults，包含失败候选先前已完成的部分转移，不能只读最终 common 对应物。
7. checker 重放 visits/events：Occupied 表示第二绑定，不能把变量合法覆写当 multiple；property/pattern 总失败仍可能有保留绑定。后置返回类型复核不改写历史匹配。
8. 完成 P3/C4 真正 actualized member graph 和独立缓存，发生在继承归并之前。tower 过滤不能恢复早期缓存已丢弃的候选；禁止将 CJMP getter 塞进 STATUS/TYPES buildIndex。

需覆盖的生产消费者：CfirCallResolver/MapArguments、TowerLevelHandler/CfirTowerResolver、class/extend member scopes、GenericInstantiation/Inheritance checkers、CfirCommonSpecificChecker/CommonSideFacts、CjmpDeserializedCommonSideReporter、CLI 与 LL/两段式入口。

## 9. 已有纯算法与缺失输入工厂

- `resolution.common/.../CjmpMatchPlan.kt` / `CjmpMatchEvaluator.kt`：Callable TryBind、Property 本体→getter→setter、有序默认值转移、static/global assignment、MergedMemberVariable seed、tuple/non-tuple、事件与访问历史。
- `CfirCjmpResolver.buildFunctionMatchPlan`：已冻结签名与参数事实，传入 commonDefaults、eligibility、before/after diagnostics 等；不能把后面参数失败折叠成签名失败。尚无生产 runtime 接线。
- `CjmpContainerMergePlan.kt` / `CjmpContainerMergeEvaluator.kt`：原始 nominal/extend 快照→有效/残留成员、逻辑 owner、依赖、导出抑制、non-exhaustive、merge history、真实 initialBindings。现在已具备正确 CFIR 字段模型来构造输入。
- `CfirGenericConstraintCompatibility`：纯约束域比较，可供 CJMP 提供 outer+own substitutor，checker 负责定位呈现。

仍缺完整 CFIR property/accessor、变量、enum、pattern 工厂及容器适配。不要只接函数、其余继续旧 runner。普通 enum variant 即使 native 是 VarDecl 也走 enum TryBind，不能当 global variable assignment。pattern 不能用扁平 bindingVariables 抹掉非变量项的停止边界。

关键官方语义：

- nominal 不是 first specific wins；保留 identifier 首个角色声明，后来的对侧可多次合并。generic/non-generic 不同跳过；N/M 都 generic 时仍合并，数量诊断在后面。
- common 全部成员并入 specific（包括普通/private）；logical owner 改为 specific，成员 doNotExport 重置 false。
- 只有 COMMON instance VarDecl（含真实成员参数字段）按名 seed；specific 字段换入原 slot，旧节点保留在 common 供 initializer 分析；普通 member-param 只去重、无实现 seed。
- 重复 specific 字段绑定当前 slot，可能形成 common→specific1→specific2 指针链；不能压成按名称取最后值。
- common-only 成员必须留在最终成员视图；property、function、constructor 不在字段 seed 阶段预绑定。
- extend 的 native 输入是 unordered_set、common 组是指针 set；纯算法接受显式顺序，不把文件名排序当成官方保证。
- CJO HAS_INITIAL 与实际 desugar helper 是独立事实；library 的默认 marker 不证明 helper 存在，绝不能再写 `desugars ?: parameter`。

## 10. 完整发现与失效的已核实边界

- `LLCfirProvider.getCfirFilesByPackage` 会通过 declaration provider 构建全部 raw 文件，不只是当前缓存。契约更精确的现成方法是 declarationProvider.getPackageFiles；支持 class-only/extend-only 文件、模块 contentScope。不要新建文件系统扫描兜底。
- 顶层 family 按 package/name，pattern 要按相交 binding 名字闭包；nominal 要保留全部同名声明，不用单值 getClassLikeSymbolByClassId 收 duplicate 集合；extend 原始输入从包文件直接取。
- `extendProvider.getAllExtends` 会触发 TYPES/索引更新，不是纯 raw 枚举入口。
- 模块身份使用 `CfirModuleData.equals`；LLCfirModuleData 已按 caModule 实现 equals/hashCode，不能再用 moduleData.name 或对 wrapper 用 ===。
- CfirDeclarationAttributes.copy 是浅复制；新 plan 不能从旧符号照抄到新符号。substitution override 应 unwrap 后读原 plan。
- 默认参数修改及省略返回类型函数的空块↔非空块修改走模块 OOB；依赖 session 同时失效。普通显式返回函数的 body 修改可保留签名 plan，但 post-body diagnostics 需失效。
- accessor 的 LL root 是 property；value parameter root 是包含 callable；pattern binding 通过 provider.getCfirPatternVariableForBinding 找真实 root，不能按名字猜。
- StateKeeper 不会自动保护新 attributes；成功后一次发布，或者显式纳入 rollback。

## 11. 参考与历史状态

- 主计划及 §13.1–§13.14：`docs/cjmp-framework-review-and-fix-plan-20260928.md`。
- 修复记录：`cfir/analysis-tests/REPAIR_LOG.md`。
- `.workbuddy/tmp/cjmp_probe113/review-20260928/`：P1-FRAMEWORK-MAPPING.md、P1-CANDIDATE-EVENTS.md、VERSION-EVIDENCE-20260929.md、P0-HIGH-SEVERITY-FIXTURE-INDEX.md、p1-cjc-20260929（12 探针）、p1-first-fit-20260929（11 探针）、ll-completeness-20260930。
- 官方 SDK：`C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie`。
- 未提交内容并未全部丢失：先前恢复并入 base `c56172652`，`.recovery/RECOVERY.md` 记录184个恢复的源/测试/文档文件。旧 workspace.xml 最终状态和部分二进制不保证完整；不要重新进行恢复/覆盖工作。
- 已知无关失败：PSI `ForeignAndAnnotationParsingTest.platformAnnotationSurfaceIsRetainedWithoutBuiltinKindFacade`，见 `docs/import-psi-group-verification-20260928.md:35`。文档聚合曾因两处 CJD 锚点及 module-catalog 缺 CLI 模块条目失败。不要因此偏离 CJMP 主线，也不能说全仓所有 check 已通过。

## 12. 接手后的第一步

1. 进入上述现有工作树，读 AGENTS/技能、本交接和主计划；核实 HEAD、真实 diff，保护无关 HTML 删除和生成文件状态。不创建第二个工作树、不重做恢复。
2. 确认本交接提交与字段迁移已保存；无需重跑刚完成的全部验证来“热身”。
3. 以 unused 红例为入口，实施完整声明计划发布与只读 family aggregate，并接入刚完成的容器合并/字段事实；同时迁移各声明种类与所有消费者、删除旧存储，不能用新预热补丁掩盖旧架构。
4. 先验证该红例（零诊断、unused 已匹配、body 仍 lazy）、旧 caller-first/成员/并发/关闭门、匹配组 eager/LL 一致性和两段式，再扩大到完整 CFIR/LL。
5. 按原计划继续 P3–P10，并及时提交；只有逐项证据证明整个目标完成才标记 goal complete。
