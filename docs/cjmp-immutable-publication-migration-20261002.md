# CJMP 不可变发布迁移：2026-10-02 当前源码调用面

本文记录下一集成批次的实际迁移边界，不代表已实现。完整目标仍为主计划 P0–P10 / A1–J1。
原始约束来自 `.workbuddy/tmp/cjmp_probe113/review-20260928/P1-FRAMEWORK-MAPPING.md` §4：
声明属性保存独立兼容性事实，确定顺序的组聚合裁决占用；两者不得混成受请求顺序影响的状态。

## 1. 已核实的当前问题

- `CfirCjmpMappingStorage` 用多个可变 map 保存同一前向状态，并公开 bind/record/clear 写事务。
- `CfirCjmpFamilySnapshot` 已改为构造时复制声明事实并脱离 live storage；但 `buildSnapshot(..., storage)` 仍依赖旧存储，尚未成为最终声明属性的发布入口。
- `publishSettledShadows` 写依赖 common 声明上的无 specific-session 身份 Boolean。
- runner 的组求值和结果翻译会写同名兄弟；访问一个目标的 CJMP 阶段不能证明只有目标被发布。
- `CjmpAttributes.getMatchedCommonForSpecific` 直接从 storage 读取；访问器和 pattern binding 的
  阶段根与真实结果键尚未统一。
- pattern 路径仍扁平读取 `bindingVariables()`；官方 tuple 直接元素上的非变量停止边界未落地。
- runtime 不消费 `aggregate.defaults`；`cjmpHasDefaultValue` 从最终 counterpart 推导默认来源，
  无法保留失败候选已经转移的参数前缀。
- LL complete 的提前完成标记、重入时返回部分结果与失败/取消后的完成协议仍需改写。

这些问题需要一次集成迁移；不能给旧表加属性镜像后继续维持两套真相。

## 2. 最终契约

1. 每个阶段根只发布一次完整不可变声明事实。null 仅表示未计算；空候选、失败和合法额外声明
   均有显式已计算结果。发布端不能回写同名兄弟或依赖 common。
2. 声明事实保存有序计划、每个 common owner 的映射及真实子声明身份；不保存依请求时序产生的
   SECOND_BINDING。占用、实现要求、参数转移与最终实现由同一确定顺序纯聚合裁决。
3. 属性/tuple root 拥有其访问器/binding 子结果。阶段和锁使用 root，查询使用真实原始 symbol，
   不能把全部子声明折叠到 root symbol。
4. pure evaluator 只读冻结计划和真实 merge seed，不请求 provider/阶段、不改 CFIR、不报告诊断。
5. cache 只能保存冻结输入的派生值。LL 的 `CfirThreadSafeCache` 使用 concurrent get-or-put，factory
   可以竞态执行；cache value 应为强 `CfirLazyValue`，胜出 cell 内才执行求值。cell monitor 内
   不得有依赖签名准备、lazy resolve 或 provider 查询。
6. 完整家族快照复制有序 events/visits、最终 implementations 和 owner 映射；不引用 live storage。
   历史 acceptedBindings 与最终 implementations 分别建模，VAR 覆写不能被旧 bind 再次拒绝。
7. STATUS 的 raw scope cache 只读结构；后期实际化成员图在继承合并之前形成，使用独立视图身份
   和缓存，static/receiver/substitution/parent 路径递归传递该身份。

## 3. 必须整体切断的写面

| 当前入口 | 最终归属 |
|---|---|
| storage clear 与 processor beforePhase 清表 | 删除；声明/session 失效生命周期管理结果 |
| bind / bindCommonExtendCounterparts / addCommonExtendCounterpart | 纯求值的 acceptedBindings / 最终实现 / merge owner 对位 |
| recordUnmatched / recordMismatch / recordMismatchState | 显式声明事实与 ordered visits |
| recordParameterMismatch / recordMissingImplementation / recordOccupied | 真实 ordered events，禁止从 root 返回值猜子声明锚点 |
| recordDuplicateSpecificExtend | 不可变 merge 事件 |
| writePlanAggregate 与 direct matcher mutable 翻译 | 删除；只发布当前根的不可变事实 |
| transformer 同名兄弟遍历写入 | 删除；完整组只作为纯计算输入 |

`recordMismatchState` 与 `recordMissingImplementation` 是当前发布器修正，仍属旧写 API，
本迁移必须一并删除，不能将它们当作最终架构。

## 4. 读面与消费者

- `CjmpAttributes`：单 root 惰性读取和真实子 symbol 查询；删除 settled common shadow Boolean。
- resolver：只读已完成 outer 的 raw 事实及逐 owner 映射；目标锁内不推进 outer/common/兄弟。
- LL CJMP 后置条件：验证 root 属性和 root-owned 子结果，而非多个表拼出的 hasResolutionResult。
- `CfirCjmpFamilyAggregate/Snapshot`：完整不可变快照；删除 buildSnapshot(..., storage) 与
  publishSettledShadows，完成前准备阶段必须在 monitor 外，失败/取消不得发布 complete。
- `CfirCommonSpecificChecker/CommonSideFacts`：消费同一 visits/events/最终实现；多实现依据实际
  Occupied 事件，不能用混合 accepted/rejected 的反向列表替代所有语义。
- `CfirMapArguments`、`CfirCallResolver`、CJO/default helper 消费者：读取 owning callable 的聚合
  defaults；删除从最终 common counterpart 推导默认值的 `cjmpHasDefaultValue`。
- `CfirGenericInstantiationChecker/CfirInheritanceDeepChecker/CfirExtendMemberScope`：消费真实
  实际化成员图；删除零散反向过滤与现场 counterpart signature 投影，不机械替换成早期 lazy getter。
- CLI deserialized-common reporter：非空聚合入口和完整结构声明集合；不保留 nullable storage。
- session 装配：删除 storage 注册和 nullable accessor；`cjmpHasCommonDefault` 移到声明语义层，
  它是独立语言事实，不能随旧存储类一起删除。

## 5. 同一集成批次的编辑顺序

1. tree 不可变契约、声明事实、root-owned 子结果、family snapshot。
2. 全声明计划：真实 callable/property/VAR/pattern、nominal/extend merge 输入及 initialBindings。
   tuple 逐真实 elements，非变量项停止；实例字段 seed 只来自符合资格的真实 VarDecl。
3. 锁外冻结依赖输入、canonical group、强 lazy cell 的纯求值入口。
4. runner 返回不可变事实，transformer 单目标发布，processor 删除 clear。
5. LL root designation、pattern binding owner、phase updater、StateKeeper/取消及属性后置条件。
6. raw/actualized scope 视图和缓存，在继承归并前完成成员输入实际化。
7. 全部诊断、调用默认值、实例化、继承、CLI/CJO 消费者迁移。
8. 删除旧 storage 类、所有写/nullable API、live snapshot、ThreadLocal 部分结果与 runCatching。
9. 迁移全部旧 storage 测试到属性/不可变快照/真实事件身份，并做完整验证。

这九步属于同一架构切换，不以保留新旧接口的中间版本作阶段验收。

## 6. 验证要求

- 任意兄弟首先进入 LL CJMP，仅目标属性/阶段发布；组求值稳定且一次，未用 common/caller body 保温。
- 单 common parent 内的候选按官方次序 first-fit，失败候选产生的诊断不会因后续成功而撤销；多个 parent 的规则按语言版本区分：1.1.x 单 CJO 输入不表达多 parent，alpha.20 起同一 specific 可继续匹配后续 common parent，并分别建立关系。不得将多 parent 关系归并成全局 first-fit。
- property 前缀绑定不回滚，三个键各自保持真实身份；tuple 非变量停止并保留已接受前缀。
- binding 首次请求路由到 pattern root，返回真实 binding 结果；明确 false/空候选重复查询不重算。
- VAR 最终覆写、merge seed 的重复字段指针链、common-only/普通/private 成员均保留真实语义。
- 两个 specific sessions 共享 common 不互相污染；依赖请求不在目标锁/cache monitor 内。
- 取消/失败不发布声明属性或家族 complete；已有 StateKeeper 不自动回滚新增属性，需显式纳入。
- raw STATUS 缓存不读匹配结果，后期视图在继承合并前形成且递归携带视图身份。
- 完整 LL、完整 CFIR、CLI 两段式及相关 resolve 测试；定向绿色不能证明整批迁移完成。

2026-10-02 取证由当前工作树源码、原始 P1 映射和只读 publication mapper 复审交叉核对。
本文没有将任何尚未实施项标记完成，也不更新历史总体百分比。

## 7. 下一步先闭合的 LL 输入时序

2026-10-03 主线程直接复核的源码事实：

- `CfirTypeResolveTransformer.resolveFileTypes` 只处理文件自身，`resolveClassTypes` 只处理容器
  头部，不能假设一个目标完成 TYPES 后同名兄弟的类型已物化。
- `LLCfirTypeTargetResolver.rawResolve` 的 callable 是独立 designated target；外围 class/extend
  只建立类型语境，不能用外围阶段代替目标成员阶段证明。
- `LLCfirStatusLazyResolver` 有 OnlyTarget/AllCallables 模式，class-like 单目标使用 OnlyTarget；
  部分成员 STATUS 请求会连带处理 callable，但包级函数没有可供此规则保证的 class owner。
- 当前 `LLCfirCjmpMatchingTargetResolver.doResolveWithoutLock` 显式推进 matchingGroup 兄弟到
  previous，并收集全组 common 隐式签名；transformer 随后仍循环发布兄弟。这是待删除的当前
  路径，不是已经解决单目标发布的证据。
- Kotlin `LLFirExpectActualMatcherLazyResolver` 只准备真实外围 class 的阶段，目标锁内调用
  transformer；`FirExpectActualMatcherTransformer.transformMemberDeclaration` 只写目标属性，
  包括空结果，没有同名兄弟发布。

因此不能直接把当前 matchingGroup 放进 lazy cell，也不能直接删兄弟准备后用未解析类型建全组计划。
实施前必须把单声明独立事实准备、调用/default 消费的纯组准备、checker 完整家族准备分别落到
真实阶段/锁入口，并核对原 P1 映射 §3 的不推进兄弟约束。若完整组需要前一阶段签名，必须证明
该准备属于明示语言依赖，而不是借 target hook 预热；不得扩大 same-phase 权限或进入兄弟 body。
这项协议尚未闭合，不能宣称不可变发布已可验收。

## 8. 2026-10-03 再核对：依赖准备不止 common 函数 IMPLICIT

以下来自当前实际源码，不是已经实施的协议。九步迁移不能只改 LL hook 和缓存包装。

| 当前读取/推进位置 | 核实的行为 | 必须切断的边界 |
|---|---|---|
| LLCfirCjmpMatchingTargetResolver.doResolveWithoutLock | 目标 read lock 中枚举 specific 同名组，锁外推进兄弟 previous；再收集全组 common 函数并推进 IMPLICIT | 单目标只准备自身的 common 依赖；兄弟属性准备属于显式组/家族消费者 |
| CfirCjmpMatcherTransformer.transformMemberDeclaration | 枚举同名组并逐个 matchDeclaration | transformer 只发布当前 root 的完整独立事实，不写兄弟 |
| MatchRunner.matchStaticOrGlobalVariableWithPlan | common VAR 候选推进 TYPES 后建计划 | VAR 候选签名在目标锁外准备，计划构建只读完成事实 |
| MatchRunner.matchPropertyGroupWithPlan | 每个兄弟的 common property 候选推进 TYPES；求值后写整个组 | 属性与访问器计划在原 root 发布；组计算不承担发布或 lazy resolve |
| MatchRunner.matchCallableGroupWithPlan | 所有 specific 组成员与 common function/enum 候选推进 TYPES | 单目标冻结自己的 function-like 事实；组只消费全部已经发布的冻结计划 |
| MatchRunner.matchPatternVariable | root 和 common VAR 候选推进 TYPES | root-owned 原绑定头在 TYPES 已完成；common 候选准备仍需从求值调用面移出 |
| MatchRunner.matchExtend / matchAgainstCandidates | common extend 或全部候选推进 TYPES | nominal/extend 的类型映射与 seed 输入先准备再冻结，不在 matcher 中请求阶段 |
| MatchRunner.isDuplicateSpecificExtend | getAllExtends 后把全部 extend 推进 TYPES，再按包/模块/key 判重 | 判重属于规范排序的冻结 extend 集合；单目标不得扫描并预热无关 extend |
| LLCfirCjmpFamilyAggregate.ensureFamilyComplete | 提前 completedPackages.add；ThreadLocal 重入返回 live snapshot；结束写 common shadow | 原子完成结果只能在冻结计划/纯聚合成功后发布；失败/取消不置完成，重入不能返回部分家族 |

`resolveCjmpSignatureTypes` 仍在 runner 内调用 lazyResolveToPhase(TYPES)。LL 标准入口在目标
CJMP write lock 内调用 transformer/runner，所以这些也是目标锁内的阶段请求；不能因为 TYPES
低于 CJMP 就把它们留在最终纯求值路径。迁移验收必须搜索并清除 runner/纯 cache factory 内
的 lazyResolveToPhase、provider 查询、storage 写事务。

对应 Kotlin 已重新读取 LLFirExpectActualMatcherLazyResolver 与 FirExpectActualMatcherTransformer：
前者准备真实 outer class，后者计算并设置当前 member 的 expectForActual（含空结果）；没有兄弟循环。
仓颉 common 隐式返回依赖仍按原 P1-FRAMEWORK-MAPPING §3 在短 read lock 外准备。

tree 模块已经 api 依赖 resolution.common，可以在原 CjmpAttributes 所属层承载冻结的计划事实，
无需新增反向模块依赖。现有 CjmpMatchEvaluator 的 initialBindings、defaults、ordered visits/events
已经是纯输入/输出表面；它们必须由真实 merge seed 与完整冻结计划接线，不能继续用 mutable storage
翻译结果，也不能在 cell factory 临时枚举 CFIR。强 CfirLazyValue 只执行纯 evaluate；归属索引、
签名准备、规范排序和完整输入收集均在进入 cell monitor 之前完成。

本轮诊断/声明头修复仅补齐真实输入的归属；不可变声明发布、成员图和所有消费者仍未迁移。

### 8.1 2026-10-10 LL 锁外输入准备复核

本节更新 §7–8 中 LL 当前源码行为；上文保留其 2026-10-03 的历史审计记录。

- `doResolveWithoutLock` 仍在 target read lock 中捕获 matching group，随后在锁外推进组成员的 previous 阶段。function/enum-constructor 路径在锁外为每个 root 收集 common 候选并冻结 callable 计划；已缓存组结果时传递规范组身份快照。
- callable 以外的 class-like、extend、property、pattern、变量与 constructor 路径现在也在锁外准备逐 root common 候选、所需签名阶段和外围类型参数映射。目标写锁内只匹配/发布请求 root；prepared runner 分支不重复查 provider 或请求签名阶段。eager 路径保留原同步准备行为。
- specific extend 判重先筛同模块、同包的 specific sibling，再解析这些候选的 TYPES 并比较 extend key；重复目标在 common 候选查找前短路，不再推进全部 extend。
- 定向验证队列 `b0e87523` exit 0：CFIR resolve CJMP 52/52、LL CJMP 45/45，0 失败、错误或跳过。

这项修复切断了 LL matcher 的候选查询/签名推进对目标写锁的依赖，但没有替换 storage 权威：声明结果仍由 `CfirCjmpMappingStorage` 的可变写表承载，完整家族、checker、默认值与 actualized-scope 消费者仍读取旧真相。单 root 不可变事实发布及旧 API 删除仍须按第 5 节作为同一集成迁移完成。

## 9. 2026-10-03 成员 scope 与默认值的实际消费边界

测试输入冻结期间主线程重新读取以下调用面。原九步仍是同一切换；以下均为未实现要求。

| 当前入口 | 已核实的缺口 | 最终消费者必须取得的输入 |
|---|---|---|
| CjmpAttributes.getMatchedCommonForSpecific | 请求 CJMP 后读 mutable storage | 当前 root 的不可变独立声明事实；不能冒充组内最终实现关系 |
| CfirActualizingScope.actualizeCjmpCandidates | 候选逐个读取上述 counterpart，收集 hiddenCommon | 冻结组的最终 implementations；保留原候选与 lookup provenance |
| CfirExtendMemberScope.buildIndex | 建索引时调用 isCjmpShadowedCommonDeclaration，读 common shadow 或 live storage | 在索引/继承归并之前准备好的 actualized 成员输入；raw 结构索引不得读匹配结果 |
| CfirGenericInstantiationChecker / CfirInheritanceDeepChecker | 多处现场按 isCjmpShadowedCommonDeclaration 过滤 | 与普通成员解析同一 actualized 成员图及来源身份，不能另建反向过滤真相 |
| cjmpHasDefaultValue | 从最终 counterpart 的同位置参数推导默认值 | owning callable 聚合中的 defaults，包含先前失败候选已经转移的前缀 |
| CfirMapArguments / CfirCallResolver | 多个参数适用性、缺参、默认参数计数入口消费上述 bool | 同一个冻结 defaults 来源事实；不能把先前参数转移丢失为“未匹配则无默认” |

对应源码定位：CfirActualizingScope.kt:71、CfirExtendMemberScope.kt:178、
CfirGenericInstantiationChecker.kt:999/1007/1466/1490/1508、CfirInheritanceDeepChecker.kt:1783/1862、
CfirCjmpMappingStorage.kt:438、CfirMapArguments.kt:573/584/608/754、CfirCallResolver.kt:774/2375。
这些位置用于迁移取证，不是硬编码语义。

已经逐项核对的缓存/构造边界：

- CfirScopeProvider.getUseSiteMemberScope 当前只有 klass/useSiteSession/scopeSession 三个参数。
  Kotlin FirScopeProvider/FirKotlinScopeProvider 还有 memberRequiredPhase；Kotlin 在 getOrBuild 之前
  lazyResolveToPhaseWithCallableMembers，在 factory 内先 declaredMemberScope，随后
  declaredMemberScopeDecorator，最后构造继承输入与 FirClassUseSiteMemberScope。
  当前 CfirCangJieScopeProvider 不具备这个明确的成员阶段参数和 declared scope decorator。
  不能把晚期 body tower 候选过滤宣称为与这个“归并前成员输入”边界对齐。
- CfirUseSiteMemberScopeKey 当前只有 useSiteSession/classSymbol；typealias constructor key 只有
  session/typealias symbol，均没有 raw/actualized 视图身份。
- StaticScopeForQualifierTypeKey 当前有 classId/qualifierType/memberScopeKind；builtin key 有
  targetKey/qualifierType，也没有视图身份。只加 nominal 主缓存键不会隔离 static 与 builtin 路径。
- CfirClassUseSiteMemberScope 的 createForType、withReplacedSessionOrNull、父类型 scope 构造和
  collectNames 父递归都会重新创建 scope；typealias constructor 展开也直接构造该类。
  view 必须沿这些真实路径传递，而不是仅包最外层查询结果。
- 当前类还保留“兼容旧调用点”的构造器；最终不能把它作为缺失 view 的隐式入口继续保留。

Kotlin 参考已直接读取：compiler/fir/providers/.../FirKotlinScopeProvider.kt 的
getUseSiteMemberScope（准备在 cache 外，declaredMemberScopeDecorator 在继承构造前），以及
compiler/fir/resolve/.../FirActualizingScope.kt（非成员候选过滤）。仓颉 nominal/extend 合并成员图
属于官方 MergeCommonIntoSpecific/MergeCJMPExtensions 语义；需保持真实变量 seed 与来源身份，
不能复制 Kotlin 不存在于仓颉的 delegated/data/value class 行为。

这些证据进一步限定 §7：单声明发布入口不能顺便建立全部兄弟的实际化 scope；完整家族/组的
准备和成员图准备必须在各自显式消费者入口、目标锁与 cell monitor 外完成。它们的最终协议和
实现仍未闭合，不能把本节取证当作迁移完成。

## 10. 2026-10-03 声明属性与阶段失败的实际事务边界

本节核对当前实现，限定第 5 步的恢复范围；尚未实现新增 CJMP 属性事务。

- `LLCfirLockProvider.withLock` 在锁获取循环检查取消。action 抛出普通异常时，finally
  将目标恢复到原解析阶段；它不保存、恢复声明属性，也不恢复已更新的关联声明阶段。
  `PartialBodyAnalysisSuspendedException` 是 body 成功挂起的专有分支，不能用作 CJMP 失败协议。
- `CfirDeclarationDataRegistry.DeclarationDataAccessor.setValue` 直接写 attributes 的槽位；
  赋 null 删除该键。`CfirDeclarationAttributes.copy()` 只复制数组容器，值对象仍共享。
  因此阶段恢复不等于结果恢复，数组副本也不能把可变结果变成不可变结果。
- `resolveWithKeeper` 只恢复 DSL 显式登记的值。现有 CJMP resolver 没有调用它；现有
  pattern keeper 保存原 binding 的结构和类型，但没有新增 CJMP 属性的恢复规则。
  单 root 的发布值需要作为一个完整值登记，子结果保留原 symbol 身份，不分别回写兄弟属性。
- `LLCfirTargetResolver.performCustomResolveUnderLock` 的顺序是 action、
  `updatePhaseForDeclarationInternals`、锁释放发布 root phase。若 keeper 仅包 transformer，
  它不覆盖随后 phase updater 的异常；不能用这样的测试宣称整个声明事务已覆盖。
- 当前 `LLCfirPhaseUpdater` 的 CJMP 路径仅递归处理类型参数、值参数、访问器和 pattern binding，
  没有 provider 查询、lazy resolve 或取消检查；body 遍历只在 BODY_RESOLVE 分支发生。
  最终设计需明确这一结构更新是否保持无外部失败点，并验证原子属性发布与原子 root phase
  发布之间的顺序。不能为掩盖该边界新增全局异常吞噬、手动 phase 复位或第二份结果表。
- Kotlin 的 `LLFirExpectActualMatcherLazyResolver` 也直接在目标锁内运行 transformer；
  `LLFirTargetResolver` 同样在 action 之后更新声明 internals。它没有 CJMP 的 tuple 子结果或
  家族 complete 协议，不能据此断言仓颉新增属性自然获得恢复保护。

因此迁移验证要区分：依赖准备失败（未进入发布）、纯求值失败（不写目标）、目标属性 action
失败（显式 keeper 恢复）、正常结构阶段更新及 root 发布、家族纯聚合失败（不发布 complete）。
失败后重试必须从完整冻结输入重新求值，不能把缓存存在、root phase 恢复或旧 partial snapshot
当作完成证据。强 lazy cell 的纯求值不允许调用属性写入或阶段更新；声明事务归 LL 目标锁管理。

## 11. 真实容器 seed 的依赖事实与准备时序

2026-10-03 继续读取官方 v1.1.3 和当前生产入口，确认以下输入边界：

- `include/cangjie/AST/Node.h` 的 `Decl.dependencies` 是变量依赖的声明指针列表。
  `InitializationChecker::CheckInitInVarDecl`（`src/Sema/LegalityOfUsage/InitializationChecker.cpp`）
  只对实例成员的初始化器开启依赖收集，检查结束后将真实依赖保存到 VarDecl。
  它不是仅由字段名称、显式类型或主构造参数形状推导的列表。
- `CheckCJMP.cpp` 的 `UpdateVarDependencies` 在每次容器合并后更新有效成员依赖；每个依赖
  有 specificImplementation 时仅替换这一条边。重复字段的实现链不求传递闭包。
  这与现有纯 `CjmpContainerMergeEvaluator` 的 dependencies 输出一致，但不证明生产已经接线。
- common 已独立编译后的依赖与 specific 尚未检查初始化器时的依赖不是同一准备状态。
  不能为了冻结 specific 的合并输入提前推进它的 IMPLICIT/BODY，也不能无条件给 common
  的序列化依赖填空列表。需要按各自声明生命周期保存已经存在的事实，并明确未收集状态。
- `ClassMembers.kt` 已有独立 `isMemberParameter` 属性与原 parameter/field 关联；
  CJO producer/deserializer 分别处理 ParamInfo/VarInfo 的成员参数标志。
  合并工厂应读取该属性，不能把源码关联是否存在当作库字段的成员参数身份。
- 当前 CFIR 生产源码没有调用 `CjmpContainerMergeEvaluator`；runner/resolver 注释明确记录
  实例字段仍走 direct 路径。纯测试中 `actual container merge seeds satisfy the member variable
  evaluator contract` 使用真实纯容器结果验证 seed，但未覆盖 CFIR 的输入采集和生产消费者。
- 当前 CJO writer metadata 有 dependencies 字段；当前声明 deserializer 未找到读取
  `Decl.dependenciesLength` 的路径，CFIR tree 也未找到对应的声明依赖属性。
  下一批必须继续核对 FullId 解析、common 产出与初始化检查 owner 的端到端来源，
不能把 metadata schema 字段存在当作已恢复真实依赖的证据。

同日补充有效 cjc 1.1.3 实测：`review-20261003/instance-dependencies` 的 common 与 specific
均 exit 0。common CJO 的 y 保存 `FullId(pkgId=-2,index=2)` 指向 x；specific CJO 不保存这些边。
inspect_dependencies.py 按官方 ModuleFormat.fbs 固定槽位读取，未猜字段；命令和失效环境尝试
分别记录于该目录 README。DLL 未执行，原生产物不混入源码提交。

官方 ASTWriter::CollectInitializationDependencies 在目标不计划序列化时递归收集该目标依赖，
使用 visited 阻止循环；SaveDecl 仅在 serializingCommon 时写入。ReferenceLoader 的
LoadDeclDependencies 仅在 deserializingCommon 时通过 GetDeclFromIndex 恢复真实声明。
因此 CJO 恢复的不总是源码中的直接边；不能按短名称重建、无条件保存 specific 的边，或把
writer 的递归投影与 MergeCommonIntoSpecific 的单边替换混成同一个“传递闭包”算法。

`src/AST/Utils.cpp::GetVarsInitializationOrderWithPositions` 消费 FROM_COMMON_PART 字段的
SPECIFIC 依赖，确定合并后的实例字段初始化顺序。第 7 步还必须迁移初始化消费者，使其使用
同一合并成员图与替换后的真实依赖；只让 scope 查找看到 common-only 字段不足以完成 P2。
上述直接边保存与两段式成功已有实测；未序列化中间节点投影和具体排序目前是官方源码证据，
仍需对应生产实现及验证。

这些事实限定第 2、6、7 步的接线：物理 owner、合并后 logical owner、保留旧字段、真实 seed
及依赖替换必须来自同一次容器纯求值；调用与初始化消费者不能分别按名字重建实现关系。
本节没有新增生产实现，也没有把单声明发布协议或九步迁移标记完成。

## 12. 纯求值输出的集合发布约束已加固

2026-10-03 修改两个平台中立 evaluator 的结果构造：全部外层 map/list/set，以及 defaults、
members、dependencies、acceptedBindings/initialBindings/merges 的嵌套 list/map，均复制后用
不可写的集合视图发布。消费者强转成 MutableMap/MutableList/MutableSet 也不能改写结果。
真实 symbol 身份、候选动作顺序、seed、默认转移与成员图内容不变；没有新增配对真相或缓存。

新增两项测试分别验证纯匹配结果与真实字段合并结果：输入集合后续改写不污染输出，所有集合
层级上的改写均被拒绝。queue 093f8bce 的定向 36/36 通过；queue 634f0528 已 exit 0（4m48s），
完整 resolution.common 59/59、CFIR resolve 的 Cjmp 消费者 52/52，均 0 failed/skipped。

这项改动只实现结果集合本身的发布不变量。冻结计划采集、声明属性、强 lazy cell、完整家族
完成协议、真实生产 merge seed 与实际化 scope 仍未迁移；第 5 节九步的整体验收要求不变。
此前 8968 条完整 CFIR 快照早于此项新输入，不能冒充新输入的全量验收。

## 13. 容器成员图的初始化排序消费者：共享层已实施，CFIR 接线未完成

2026-10-03 新增平台中立 `CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions`，
直接消费 `CjmpContainerMergeResult.members/dependencies/memberFacts`。原成员资格、FROM_COMMON_PART
和 begin 排序名次与同一合并图发布；memberFacts 的 map 及原 dependencies 列表均禁止写入。
它按官方 v1.1.3 src/AST/Utils.cpp 保留 common 顺序，用已替换的 SPECIFIC 依赖推进 specific
源码前缀，并保留途中普通字段。返回 position 仅计真实实例 VarDecl，不包含 callable/property/static。
没有 provider/阶段请求，没有依赖传递闭包，不回写原成员图及其保留的旧字段。

官方 src/Basic/Position.cpp 的 operator< 只比较 line/column，不包含 fileID。前端冻结 sourceOrder
必须遵循这个比较关系，不能使用包内文件顺序或合并后的字段槽位代替。FROM_COMMON_PART 是来源位，
不能用 COMMON 修饰角色替代；common part 的普通/private 字段也属于 common 初始化序列。

RecordInstanceVariableUsage 不限制 target 的物理 owner，因此容器外依赖的 SPECIFIC 位由调用方
显式冻结为 externalDependencyRoles。该输入不允许覆盖 memberFacts；未提供的依赖身份会失败，
不能被视为普通依赖。如果真实 SPECIFIC 依赖无法在剩余 specific 序列找到，保留官方 CJC_ASSERT
的不变量失败，不添加退化排序。依赖角色采集必须在最终强 cell 的 monitor 外完成。

新增 6 项测试覆盖真实 merge 替换后的边与原槽位、普通 specific 前缀、来源与修饰角色分离、
真实实例变量过滤、重复依赖、未提供及外部依赖角色。原集合边界测试扩展至 memberFacts。
初批 queue 6448bd62 / handle 39351 已 exit 0（1m9s），容器测试 21/21。
初批完整 queue afa8e964 / handle 45577 已 exit 0（5m28s），resolution.common 64/64、CJMP resolve 52/52。
外部依赖契约是后续新输入：queue 5a184292 / handle 20067 已终止，exit 0，12m47s。
最新共享层新鲜 XML 65/65（UTC 10:18:27），CJMP resolve 新鲜 XML 52/52（UTC 10:30:34–35），
均 0 failed/error/skipped。当前无该作业运行；更早全量快照仍不是本批输入的全量验收。

新的原始官方探针 `review-20261003/instance-dependency-order`：两段编译 exit 0，正确 SDK 运行环境下
EXE exit 0，验证 before=7、specific x=8、common y=15、specific after=15。common CJO 保存私有
hidden 本身，hidden→x 与 y→hidden 都是直接 CURRENT FullId；specific CJO 不保存依赖。
所以该例证明真实初始化关系，不证明未序列化节点的递归投影；不能以 private 作为省略判据。
README 区分第一次错误 runtime 路径导致的加载失败和修正环境后的成功，原生产物不提交。

生产 CFIR 尚未调用容器合并与初始化排序求值器。本节是共享算法与其证据，不是第 2/6/7 步接线验收。
下一批仍需：按声明生命周期区分未收集与已收集空依赖；common CJO 按 FullId 恢复 canonical
声明身份；common producer 按真实 serialization index 投影依赖；初始化消费者从同一实际化成员图
取得排序。不能为了 specific 依赖预热 BODY，也不能在单字段反序列化锁内递归物化尚未构造完的 owner。
九步迁移、完整验证与最终 gate 均保持未完成。

本轮还核对了 binary 的实际身份风险：CfirDeclDeserializer.convertVariableOrEnumConstructor 通过
callableIdForCurrentOwner 和 dispatchReceiverTypeForCurrentOwner 构造字段身份，两者读取当前
class-like owner 栈。若在空 owner 栈上直接 deserializeDecl(字段 index)，会得到包级 CallableId
和空 dispatch receiver，随后该错误节点还会进入 context.declCache 的 canonical 索引。
因此恢复 FullId 不能仅对 peer 字段新建 deserializer；必须经当前 provider 的同一 package context
先物化其真实容器，再从同一 declCache 取得字段。跨包也不能另建 context 或走 source-preferred 的
短名称查询。现有 convertClass 先反序列化 body，最后才绑定 class symbol；字段 pre-publication
hook 中递归加载 peer/owner 会进入尚未完成的 owner 构造。这是后续恢复接线的实际边界，尚未实现。
