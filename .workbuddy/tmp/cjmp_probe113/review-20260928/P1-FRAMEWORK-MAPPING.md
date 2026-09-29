# P1 框架映射与设计预审（2026-09-29）

角色：KotlinFrameworkMapper / KotlinParityGatekeeper。范围：P1（A1、A2、A3、F6、F8、J1、I6）及必须一并验证的 G1/G2。本文是只读源码审查的产物；未修改生产代码，未执行 Gradle。路径均相对 `D:/code/intellij/cangjie`，行号为本次读取状态。

## 1. 预审结论

以下设计可进入实现：声明属性保存独立兼容性结果，单个声明在自身锁内一次发布；符号访问器先推进 CJMP_MATCHING；源码 common 依赖在 CJMP 目标锁外完成隐式签名，再对任一侧仍然真实为 Quest 的类型延后返回兼容检查；第二绑定在 checker 聚合阶段按官方顺序判定；LL 始终走通用阶段锁，模式只门控 transform。

**2026-09-29 实测修订：本文件早期版本中“按原始省略返回类型永久视为 Quest”和“任侧 Quest 可放行所以无需 common 依赖准备”两条建议已撤回。** 以第 3 节经 authority 确认的 12 组实测和锁链为准。

以下方案必须拒绝：

- 在现有 session 前向 map 外再包一层惰性 getter，继续保留双份真相。
- 在目标锁内重算/写入同名兄弟；通过反向表的占用状态决定独立匹配是否成功。
- 阶段前移后继续在 matcher 的 CJMP 锁内推进任一侧到 IMPLICIT_TYPES；源码 common 的 IMPLICIT 准备应在该锁外。
- 用 `coneTypeOrNull == null` 表示 Quest，或把错误类型当 Quest 放行。
- CJMP 内自行按 ClassId 找 owner，或依赖当前 `CfirProvider.getContainingClass()` 的 ClassId 优先旁路。
- 门关闭时不推进阶段；建 session 后重注册模式；用文件名/模块名/声明形状推断模式。
- checker 直接把尚未完整解析的反向表当作全量事实。

预审通过的是上述设计边界，不是尚未产生的最终补丁。第二绑定的多候选重放、provider owner、模块模式装配是实现后的必审项。

## 2. Kotlin 精确对应

| 职责 | Kotlin 文件、声明与行号 | 本仓对应位置 |
|---|---|---|
| 配对阶段 | `external/kotlin/compiler/fir/tree/src/org/jetbrains/kotlin/fir/declarations/FirResolvePhase.kt:150–183`：STATUS → EXPECT_ACTUAL_MATCHING → CONTRACTS → IMPLICIT_TYPES_BODY_RESOLVE | `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirResolvePhase.kt`：EXTENSIONS → CJMP_MATCHING → IMPLICIT_TYPES |
| 属性 key/值 | `.../tree/.../declarations/ExpectActualAttributes.kt:21,23,35`：`ExpectForActualAttributeKey`、`ExpectForActualMatchingData`、`FirDeclaration.expectForActual` | `cfir/cfir-tree/.../declarations/CjmpAttributes.kt`（新文件）；现成 `CfirDeclarationDataRegistry.data`，不要自建注册器 |
| 惰性读取 | 同文件 `:50–62`：符号 `expectForActual` getter 先 lazy resolve，再读声明属性；single-match helper 在其上实现 | 同一属性文件提供符号读取入口；消费者不得访问 raw 属性或 session 前向表 |
| 单声明写入 | `.../resolve/.../transformers/mpp/FirExpectActualMatcherTransformer.kt:103–115`：先计算 `findExpectForActual`，最后只写目标声明属性；空结果也发布 | 现有 `CfirCjmpMatcherTransformer.transformMemberDeclaration` 只处理传入声明；删除兄弟重跑 |
| 候选匹配 | `.../transformers/mpp/FirExpectActualResolver.kt:31–109`：返回兼容性结果，不用全局占用表决定匹配 | 本仓 `CfirCjmpResolver` 与 matcher 保存官方有序 first-fit 语义；独立计算，最后一次发布 |
| 外围类公共入口 | `external/kotlin/compiler/fir/providers/src/org/jetbrains/kotlin/fir/resolve/ContainingClassUtils.kt:44–46`：`moduleData.session.firProvider.getContainingClass(this)` | 本仓同包同名文件应属于 `cfir/providers`；目前误放 `cfir/resolve` |
| provider 默认 owner | `.../providers/.../resolve/providers/FirProvider.kt:60–67`：callable 的 `containingClassLookupTag()?.toSymbol(symbol.moduleData.session)`；其余符号按类型分发 | 修 `CfirProvider.getContainingClass`；不能保留 ClassId 优先、lookup-tag 兜底的本地实现 |
| LL 精确 owner | `external/kotlin/analysis/low-level-api-fir/.../providers/LLFirProvider.kt:106–112`：PSI containing-class calculator，再走标准 provider | 本仓 `LLCfirProvider.kt:171–177` 已有此形状；保留 override，使类重声明仍能定位真实宿主 |
| LL 门 | `.../transformers/LLFirExpectActualMatcherLazyResolver.kt:46,49–54,65–69`：固定 enabled；先 outer；仅锁内 transform 受门控 | 本仓 `LLCfirCjmpMatchingLazyResolver` 删除早退与自行 replace phase |
| 阶段锁 | `LLFirTargetResolver.kt:279–307,336–347` → `LLFirLockProvider.kt:50–58` → `withLock(updatePhase=true)` | 本仓通用锁链已经存在，不需要新锁/特殊阶段分支 |
| 锁外依赖准备 | `external/kotlin/analysis/low-level-api-fir/.../transformers/LLFirAnnotationArgumentsLazyResolver.kt:96–116`：`doResolveWithoutLock` 中 read-lock 收集依赖，退出 read-lock 后 lazy resolve，最后返回 false 进入常规写锁 | 用现成 hook 实现 common 签名准备；不能在 read-lock block 内推进 IMPLICIT |
| 默认参数读取 | `external/kotlin/compiler/fir/providers/src/org/jetbrains/kotlin/fir/declarations/declarationUtils.kt:289–313`：`FirFunction.itOrExpectHasDefaultParameterValue(index)` | providers/declarations 中的 `CfirFunction.itOrCommonHasDefaultParameterValue(index)`；通过符号惰性入口读取配对 |
| actualizing | `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/scopes/impl/FirActualizingScope.kt:61–84`：从 actual 正向得到 matched expect，再过滤 | resolve scope/tower 消费点从 specific 候选集合正向过滤；不按 common 查反向表 |
| 公共注册 | `external/kotlin/compiler/fir/entrypoint/.../session/ComponentsContainers.kt:99`；非空 accessor 在 `ExpectActualAttributes.kt:113` | 本仓 `cfir/entrypoint/.../session/ComponentsContainers.kt:76` 已注册现有 storage；新只读聚合接口亦须统一注册 |

Kotlin processor/transformer 共居 `FirExpectActualMatcherTransformer.kt`，resolver/context 在 `transformers/mpp`。本仓现有 `transformers/CfirCjmpMatchingProcessor.kt`、`resolve/cjmp/*` 分拆是历史结构，不能把它声称成“文件/包完全相同”。若本轮保留仓颉 CJMP 专有 matcher 拆分，应在计划记录官方 first-fit、extend、Quest 等明确职责原因；不要新增没有 counterpart 或官方职责的中转层。

## 3. 不产生循环的调用链（已按新增实测修订）

### 3.1 authority 确认与亲自读取的证据

已向 `CangjieSemanticsAuthority` 确認并读取 `p1-cjc-20260929/README.md`、以下两组原始 JSON。该目录共 **12 组** cjc 1.1.3 实测，不是早期的 11 组：

- `common_value_specific_empty/specific.diagnostics.json`：common 先编译得到 Int64，specific 空体，只有 `sema_not_matched`。
- `common_empty_specific_implicit_value/specific.diagnostics.json`：common Unit，specific 非空隐式返回 Int64，只有 `sema_return_type_incompatible`。
- `common_implicit_specific_explicit_bad`：common 原来省略返回但 CJO 已具体 Int64，specific 显式 String，仅 specific NOT_MATCHED。
- `specific_implicit_error`：specific 初始 Quest 可配对，体解析后 absent 未声明；结果为 UNDECLARED_IDENTIFIER + RETURN_TYPE_INCOMPATIBLE，没有 NOT_MATCHED。

authority 的官方源码依据：v1.1.3 `src/Sema/PreCheck.cpp:1774–1802` 初始 retTy 为 Quest；当函数体存在且空时在 :1795–1796 改成 Unit，再构造 FuncTy。`TypeManager.cpp:996–1003` 允许任一侧**当前真实 Quest**；它并不把声明原本省略返回类型永久等同 Quest。common 已独立编译成 CJO 时其 FuncTy 已具体。

因此：源码 LL common 为满足 CLI/CJO 一致性必须在 specific 匹配前完成依赖隐式签名；specific 只做 PreCheck 对应的空体→Unit 签名规则，不能提前解析非空函数体。matcher 使用当前类型状态，不能使用 `hasImplicitOrInferredReturnType()` 或 `source == null` 永久豁免。specific 正确阶段链已经保证本次匹配先于其体解析；完成匹配后结果不可因推断完成或后续错误而重算。

### 3.2 精确锁链与 hook 位置

本仓实际链：

1. `LLCfirModuleLazyDeclarationResolver.lazyResolveTargets:207–223` 逐 phase 调 runner。
2. `LLCfirLazyResolverRunner.runLazyResolverByPhase:26–30` 只取得可重入 globalLock，再调用 lazyResolver.resolve；它**不设置** currentTransformerPhase。
3. `LLCfirLazyResolver.resolve` 创建 target resolver，再调用 resolveDesignation。
4. `LLCfirTargetResolver.performResolve:302–328` 先 `resolveDependencies(target)`，再调用 `doResolveWithoutLock(target)`；只有 hook 返回 false 后才进入 `performCustomResolveUnderLock` / `withJumpingLock`。
5. `performCustomResolveUnderLock:359–369` 通过 `LLCfirLockProvider.withWriteLock` 进入 CJMP phase checker 和目标锁。
6. `LLCfirLockProvider.withReadLock:74–82` **同样会把 checker 置为 CJMP**，其 block 退出时才恢复原始 caller phase；它不是可在其中推进高阶段依赖的“无阶段读”。

直接参照 Kotlin `LLFirAnnotationArgumentsLazyResolver.kt:96–116`：在 doResolveWithoutLock 中用 withReadLock 只收集依赖符号，退出后再 lazyResolve 依赖，返回 false，让标准 performResolve 取得写锁。无需新增 framework hook、清空 ThreadLocal、放宽契约或手动 replace phase。

### 3.3 推荐的具体流程

1. 维持 outer nominal/extend 先配对的 designation 规则；目标先到 EXTENSIONS。
2. `doResolveWithoutLock` 在 enabled 且目标可配对时，通过 `withReadLock(target)` 读取目标已完成阶段的签名/真实 outer，收集 common 候选符号快照。这里不能调用任何 IMPLICIT 推进，也不能写匹配结果。
3. 退出 read-lock block 后，按声明站点 common session 将确实需要隐式签名的 common callables 推到 IMPLICIT_TYPES。仅处理 common 候选，不推进 target、同模块 specific 兄弟、全部 extend 或 common nominal 的全部成员。
4. hook 返回 false；正常 `performResolve` 获取目标 CJMP write lock，matcher 只消费准备好的依赖签名、计算单声明结果并一次发布。也可以在 hook 中用 `performCustomResolveUnderLock` 传入不可变候选快照后返回 true；若采用此形状，必须实际调用该方法，不恢复 disabled→true 的旧缺陷。
5. 并发期间另一个线程先完成目标，标准锁跳过 action 是合法完成；不要以“收集到空列表”判定无候选并写结果。Kotlin 示例使用 processed 标志区分 read-lock action 未执行，主线程可原样采用其并发完成分支，或返回 false 让标准锁复查。
6. COMMON 模式的依赖走空 CJMP transform 但仍推进阶段，随后完成自己的 IMPLICIT_TYPES；因此 G2 必须同步修，不能让 common 停在 EXTENSIONS。
7. specific 的匹配完成后才允许其 IMPLICIT_TYPES。specific 初始仍为 Quest 时接受配对；已为 Unit/显式具体类型则正常比对；显式错误类型不能伪装成 Quest。
8. checker 在体解析后验证最终返回关系。`specific_implicit_error` 证明不能因最终错误而删配对，也不能无条件 `containsErrorType()` 后跳过全部后检查；当前 `CfirCommonSpecificChecker.checkMatched` 的错误类型过滤需由官方后检查规则修正。

源码与 CJO common 应有相同具体签名，这项属于 common 编译依赖准备；不要在 matcher 里增加“来源是 CJO 则另走算法”的分支。eager 的 common 模块通常先编译；仍要用其实际流水线验证此前置条件，不能仅因 session 是依赖就假定类型已推断。

### 3.4 上层 caller phase 合法性表

在**退出 target read lock 后**推进 common IMPLICIT，checker 恢复的是调用此 CJMP resolver 之前的 phase：

| 外层 currentTransformerPhase | common 请求 IMPLICIT_TYPES | 实际规则 |
|---|---|---|
| null（直接请求 CJMP） | 合法 | checker 直接接受，无当前 phase |
| IMPLICIT_TYPES（调用方正在推断） | 合法 | `isItAllowedToCallLazyResolveToTheSamePhase` 对 IMPLICIT_TYPES 为 true；common 使用既有 jumping lock |
| BODY_RESOLVE | 合法 | BODY_RESOLVE > IMPLICIT_TYPES |
| CJMP_MATCHING（错误地在另一个匹配锁里请求未准备依赖） | 非法 | 高阶段请求；本设计禁止 matcher 递归请求别的未配对 specific |
| TYPES / STATUS / EXTENSIONS | 非法 | 它们本就不允许消费 CJMP 结果；不能在 hook 中通过 ThreadLocal 操作掩盖 |

“doResolveWithoutLock”只表示当前目标未持 CJMP 锁，并不代表整个线程没有外层锁。这里能处理外层 IMPLICIT/BODY 的原因是枚举关系及 IMPLICIT 同阶段许可，不能泛化成任意外层 phase 均安全。

common→specific 的反向依赖仍必须由模块图禁止；正常 common 编译只看自身及其依赖，不会读回当前 specific target。common 内部真实递归按既有 IMPLICIT jumping cycle 机制处理，不由 CJMP 添加错误兜底。若多层 refinement 同时允许中间模块兼任角色，应额外验证合法图中的递归请求没有在 CJMP 锁内发生；不能在未验证前称全部图无环。

### 3.5 必须删除与保留的边界

删除：`CfirCjmpMatchRunner.resolveCjmpSignatureTypes()` 的无差别 IMPLICIT 请求；`CfirCjmpResolver:192–194` 对 specific/全体 extend 的推进；`CfirCjmpMatchRunner:291` duplicate-extend 推进全部声明；LL hook 中同名兄弟预解析。

保留：在 CJMP **目标锁外**、仅针对真实 common 候选的依赖签名准备；在目标 CJMP 锁内读取已配对 outer 的 raw 属性。`CfirResolvePhase` 同阶段许可仍仅 IMPLICIT_TYPES，不能为了父/兄弟匹配增开 CJMP 同阶段访问。

最后一项关联风险：`CfirExtendMemberScope` 可在 TYPES/STATUS 等早期被使用。将旧 reverse bool 机械替换成符号 lazy getter 可能把 CJMP 消費提前到非法阶段。应把 actualizing 过滤放在具有 CJMP 前置条件的 consumer/scope 层，不能在所有早期 type-resolution scope 上无条件推进 CJMP，也不能恢复“没数据就不过滤”的兜底。

## 4. 数据结构、读写边界与幂等

### 4.1 一个声明只有一份兼容性结果

`CjmpAttributes.kt` 中保存完整只读结果，而不是多个并发 map 拼出的状态。建议结果内容按真实语义组织为：

- ordered candidate results：候选符号、兼容性结论、参数级失败、是否缺实现体；保留官方候选顺序。
- matched counterparts：单普通声明 first-fit 对应项；extend 为全部同 key common owners；模式变量按具体 binding symbol 关联。
- 每个 counterpart 自己的 common→specific 类型参数映射；多 common extend 不得只保留一个无法区分 owner 的 map。
- 独立匹配后的状态：无候选、成功、全部失败、合法非穷尽 enum 额外构造器等。合法无配对状态必须也有已计算结果，不能继续豁免 LL 后置条件。
- 真实候选诊断事件：成功候选之前已产生且官方不会撤销的参数诊断等。

`null` 仅表示尚无阶段结果（或明确不参与匹配的声明），不能与“已解析、无候选”的空结果混为一谈。避免 `isUnmatched`、`hasResolutionResult`、`isEmpty` 由多个表的内容推断。

### 4.2 写入端

- 局部可变收集器放在 `cfir/resolve/.../cjmp/` matching context/resolver 所在层；只计算当前目标。
- matching context 面向写接口，只有候选尝试、失败记录、选择 counterpart 等操作；不暴露 session 反向聚合读取能力。
- transformer 得到不可变结果后，单次设置目标 declaration attribute。不要让 storage.bind 在每次候选尝试期间向全局表边算边写。
- 公共符号 getter 只暴露只读结果；raw 声明 setter 是框架内部 API，依 Kotlin 的属性写入口使用纪律限定到 transformer，不能给 checker 提供“重新绑定”方法。
- session 级组件不得再保存 `commonForSpecific`、`typeParameterMappings`、`mismatchKinds` 等同一前向信息。否则属性与表会在 LL 重建/取消时分叉。

### 4.3 第二绑定与 common 聚合

计划 §12 已选择“单声明独立兼容性、第二绑定由 checker 推导”。因此属性中不能记录受请求顺序影响的 SECOND_BINDING。

checker 聚合流程：

1. 经 **specific session 自己的 provider** 枚举当前 package/声明族中的 specific 候选，不能只枚举此前进入过 matcher 的符号。
2. 推进全部待聚合候选到 CJMP_MATCHING；成员必须先对应 outer。
3. 将候选按官方文件名 + 声明源码顺序排序；同一个 common owner 的多实现关系按这个稳定顺序推导。
4. 从各声明只读兼容性结果构建本次完整聚合视图；common 未匹配、多实现、specific 第二绑定诊断共用该视图。
5. 该聚合不得改写 declaration attribute，不得把顺序裁决回写给 matcher；不能在 session monitor 内调用 lazy resolve。

**多候选必须保存足够的诊断信息。** 若前一个兼容 common 已被先行 specific 占用，官方可能继续候选循环；仅保存每个 specific 的首个 matched symbol，再用 `groupBy(common)` 数量判第二绑定，不足以重放 first-fit。checker 必须从 ordered candidate results 重放占用检查及缺体判定顺序。实施前应补“多个 compatible common + 两个 specific”的 fixture，核对官方最终诊断与候选选择；不能仅有一个 common、两个 specific 的样例。

声明属性表达“该声明独立的兼容对应物”，checker 聚合表达“按官方次序进行全组绑定时的接受/拒绝”。两者概念必须在 KDoc 写明，不得把 independent match 冒充官方全局绑定。正常无冲突程序二者相同；有冲突程序由 checker 确定第二绑定及后续候选诊断。

F8 读/写接口的推荐落点：

- tree/declarations：不可变声明结果及符号 getter。
- tree/session 或 tree/declarations：供 common-facts/序列化前端消费的只读聚合接口（仅查询已构建快照），非空 session accessor；这是仓颉第二绑定所需的明示适配，不是 Kotlin 原样类型。
- resolve/cjmp：局部匹配写接口及实现；不要让 session 聚合实现承担匹配器写事务。
- checkers：聚合构造与第二绑定裁决。若为现有 frontend reporter 共用需要可独立调用的聚合准备入口，应置于其共同可依赖的 framework 层，且不得让 tree 依赖 providers/checkers；P8 删除 reporter 后再收窄。

若聚合被缓存，缓存单位应是已经完整收集的声明族快照，生命周期与 LL session/源码变更一致；禁止“正在收集”的列表被消费者看见。可先每次确定性构造不可变快照，不引入缓存即可保证正确性；这是完整计算方案，不是无数据兜底。

## 5. 外围容器唯一入口

必须修共享 containing-class owner，再接 CJMP：

1. 将本仓 `cfir/resolve/.../resolve/ContainingClassUtils.kt` 对位到 Kotlin 的 **providers** 模块同包；`getContainingClassSymbol()` 简化为声明站点 provider 查询。
2. `cfir/providers/.../resolve/providers/CfirProvider.kt:117–123` 改为 Kotlin 的 lookup-tag 查询，不保留 `symbol.callableId.classId?.let(symbolProvider::get...)`。
3. 使用 lookup tag 的 `toSymbol` / `toClassLikeSymbol`，不能用当前 `toClassSymbol` 把 struct/interface/enum 静默丢掉。`ToSymbolUtils.kt:80` 的 `toClassSymbol` 只返回 CfirClassSymbol。
4. 保留 `LLCfirProvider.getContainingClass` 的 PSI override；该路径为类重声明找到准确 owner，是 Kotlin 既有框架行为。
5. CJMP 外围入口对 callable：先通过声明站点的 extend provider 查真实 extend owner（源码/LL/CJO 已各自实现），否则调用公共 containing-class symbol 入口；二者代表不同声明形态，不是 ClassId 猜测兜底。
6. 删除 transformer ThreadLocal、LL `containingDeclarations.lastOrNull` 参数、MatchRunner 可选 containingContainer 参数、resolver 独立 common ClassId 查找分支。common owner 只能来自 specific owner 的已发布 counterpart。

`ClassMembers.kt:34` 已有 `CfirCallableDeclaration.containingExtend`，raw PSI/LightTree 在各自 builder 赋值；CJO 没有搜索到该属性赋值，但 `CfirDeserializedExtendProvider` 有真实归属索引。因此不能仅用该 raw 属性替换全部 extend provider 查询。保留统一 provider 入口，必要时在 provider 的结构 owner 层收敛该属性和 CJO 索引。

另一个迁移检查点：当前未搜到 `containingClassForStaticMemberAttr` 的赋值，raw builder 多数 callable 统一写 dispatchReceiverType。删除 ClassId 旁路后要验证 static function/property、constructor、enum constructor 四类归属；若丢 owner，修 raw/CJO 写 owner 的共享路径，不在 CJMP 恢复 ClassId 查找。

## 6. 现有 storage 与 helper 的完整消费迁移清单

搜索范围：一方 `*.kt`，排除 external/build/out；本轮命中以下 19 个文件。

| 文件（省略共同 `src/org/cangnova/cangjie` 前缀时以模块/尾路径定位） | 迁移 |
|---|---|
| `cfir/cfir-tree/.../session/CfirCjmpMappingStorage.kt` | 移除 session 前向表与可空 accessor；局部写操作移到 matching 层，结果移到 declaration attribute；COMMON_WITH_DEFAULT 与默认参数 helpers 分家 |
| `cfir/cfir-tree/.../session/CfirCjmpSpecificCompilation.kt` | 保留 mode 判据；删除 `bodyResolvePrerequisitePhase`；删除按 common 查 reverse 的 `isCjmpShadowedCommonDeclaration` API，调用方改传完整候选集合做正向过滤 |
| `cfir/cfir-tree/.../session/CfirCjmpCommonSideFacts.kt` | `mustReportNotMatched` / `hasMultipleImplementations` 接收已完整构建的只读聚合，不依赖 mutable storage；COMMON_WITH_DEFAULT helper 改 import |
| `cfir/cfir-tree/.../declarations/CfirResolvePhase.kt` | 调序、声明属性输出 KDoc、IMPLICIT 输入契约 |
| `cfir/resolve/.../transformers/CfirCjmpMatchingProcessor.kt` | 删除 clear/ThreadLocal/同名组重跑；只发布目标结果 |
| `cfir/resolve/.../cjmp/CfirCjmpMatchRunner.kt` | 改返回独立结果；移除 storage 参数/全局 bind；统一 outer；删 IMPLICIT 跳转 |
| `cfir/resolve/.../cjmp/CfirCjmpMatchingContext.kt` | 仅持本次局部写接口；Quest 精确识别；无跨声明写入 |
| `cfir/resolve/.../body/CfirCallResolver.kt` | 3 处默认参数改 function/index helper；`reduceCjmpShadowedCommonCandidates` 改候选集合中 specific→common 正向过滤，删 nullable/isEmpty/reverse 快路径 |
| `cfir/resolve/.../calls/stages/CfirMapArguments.kt` | 4 处默认参数判断改 providers 的共享 helper |
| `cfir/providers/.../scopes/impl/CfirExtendMemberScope.kt` | 不能原地换成惰性 getter：STATUS 已调用它。按第 9 节将其原始索引去除配对读取，实际化迁到本批前移的 P3/C4 后期视图与独立缓存边界 |
| `cfir/checkers/.../declaration/CfirCommonSpecificChecker.kt` | commonFor/typeMap/mismatch/candidateDiagnostics 全部读目标符号属性；第二绑定与 common 方向先完整聚合；删 runCatching；checkMatched 对 Quest 的 post-check 在此暂存，P6 收敛到共享 checker |
| `cfir/checkers/.../declaration/CfirGenericInstantiationChecker.kt` | 前向映射/类型映射改符号属性；所有 `isCjmpShadowedCommonDeclaration` 逐点改为其 owner 候选面正向过滤；P3/P4 删除剩余手工 common 投影 |
| `cfir/checkers/.../declaration/CfirInheritanceDeepChecker.kt` | 两处 reverse 遮蔽改从对应 specific 成员面正向过滤 |
| `cfir/entrypoint/.../session/ComponentsContainers.kt` | 统一注册新的 session 只读聚合 owner（如保留）；取消可空组件路径 |
| `cfir/cfir-serialization/.../cjo/CfirCjoPackageMetadataProducer.kt` | COMMON_WITH_DEFAULT helper 改到独立声明语义文件；不得因拆存储改变 CJO 位语义 |
| `compiler/frontend/.../pipeline/CjmpDeserializedCommonSideReporter.kt` | 非空组件/已完整聚合；与源码 common checker 同 owner；P8 按计划删除 reporter |
| `compiler/frontend/test/.../pipeline/CjmpTwoPhaseCompilationTest.kt` | 删除 nullable-storage continue；前向配对断言从 declaration attr 读取；完整断言 completed/messages |
| `analysis/low-level-api-cfir/.../transformers/LLCfirCjmpMatchingLazyResolver.kt` | 后置条件检查 raw 属性已计算；不读 storage；固定 enabled；按通用锁推阶段 |
| `analysis/low-level-api-cfir/testFixtures/.../resolve/AbstractCfirCjmpMatchingTest.kt` | dump 独立结果及确定性聚合；不再断言请求一个兄弟会写另一个兄弟；改指令在建模块时设置模式 |

间接关联但不包含 storage 名称的必须迁移文件：

- `CfirCjmpResolver.kt`：签名阶段/同名组工具/owner 旁路。
- `resolution.common/.../mpp/AbstractCjmpMatcher.kt` 与 `CjmpMatchingContext.kt`：去 hasInferredReturnType；保留有序候选事实供第二绑定重放。
- `cfir/checkers/.../declaration/CfirConflictsHelpers.kt` 的 J1 runCatching。
- `CfirMacroAnnotationSourceModuleTest.resolveThroughBody`：按 enum 阶段驱动。
- G2 动态前置阶段引用：`LLCfirTargetResolver`、`LLCfirLazyResolver`、`FileStructure`、`FileElementFactory`、`FileStructureElement`、`inBlockModification`、`CfirGeneralSemanticsChecker`。统一换 `BODY_RESOLVE.previous` / 明确所需相位，不保留 session 相关相位函数。

COMMON_WITH_DEFAULT 是 tree 层声明语义，`CfirCjmpCommonSideFacts` 已消费它；应留在 tree/declarations 的独立语义文件。默认实参读取是 providers 层 declaration helper。把两个 helper 一起搬 providers 会造成 tree→providers 循环依赖，必须分别处理。

## 7. G1/G2 的完整接口路线

### 7.1 现有事实

- `CaModule` 有 directDependsOnDependencies、targetPlatform，没有 CJMP 角色。
- `CangJieProjectStructureProvider.getImplementingModules(module)` 已存在；standalone/LSP/test 均实现为反向 dependsOn 查询。
- `TargetPlatform.isCommon()` 已存在（`common/.../platform/TargetPlatform.kt:120`）。不要把 common part 自行当作新的机器目标平台。
- `CfirCjmpMode` 位于 `cfir/cfir-common`；Analysis API 公开模块并不直接依赖此模块。不能为了公开角色把底层 session enum 泄露到 CaModule API。
- LL `registerAllCommonComponents(languageVersionSettings, module, ...)` 是装配单点，当前未注册模块 CJMP 设置；另有 not-under-content-root 创建路径直接调用 registerCommonComponents（约 :422），也须覆盖。
- 现有 LL 测试在 `getOrBuildCfirFile` 后按模块名注册 `CfirCjmpSettingsComponent`，正是 G2 动态模式根源。

### 7.2 推荐接口与装配

在 Analysis API 的 projectStructure 层定义独立高层角色枚举 `CaCjmpModuleKind`（NONE/COMMON/SPECIFIC）及模块只读角色属性；该类型只表达模块事实，不包含 cjo/chir 路径。若采用 platform-interface provider 而非 CaModule 属性，也必须由所有宿主共享同一接口，不能仅测试实现。此处是仓颉 common-part/specific 编译模式所需适配，Kotlin 没有同名 enum，必须明示。

角色的唯一计算规则：

1. dangling/code-fragment 继承 contextModule 的角色。
2. 显式项目配置角色（包括 NONE）按配置固定，用于单独 common 编译与模式反例。
3. 没有显式角色时，directDependsOnDependencies 非空 → SPECIFIC。
4. 没有显式角色时，被 implementing module 引用或 targetPlatform.isCommon() → COMMON。
5. 其余 → NONE。

不能用 `NONE` 同时表示“未显式配置”和“显式关闭”；配置层需要区分两者，最终模块角色必须非空。显式配置与 graph 的不合法组合应由配置校验报告，不在 resolver 临时重写。

LL factory 在模块图完整且服务已注册后，计算角色并一次性映射到 `CfirCjmpSettingsComponent(explicitMode=...)`；之后 raw CFIR 构建、checker 与 matcher 共用该值。mode 变化走模块修改事件、session invalidation、重建 session；不在已有 session 上 register 覆盖。

### 7.3 各宿主写入点

| 宿主 | 真实入口与所需修改 |
|---|---|
| 公共 API | `analysis/analysis-api/.../projectStructure/CaModule.kt`（角色契约）及同目录 enum；或 `analysis-api-platform-interface/.../projectStructure` 的统一角色 provider；更新 API surface baseline |
| LL | `analysis/low-level-api-cfir/.../sessions/LLCfirAbstractSessionFactory.kt` 的 `registerAllCommonComponents` 与直接公共装配的特殊 session 路径；dangling 继承 context；库/内建无 role 的明确 NONE 注册 |
| standalone | `CaStandaloneModules.kt` 构造输入携带显式角色；现有 mutable directDependsOnDependencies 在创建 project structure 前填完；`CaStandaloneSessionBuilder.kt` 构造/接收整图；`CaStandaloneProjectStructure` / `CaStandalonePlatformServices` 提供 implementing 查询 |
| LSP | `lsp/.../state/LspProjectConfiguration.kt`：`LspWorkspaceModuleDefinition` 目前只有 name/sourceRootUris/packageSearchPaths，需加入真实 dependsOn 模块引用与可选显式角色的配置解析；`AnalysisApiLspProjectStructure.buildWorkspaceModuleEntries` 目前只 map 建模块，需先建全部模块，再解析依赖名、填边、校验，然后发布快照 |
| 测试 | `analysis-test-framework/.../projectStructure/CjTestModuleStructureFactory.kt` 建模块时读取 `CfirDiagnosticsDirectives.CJMP_MODE`，写角色输入；现有 `wireDependencies` 已写 dependsOn。`CaTestModuleBase` 暴露最终角色；LL test 删除后置 register 与模块名分支 |
| dangling | 平台 `CaDanglingFileModuleImpl`、standalone 与 LSP 各自 dangling 实现统一继承 context，不能遗漏已有自有实现 |
| IDE 插件 | 先保证公开接口可实现；外置 `intellij-ide`/`deveco` 不在默认一方主构建内。若声明“IDE 已完成”，必须另外核实它们真实 project-structure provider 传角色/边，不能凭 LL 层修复宣称完成 |

**P1 不应为 G1 实现另外一套暂时角色规则。** 若同步只完成 LL factory 与 test graph 的固定角色，需把 LSP/standalone 配置传入尚未完成明确登记到 P7；不可宣称生产跨平台入口已修。

## 8. 实施顺序与验收门

1. 写红 fixture：common 源码/CJO 签名一致、specific 真实 Quest 与空体 Unit 的区别、前置显式错误和后置隐式错误、调用方在 IMPLICIT/BODY 内先请求、重载逆序/并发、多 compatible common 二次绑定、static/struct/interface/enum owner。
2. 建 declaration result + 唯一 getter + 局部 writer，删除 session 前向重复数据。
3. 修 provider containing-owner 的模块/默认 lookup-tag 与 LL 精确路径；接统一 CJMP outer 入口。
4. 实现 common 签名锁外准备及纯兼容性检查，移除所有 matcher 锁内 IMPLICIT 请求；specific 保留当前真实 Quest 的延后规则，单声明一次发布。
5. 完成角色装配必需部分，恢复 LL 固定门、通用锁与阶段阶梯，再前移 enum 阶段。
6. 全部消费者迁移；common checker 构造完整确定性聚合；删除旧 getter/nullable API/ThreadLocal/兄弟写入与 runCatching。
7. 精确检查取消传播、重复调用、重建/失效后结果、pattern binding 与 enum 合法无匹配后置条件。
8. 按计划运行 matcher / LL CJMP / CommonSpecific PSI+LightTree / 两段式定向测试及阶段全量 LLT ledger；运行前由主线程统一进入 Gradle 队列。

最终 gate 必须确认：无 session 前向 map；无 CJMP 读/写锁内到 IMPLICIT_TYPES 的跳转（common 依赖准备只在目标锁外）；无 ClassId owner 旁路；无 matcher 跨声明写；无 read-side runCatching；反向聚合只供完整 checker 事实；模式在 session 构建时固定；COMMON_WITH_DEFAULT helper 拆分没有制造模块循环。

## 9. 早期 scope 调用链与 P1/P3-C4 集成边界（2026-09-29 追加）

主线程已决定：**P3 中 C4 的 actualizing scope 和独立缓存边界与 P1 同批实现；nominal common-only 成员/构造器合并仍在 P2 之后完成。** 本节覆盖新增取证，不允许用 phase 条件返回 raw scope 作为过渡方案。

### 9.1 classifier 偏好不是读取配对

`CfirCompositeSymbolProvider.withoutCjmpShadowedCommon()` 当前只读 `isSpecific/isCommon`，不是 session reverse store 的消费者。它位于两条早期入口：

- `getImportNamespace(...).classifiers`（:29–33）。
- `getClassLikeSymbolsByClassId`（:54–58）。

已核实的调用链：

1. **IMPORTS**：`CfirImportBindingResolver.resolveImportBinding:109–110` → import namespace classifiers → composite filter。import binding 随即把符号固化到 targets；这里不能请求 CJMP。
2. **SUPER_TYPES**：`CfirSupertypeResolverVisitor.prepareFileScopes:727–744` → `createFileLookupScopes(...).typeResolutionScopes` → package/star-import scope → namespace classifiers。继承类型尚未完成，不能要求 STATUS/CJMP。
3. **TYPES**：`CfirTypeResolveTransformer.withFileScope` / :931 → 相同文件 scopes → `CfirTypeCandidateCollector.candidatesFromScope` / `collectClassIdCandidates` → `declarationAvailabilityProvider.classLikeCandidates:178` → plural symbol-provider 查询。
4. **类型可达性与 re-export**：`CfirImportTypeReachability:37–38`、`CfirSourceSymbolProvider:602` 直接查询 namespace classifiers。不能在 provider 聚合层引入配对副作用。

authority 已核实官方 v1.1.3 `PreCheck.cpp:390–450 GetTyFromASTType(RefType&)`：先 LookupTopLevel、筛选 IsTypeDecl，按 scopeLevel 降序；同 scope SPECIFIC 优先；:432–441 首项 SPECIFIC 直接选中，不检查配对成功、kind 或泛型参数数。`CheckCJMP.cpp:173–180` 不同 kind 不合并、:320–323 generic/non-generic 不合并，**都不取消早期同 scope specific 类型名优先**。

因此安全迁移为：composite provider 保留完整结构候选；同 scope 类型名选择在 `CfirTypeCandidateCollector` / 类型解析的名称优先级 owner 实现，使用预先固定的模式/语言门与声明旗标，不读取 CJMP 属性。不得把远 scope specific 提升到较近 scope common 之前；也不能把同名 re-export 中不同 classId 的 common 全局删掉。

Kotlin 对位的边界：`FirCompositeSymbolProvider.getClassLikeSymbolByClassId` 返回 provider 顺序第一个符号；`FirPackageMemberScope.processClassifiersByNameWithSubstitution:37–52` 缓存单个 classifier；`FirActualizingScope:42–45` 对 classifier **不实际化、不查匹配**，依靠名称查找顺序。仓颉需要保留其真正的重声明候选及官方 SPECIFIC 优先，因此不应照搬“无条件取第一个”丢掉本仓候选诊断。

IMPORTS 负责绑定原始可见声明；最终类型名优先级由类型查找处理，不能在 import 阶段先启动配对。检查器需要完整候选做重定义时仍应拿完整 provider 数据。

### 9.2 STATUS 确定会触达 extend 的 reverse filter

直接证据：

```text
CfirStatusResolver.getOverriddenFunctions / getOverriddenProperties (:849, :868)
  → containingClass.unsubstitutedScope(memberRequiredPhase = null)
  → CfirCangJieScopeProvider.getUseSiteMemberScope（USE_SITE key）
  → CfirClassUseSiteMemberScope.processDirectOverridden* (:549+)
  → computeDirectOverriddenForDeclaredFunction (:850)
  → getFunctionsFromParentsByName (:870)
  → getUnmergedFunctionsFromParentsByName (:888)
  → parentScopes / parent.requireFunctionInheritanceScope().processFunctionsByNameWithProvenance
  → parent.collectFunctions (:757)，或 containsOwnFunction (:636)
  → extendScope.processFunctionsByName
  → CfirExtendMemberScope.memberIndex/buildIndex (:84, :184)
  → isCjmpShadowedCommonDeclaration (:198)
```

LL STATUS 也走该共享 resolver：`LLCfirStatusLazyResolver:285,319` 调用上述 getOverriddenFunctions/Properties。这不是仅 eager 下的可能性。

机械把 :198 换成“查 specific counterpart 的惰性 getter”，会产生 STATUS → CJMP → 要求 STATUS 完成的循环。即使把 getter 放到 `buildIndex()` 前统一枚举，也只是把循环换位置。

另一个必须纠正的范围判断：TYPES 的 `withEnclosingClassBodyScopes` 实际使用 `CfirClassDeclaredMemberScope`（`CfirTypeResolutionConfiguration:165–172`），不走 extend member scope。TYPES 已实证的风险在 classifier provider 链；不能把所有 TYPES 成员查找都泛称为 ExtendMemberScope 循环。

### 9.3 原始 scope 缓存不能成为后期实际化缓存

当前关键缓存：

- `CfirPackageMemberScope` 的 classifier/function/callable/property cache：按 Name 缓存原始 namespace 结果，应继续保存原始符号。
- `CfirCangJieScopeProvider.getUseSiteMemberScope:41`：`ScopeSession.getOrBuild(CfirUseSiteMemberScopeKey(useSiteSession, classSymbol), USE_SITE)`；当前 STATUS 与后续 body 可以取到同一 scope。
- `CfirExtendMemberScope.memberIndex`：scope 实例级 lazy，仅构建一次；其内部当前 reverse filter 让索引取决于第一次构建时已配对的声明集合。
- `CfirClassUseSiteMemberScope:475–513`：functions、properties、functionsFromParents、unmergedFunctionsFromParents、propertiesFromParents、directOverriddenFunctions/Properties；另有 parentScopes、name caches。
- `getFunctionsFromParentsByName:870–879` 在缓存前执行 overridden / abstract / interface-default 过滤以及 `mergeEquivalentInheritedFunctions`。被丢弃或归并的输入不能靠外层后过滤重新恢复。
- `CfirClassStaticScope.staticScopeForQualifierType:269–292` 也用 ScopeSession 缓存，key 当前是 ClassId + qualifier type + memberScopeKind；内建 qualifier key 同样未区分原始/实际化输入。
- `CfirReceivers` 的 implicit receiver 保存 scope 计算状态；type substitution scope 的符号缓存也绑定其 delegate 的输入语义，不能复用错误 delegate。

要求的 cache owner：

1. 原始 provider、package scope、declared member scope、extend 原始索引永远不读配对；它们可以在 IMPORTS/STATUS 被缓存而保持正确。
2. 非类型实际化 scope 按 Kotlin 由 body tower 构造，包装原始 scope；如果缓存，身份至少是原始 delegate + use-site session，其生命周期归调用方 ScopeSession/LL session，不能注册成 project 全局或按 ClassId 唯一的缓存。
3. 接收者成员的后期实际化图须以未过滤 declared/extend 输入构建 **独立 scope 实例与独立缓存**，在继承覆盖/默认实现归并之前删去 matched common；不能只包在 STATUS 已经归并完的 scope 输出之外。
4. 这个后期图仅过滤当前已经存在的 common/specific 成员；不负责把 specific nominal 缺失的 common-only 成员加进来。后者依然是 P2 后的 nominal 合并任务。
5. 构建 key 按语义视图的入口/type 区分，不按“当前声明 resolvePhase >= CJMP”切换。同一 raw key 下不得先返回 raw、后返回 actualized，也不得在 phase 变化时清空同一组缓存试图补救。
6. `withReplacedSessionOrNull`、static qualifier scope、receiver scope、substitution scope、parent scope 递归都必须携带正确视图身份；原始和实际化的 direct-overridden 缓存不能共享。

Kotlin nominal actual 本身声明全部成员，没有仓颉 common-only 合并，`FirActualizingScope` 的 `init` 明确要求 delegate **不是 FirTypeScope**。因此上述成员图过滤/输入边界是仓颉 extend 与已接受 CJMP 合并语义需要的明确适配，不能把一个给 FirTypeScope 用的新 wrapper 冒充 Kotlin 同名类原样移植。

### 9.4 可直接迁移与必须连同 C4 迁移的边界

| 位置 | 本批处理方式 | 为什么 |
|---|---|---|
| 默认参数 helper | 直接用 owning specific 符号的惰性结果 | consumer 位于 IMPLICIT/BODY；有确定的正向 owner，不需要反向候选枚举 |
| 纯 matcher 父容器读取 | 读已经完成的父声明 raw 结果 | designation 先 outer；无新的同阶段请求 |
| common/specific checker 的前向读取 | 直接用声明符号 getter；common 方向先完整聚合 | 不在早期签名 scope 中，不依赖首次查询顺序 |
| `CompositeSymbolProvider.withoutCjmpShadowedCommon` | 不改成 getter；按 9.1 移到同 scope classifier 选择 | 这是早期声明旗标优先级，不是已匹配关系 |
| `ExtendMemberScope.buildIndex:198` | 删除原始索引上的 reverse 读取，连同 C4 后期视图/缓存边界迁移 | STATUS 已经调用；必须避免 phase 条件兜底 |
| `ClassUseSiteMemberScope` 的 local/parent/member cache | 原始与后期实际化图分开；从未过滤输入构造后期图 | 覆盖/默认实现归并会丢信息，外层 late filter 不足 |
| `CfirCallResolver.reduceCjmpShadowedCommonCandidates:1665–1692` | 删除最终 reducedCandidates 上的 filter，迁到 tower 的原始候选枚举层 | reduceCandidates 和 expected-type 过滤已经移除部分 specific，集合不完整；换成正向 getter也会遗漏 |
| `ScopeBasedTowerLevel.processCallablesByName/processFunctionsByName` | 非类型 scope 按 Kotlin FirActualizingScope 包装；在 consumeCallableCandidate 之前按完整同名符号实际化 | callable reference、function value、普通函数/属性访问必须共用入口 |
| `DispatchReceiverMemberScopeTowerLevel` | 使用后期实际化成员视图后再构造 candidate；不能把 FirTypeScope 直接塞入 Kotlin 非类型 wrapper | 当前本仓把成员 scope 转交 ScopeBasedTowerLevel，需恢复两种入口职责差异 |
| `CfirTowerResolver.findVariables/findFunctions/findCallables` | 改为消费同一实际化非类型 scope 视图 | 这些 helper 直接遍历 scopes，绕过普通 runResolver；只修 CfirCallResolver 会漏函数引用等路径 |
| `GenericInstantiationChecker.collectInstantiatedExtendMemberSignatures:978–1013` | 可从本次完整 extends owner 集合先计算 forward shadow set，再过滤各 owner 的直接成员和 inherited-default 输入 | 当前方法已有整组 extends，且在 checker 阶段；不可在循环内只看“当前一个 common extend” |
| `InheritanceDeepChecker.collectDirectExtendMemberInfos:1762–1790` 及约束推导 :1820–1870 | 先收集该目标/递归路径的完整候选 owners，确定 forward set，再保留原可见性/替换元数据 | 已有 provider 枚举边界；需要两个 pass，不能用某个 common 单独反查 |
| `GenericInstantiationChecker:1466,1490,1508` | 与后期实际化成员图同改，保留 provenance/constructor 通道 | 此时已经消费 concreteScopes 及 direct-overridden 缓存；单改 bool 不能恢复提前归并的 common |

所谓“完整 specific 候选集合”是该名称/receiver/owner 查找域内、在 candidate applicability/expected-type 规约前的集合，不是当前 `bestCandidates`、单个被访问 common owner、或先前已经被分析过的声明。先按 raw 符号的正向配对移除 common，再执行调用可见性/适用性阶段；不能让 specific 因某个调用失败而复活其 common 对应物。

### 9.5 Kotlin tower 具体边界与本仓差异

- Kotlin `TowerLevels.kt:347–365 ScopeBasedTowerLevel` 在构造时按固定 MPP feature 选择 `FirActualizingScope(givenScope, session)`；无当前 phase 分支。
- `FirActualizingScope.processCallableSymbolsByName:61–84` 从 actual 正向取得 expect，先收集/屏蔽，再把结果交 processor；KDoc 明确指出 actual 的 Hidden/LowPriority 等可能使其无法进入最终 successful candidates，所以不能拖到 overload conflict resolution 才过滤。
- `FirActualizingScope` classifier 与 constructor 方法透传，不做 matcher 查找；CJMP enum 构造器/普通构造器应按仓颉官方语义在成员视图/构造器入口处理，不要机械复制 Kotlin 无构造器 actualizing 语义。
- Kotlin `MemberScopeTowerLevel.processMembers:72+` 取得 `FirTypeScope`；`processCandidates:238–250` 在当前镜像中只检查 extension receiver 一致性，**没有**读取 expectForActual。不能因为 FirActualizingScope 的注释说 type scopes 由 MemberScopeTowerLevel 处理，就虚构 Kotlin 内有另一套 type-scope expect 过滤算法。
- 本仓 `TowerLevelHandler.kt:130` 的 ScopeBasedTowerLevel 持有直接传入的 CfirScope；`:258–331` 直接 consume；`:467–533` 的 DispatchReceiverMemberScopeTowerLevel 又把 type scope转交前者。非类型 wrapper 应挂在 non-member 调用边界；成员路径必须消费 9.3 的后期视图而保持 lookup provenance、substitution owner 和 dispatch receiver。

### 9.6 可分离的实施批次与验收

本批 P1 + 前移 C4 包含：

1. 声明属性和惰性读；common 锁外签名准备；固定阶段阶梯。
2. provider/原始 scope 去掉匹配结果依赖；classifier priority 按独立的官方早期规则归位。
3. 非类型 tower actualizing wrapper及 findFunctions/findVariables/findCallables 共用视图。
4. 已存在 extend/nominal 成员的后期 forward 过滤；原始与后期成员/父图/static/receiver/substitution 缓存分离。
5. checker 中现有完整 extend 集合采用 forward 过滤；不再保留 tree/session 的 reverse shadow bool。

P2 后的 nominal 合并继续负责：specific 缺失的 common-only 成员、接口义务、构造器并入；隐式构造合成修正；未配对子类的 common 投影消除；基于合并面的重定义。这些不应借本批 C4 提前猜测实现。

必须增加的缓存/阶段回归：先 STATUS/direct-overridden 预热再 BODY；先 BODY 再 STATUS；同一 scopeSession 内跨文件先调用方后被调用方；static qualifier 与普通 receiver；函数引用与直接调用；common 默认成员+specific 替代成员；两个有相同 ClassId 的不同 scope类型名。两种请求顺序的结果与只创建后期视图时一致。

**拒绝项：** `if (currentPhase < CJMP) raw else actualized`、`if (attribute == null) 不过滤`、读不到结果就继续使用旧缓存、把 actualizing 放进 CompositeSymbolProvider、仅在 reduced bestCandidates 上改正向过滤、复用早期已经归并过的成员图却宣称只需外层过滤。P1 若仍保留这些任一项，不应标为 A2/F8 已完成。
