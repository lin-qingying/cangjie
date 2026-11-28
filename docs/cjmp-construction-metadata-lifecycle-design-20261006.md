# Construction annotation metadata 生命周期（2026-10-06）

本文是共享主构造成员字段 materializer 的前置设计及 metadata 阶段实施记录。
**metadata 阶段已实施；后续共享字段 materializer 也已通过定向验收，完整 LL macro construction 仍未实现。**
取证基线为 `68f9504e5`；随后进行的参数空 MODIFIER_LIST / binary stub 同构修复不改变本文生命周期问题。
完整 CJMP 目标仍以 `cjmp-framework-review-and-fix-plan-20260928.md` 为准。
字段生成要求沿用 [主构造 completion 设计](cjmp-primary-constructor-completion-design-20261006.md)，本文不重新解释语言语义。

## 当前实施与验证

- 私有 `CfirAnnotationMetadataBuilder` 已进入 PSI/LT、所有 fragment 与 typed `PreMacroCfirFile` 产品。
  LT 不再有会丢弃 writer 的裸文件返回入口；PSI 裸构建必须由调用方显式提供 writer。
- `CfirAnnotationMetadataRegistry` 仅是 canonical session 已发布弱索引。reader/publisher 同锁；
  sealed 产品携带 session 身份，实际 declaration 只强持有自己的不可变表，generation token 不回指 writer。
- 完整 carrier tuple 保留 provenance；`currentSlot` 提供当前 owner/index/annotation；删除项退休。
  `replaceAnnotationList` 在改真实列表前验证全部来源，原子处理删除、重排与显式跨 owner 绑定。
  `transferOwner` 处理保留 annotation identity 的声明替换，并拒绝对退休 owner 再写入。
- `@When` pruner 同步 metadata 与 surface 可达性，正式 CFIR visitor 覆盖形参及已保留 body 声明。
  stable splice 在实际修改处迁移 metadata，不再事后按旧 owner/index 猜测。
- raw reparse/splice 不再吞内部不变量或取消；executor 使用 `use` 关闭；异常边界仅清理仍打开的事务后重抛。
  合法的用户语法/形状错误继续走既有类型化结果。
- batch 在 provider 的 recording lock 内先发布全部 metadata，再注册文件。
  LL 当前只在 raw cache/finder 返回前发布 raw metadata；**没有赋予宏完成身份，也没有调用 Identity 冒充展开**。
- metadata 阶段未移动字段发射时点；后续共享 materializer 阶段已关闭 `allkinds` 两入口，详见primary completion文档。

验证：8199 初版 providers 51/51；随后扩展为25个事务测试及异常、条件裁剪联合回归。
58721（exit1，9m41s）中 raw22/22、完整LL168/168通过；providers55仅异常类型断言不符、
frontend141仅两个新测试漏传session Context。修复的是测试设置，生产输入未变。
12448（exit0，1m5s）：providers55/55、frontend141/141；与上述raw/LL合计386/386，无跳过。
providers XML10:58:05–06Z，frontend10:58:26–42Z；raw10:43:11–14Z，LL10:47:09–52Z。
最终静态 gate 通过。15203已exit0（6m22s）：CfirMacroAnnotationSourceModuleTest 5/5、
CfirDeclarationOwnerTest 6/6；合计本批397/397，无跳过。完整 CFIR 尚未验收。

以下各节保留设计依据与全量接入要求；§10及真实LL宏构造相关验收仍待完成。

## 1. 改造前确认的问题

改造前 `CfirAnnotationMetadataRegistry` 同时承担三个不同生命周期的职责：

1. raw builder 的可变收集器：`record`、`recordDerivedSlot`；
2. construction 的可变槽位迁移表：`snapshot`、`migrate`；
3. ordinary checker 的 session 查询索引：`snapshot(annotation)`。

它以 session component 注册，内部是未同步的 `LinkedHashMap`、`IdentityHashMap` 和一个全局 `frozen` 布尔值。
`ensureAnnotationMetadataRegistry()` 当前没有同步；多个 LL 文件可以并行构建。
Frontend service、Identity service、entrypoint 严格失败分支都会冻结同一个 session 实例。
因此不能直接在每个 LL `fileCached` 回调内调用当前 construction service：首个文件冻结后，第二个文件或 lazy rebuild 仍需 `record`，必然与该生命周期冲突。

仅改为“冻结后创建新 registry”会丢失此前文件的查询；仅解冻会使已发布数据重新可变；把每次 rebuild 的全部强引用永久累加到 session 会阻止旧 CFIR 图回收。这三种做法均不采用。

两个必须保持的身份区别：

- `CfirAnnotationSlotSnapshot.owner` 是实际 annotation 槽位 owner。
- `CfirAnnotationCall.containingDeclarationSymbol` 是解析上下文 owner；参数 annotation 中它可以是父函数/构造器 symbol，**并非参数的槽位 owner**。

因此不能从 `annotation.containingDeclarationSymbol.cfir` 直接找到参数的 metadata attribute。
`CfirAnnotationCall` 当前也没有通用 attributes；不为本任务扩展生成式 expression 树来加入第二套属性系统。

## 2. Kotlin 框架依据与仓颉差异

参考根目录：`D:/code/intellij/cangjie/external/kotlin`，只读。

| 对应源码 | 已读取的约束 | 本设计采用方式 |
|---|---|---|
| `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/file/builder/LLFirFileBuilder.kt:24` | 在 `fileCached` 的创建回调里完成 raw 文件构建，然后返回 | 仓颉在同一发布边界内额外完成真实 macro construction、字段 materialization 与 metadata seal |
| 同目录 `LLFirDeclrationsCacheForModule.kt:22–42` | `ModuleFileCache` 使用 weak PSI keys 和 `computeIfAbsent`；同一文件完成创建后才进入缓存 | 保留现有模块/类/方法职责，不另建绕过 cache 的公开文件表 |
| `compiler/fir/tree/src/org/jetbrains/kotlin/fir/declarations/FirDeclarationAttributes.kt:19–46` | attribute 本身不线程安全；必须发布前写入，或遵守 phase/自定义锁；`copy()` 复制容器而非深复制任意值 | 将已封闭的不可变 metadata 表放在实际 declaration attribute；不在公开声明上无锁补写 |
| `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/caches/ValueWithPostCompute.kt:8–121` | 计算及 post-compute 完成前，其他线程不可见；失败复位；同线程临时可见由明确状态与锁控制 | construction 的未完成产品不能提前放入 session 查询索引。若后续确需递归构建，使用既有 post-compute 框架，而非公开半成品 |
| `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/file/structure/LLElementMapper.kt:184–199,267–300` | 在锁内构建新映射，再发布新的 map/state；不边改公开 map 边让读者观察 | 发布的是完整不可变表，不对读者可见表逐槽位修改 |
| `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/util/FirElementFinder.kt:147` | finder 使用 weak-key/soft-value 图索引，不以永久强 session map 拥有所有文件 | 本任务采用 weak-key/weak-value annotation 索引，实际 declaration 拥有强引用 |
| `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/caches/FirThreadSafeCachesFactory.kt:46–79` | cache 支持明确配置 weak keys、weak values | 使用平台已有弱引用容器，不手写引用队列或基于路径的缓存 |
| `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/lazy/resolve/RawFirNonLocalDeclarationBuilder.kt:36–94` | rebuild 从 designation 和实际原声明构建新图，函数 target rebind 有独立职责 | metadata 绑定新图实际 owner，不以 PSI 文件身份覆盖旧图全部 metadata |

Kotlin 没有仓颉 token macro construction，因此不存在可直接照搬的 `CfirAnnotationMetadataBuilder` 类。
新增事务数据属于仓颉宏语义要求；现有 LL file cache、declaration attribute、finder、session 配置与 lazy rebuild 的 owner 不移动。

SDK 实际已有 `ContainerUtil.createConcurrentWeakKeyWeakValueMap()`；已读取缓存中 253 同分支 SDK 源码 `ContainerUtil.java:2724`。
这里不需要为 providers 添加 Guava/Caffeine 新依赖。

## 3. 模块与类型归属

相关 Gradle 依赖现状：

```text
compiler/frontend -> cfir/entrypoint -> raw PSI/LT -> raw-cfir-common -> providers -> cfir-tree
analysis/low-level-api-cfir -> cfir/entrypoint、providers、cfir-tree
```

`raw-cfir-common` 已经依赖 providers。Identity service 位于 providers，故不能让它调用 raw-common 内的 materializer，否则形成反向依赖。
metadata 类型、发布索引及共享 materializer 均放在现有 `cfir/providers/.../resolve/providers/macro` construction 层；session accessor 保留在 providers 的 `cfir/session` 包。
declaration attribute 的扩展声明也可放 providers：它使用 tree 的 `CfirDeclarationDataRegistry`，不会使 tree 反向依赖 providers。

metadata 阶段已拆分原有混合职责，没有保留隐式 session writer 入口：

| 类型 | 职责与可变性 |
|---|---|
| `CfirAnnotationMetadata` | construction 所用只读查询接口，提供按 carrier / annotation 查询和 surface 查询；不提供写入或 freeze |
| `CfirAnnotationMetadataBuilder` | 一次文件构建代次的私有事务，实现上述查询接口；独占 `record`、`recordDerivedSlot`、`migrate`、`seal`、`abort` |
| `CfirDeclarationAnnotationMetadata` | 某个实际 declaration owner 的不可变槽位表；只保存该 owner 的 snapshots |
| `CfirAnnotationMetadataSnapshot` | `seal` 返回的不可变发布产品，聚合当前事务要发布的 owner 表；持有明确 generation identity，不继承 session component |
| `CfirAnnotationMetadataRegistry` | session 的已发布弱索引；仅 `snapshot(annotation)` 和完整产品的 `publish`，无 raw 写入、无全局 freeze |

核心接口如下；具体校验与列表变更接口以当前源码为准：

```kotlin
interface CfirAnnotationMetadata {
    fun snapshot(carrier: CfirAnnotationReplaceCarrier): CfirAnnotationSlotSnapshot?
    fun snapshot(annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot?
}

class CfirAnnotationMetadataBuilder : CfirAnnotationMetadata {
    fun record(snapshot: CfirAnnotationSlotSnapshot): CfirAnnotationReplaceCarrier
    fun recordDerivedSlot(sourceAnnotation: CfirAnnotationCall, owner: CfirDeclaration,
                          annotationIndex: Int, derivedAnnotation: CfirAnnotationCall): CfirAnnotationReplaceCarrier
    fun migrate(carrier: CfirAnnotationReplaceCarrier, replacement: CfirAnnotationCall)
    fun currentSlot(carrier: CfirAnnotationReplaceCarrier): CfirAnnotationReplaceCarrier?
    fun transferOwner(original: CfirDeclaration, replacement: CfirDeclaration)
    fun replaceAnnotationList(owner: CfirDeclaration, annotations: List<CfirAnnotation>,
                              bindings: Map<CfirAnnotationCall, CfirAnnotationReplaceCarrier> = emptyMap())
    fun seal(finalRoots: List<CfirDeclaration>): CfirAnnotationMetadataSnapshot
    fun abort()
}

class CfirAnnotationMetadataRegistry internal constructor(session: CfirSession) : CfirSessionComponent {
    fun snapshot(annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot?
    fun publish(snapshot: CfirAnnotationMetadataSnapshot)
    fun publishAll(products: List<CfirAnnotationMetadataSnapshot>)
}
```

`snapshotForSurface` 可以保留为只读接口上的 extension，以 `annotationCarrier` 查询；不保留从 session 活动事务推断“当前文件”的方法。
不增加 `ThreadLocal` 当前事务、按路径找事务、找不到则创建新事务等隐式入口。

## 4. 身份、事务与不可变产品

### 4.1 一个构建代次只拥有自己的 raw 图

每次初始文件 raw build 或 non-local rebuild 分配新的 builder；独立且不回指 builder 的对象作为 generation identity。
不要以文件路径、PSI 文本、source offset 或“当前最新文件”代替这一身份。
同一 `CjFile` 的两个 raw 图可以同时存活；两代 annotation 对象身份不同，应各自可查询。

`PreMacroCfirFile` 显式新增当前事务字段。`PreMacroRawBuildResult` 聚合这些文件事务，不把所有文件重新合成 session 全局可变表。
`withCfirFile`、`withSurfaces`、`mapFiles` 保留同次 construction 事务；真正重新解析为另一代 raw 图必须使用新的事务。
raw 包装工厂必须要求调用方交付构建时实际使用的事务，不能在包装阶段补建空表来掩盖漏传。

raw builder 使用显式事务参数。PSI 批量入口当前复用一个 builder 构建多个文件，应改为每个文件建立自己的 builder/事务。
LT facade 每次文件构建创建 declaration builder 时传入该文件事务。独立 raw 测试自行拥有 builder 并查询其只读视图；它们不发布到 session 索引。

### 4.2 槽位身份与解析 owner

仍以显式 `snapshot.owner` / `annotationIndex` / annotation 对象身份构造记录。
slot owner 不从 annotation 的 containing symbol、源范围或名称推断。
`record` 校验 owner 的实际 annotation 槽位；`recordDerivedSlot` 只复制已有事实并更换实际 owner/annotation identity，不创建另一条 macro surface。

transaction 必须区分：原 surface 持有的 replace carrier 身份、当前槽位中的 annotation 对象、不可变原始语法 provenance。
当前 `migrate` 会删除旧 carrier key，而原 surface/cache hash 仍可能携带该 carrier；迁移时必须一并确定所有引用的更新规则，不能靠 `?:` 查询 session 旧表。
transaction 以完整 `(owner, originalIndex, originalAnnotation)` carrier 索引稳定记录，另有当前 owner/index 表；
重排可以产生新的完整 carrier alias，但不能覆盖另一条记录的 provenance：

- carrier 的 `originalAnnotation` 继续用于实际 splice 的预期身份校验；
- `snapshot(carrier)` 通过该事务已登记的 slot/provenance 查询；不能接受未知 carrier；
- `migrate` 校验实际替换已完成、旧 carrier 指向本事务登记槽位，再更新 current identity；
- published annotation 索引仅包含最终 annotation；原 surface 的 provenance 查询仅存在于 transaction/完成的 construction 结果。

`currentSlot` 对退休记录返回null；`snapshot(carrier)`仍可读取其最后的 provenance。
当前活跃owner上的空index可由新annotation使用；整体替换后的退休owner不允许再次record。

如果后续操作删除或重排 annotation 槽位，必须在该操作的 owner 同步重建槽位表；本设计不把 index 当作跨任意结构变更永远不变的 ID。

### 4.3 seal 与失败

事务状态单向推进：`OPEN -> SEALED` 或 `OPEN -> ABORTED`，不可 reopen/thaw。
状态、读写 API 及同文件 fragment 写入采用同一个事务锁；seal 必须在该文件全部 fragment 工作结束后发生。
不同文件拥有不同锁，可以并行构建。不可持 session 发布锁运行 parser/executor/materializer。

`seal(finalRoots)` 使用最终 CFIR 图的正式 visitor 遍历实际 annotation owner；只发布仍在最终图中的槽位。
这不是按名或任意语法后代补事实：visitor 只确认事务已有记录与最终 owner/annotation identity 的成员关系。
被删除的 primary、被替换声明、未采用 fragment、初始字段旧副本均不得混入已发布表。
每个保留项必须验证 `owner.annotations[index] === currentAnnotation`，并复制暴露的集合（包括 tokens）；不把 mutable builder map 暴露给读者。

失败/取消路径只终止当前私有事务，不调用 session `publish`，不影响其他文件的已发布表。
construction 诊断所需的原始 source/surface 继续由该次结果持有；不得为了显示失败诊断发布半成品 ordinary CFIR。
若调用方保留失败结果用于诊断，它有意持有的 source 是结果生命周期；session 全局不得额外强持有该事务。

## 5. 发布索引、强引用与 GC

每个最终 owner 在公开前，通过 `CfirDeclarationDataRegistry` 写入自己的 `CfirDeclarationAnnotationMetadata`。
该表不可变，并强持有自己的 snapshots；不把整个文件所有 owner 的表挂到每个声明，避免一个存活声明拖住整代临时图。
snapshot 中的 owner/annotation 与 owner attribute 形成局部环，由 JVM 正常回收。

session registry 使用 annotation 实际对象作为 **weak key**，snapshot 作为 **weak value**。
不能只用 weak key + strong snapshot：snapshot 内又包含 annotation/owner，会从 value 反向强持有 key，使弱键失效。
weak value 也不能是唯一拥有者，否则活跃 annotation 的 metadata 会提前消失；上述 declaration attribute 正是稳定强 owner。

`publish` 分两阶段：

1. 在私有图上准备 owner attributes，验证完成产品及所有索引项；不能写已公开旧图。
2. 在 canonical session 短锁内验证所有冲突并一次提交这批索引；reader 使用同一锁取得单次查询结果，不观察逐项提交中的中间状态。

不同 generation 不按 PSI key 替换旧记录；相同 annotation identity 不允许被不同 owner/代次静默覆盖。
初始文件只有在 publication 完成后才从 `fileCached` 返回；外部获得树之前，索引已就绪。
registry 不提供枚举所有历史文件的强引用列表，也不保存强 `sourceFileIdentity -> snapshot` 映射。
索引只负责定位，文件/声明缓存继续是图的拥有者。

attribute 的浅复制不能被误认为新的 owner metadata：复制 declaration 后若要成为新的 construction 产物，应在发布前依据当前实际槽位重新生成 owner 表。
不允许新 owner 的表继续把旧 owner 当作 slot owner；普通 resolved substitution 是否保留原始 annotation identity由既有复制语义决定，不因此重建 raw metadata。

## 6. Generated fragment 必须共享宿主事务

`MacroFragmentInput` 显式携带该 surface 所属 `PreMacroCfirFile` 的 metadata builder；从现有 surface-to-preFile 映射取值。
不能通过 session 取当前事务，不能给每个 fragment 创建独立并立即发布的 registry。

PSI `reparsePsiMacroFragment` 创建的 `PsiRawCfirBuilder`、LT `createParsedLightTreeFragment` 创建的 declaration builder、LT annotation argument 内部二次 parser builder，均继承同一事务。
custom annotation、declaration、parameter、expression fragment 中新增的 annotation 都记录在这次宿主构建中。
在 stable splice 成功后完成 `migrate`；最终 materializer 的 `recordDerivedSlot` 也写同一事务。
未采用 fragment 的 metadata 保留在私有事务中供当前构造诊断使用，seal 按最终图筛除；失败不发布。

## 7. 全部生产调用点迁移表

以下路径相对本 worktree 根目录，行号是取证时位置，实施中以方法名定位。

| 文件/入口 | 当前动作 | 必须迁移的动作 |
|---|---|---|
| `cfir/providers/.../CfirAnnotationMetadataRegistry.kt` | session 可变 record/migrate/freeze 与查询混用 | 拆成事务 builder、不可变 owner/snapshot 与只含 published weak index 的 registry；保留槽位校验 |
| `cfir/providers/.../session/CfirAnnotationMetadataRegistrySessionComponent.kt` | 未同步 ensure 可变单例 | session 只注册一次已发布索引；在 session 配置期创建，任何必要的惰性 ensure 必须同步，且绝不返回 writer |
| `cfir/raw-cfir/psi2cfir/.../PsiRawCfirBuilder.kt` 构造与文件/fragment 入口 | 隐式 session writer | 要求显式事务；所有 build*InPackage 分支继承该事务 |
| 同文件约 643、768：annotation carrier 转换 | `ensure...().record` | 当前 builder 的事务 `record` |
| 同文件约 992：parameter-derived field annotations | `ensure...().recordDerivedSlot` | 移入共享 materializer 时使用同次事务；迁移前也不得通过 session writer |
| 同文件约 1527–1564：`collectMacroAnnotationSurfaces` | 在 append 后记录 session snapshot | 在 append 后记录当前事务，并以返回 carrier 建 surface |
| `cfir/raw-cfir/light-tree2cfir/.../LightTreeRawCfirDeclarationBuilder.kt` 构造 | 隐式 session writer | 显式事务参数，继续与 expression builder 共享 |
| 同文件约 607、1234–1269、2556 | derived slot、普通 annotation、特殊 carrier 的记录 | 统一当前事务 writer |
| 同文件约 1481：annotation arguments 的 `parsedBuilder` | 创建新 raw builder，只传 session/context | 显式传入外层事务；这是容易遗漏的嵌套解析入口 |
| `cfir/raw-cfir/light-tree2cfir/.../LightTree2Cfir.kt:72,103` 全部 build overload | 返回 file/surfaces | 当前构建须同时交付实际事务；所有 overload 汇入同一 typed raw product，不能丢失 metadata 后重新造空表 |
| `cfir/providers/.../MacroConstructionApi.kt`：PreMacro 类型、raw 包装工厂、withCfirFile/withSurfaces/mapFiles | file 与 surfaces，无事务 owner | 保持 per-file 实际事务；重构调用签名时直接迁移调用方，不保留隐式 session writer overload |
| 同文件：Identity service | 注册并冻结 session metadata | 消费当前文件事务，执行无宏 completion，seal；不得把待展开声明/参数宏当作完成结果 |
| 同文件：Success/Degraded 产品工厂 | 仅把文件装入 RecordableRawCfirFiles | 携带已 seal metadata 产品或要求类型层面已完成；不能在工厂中晚于 cache key 才补字段 |
| `cfir/providers/.../MacroDemandClassification.kt:253,272` | 从 pre.session 查询 mutable registry | 从对应 preFile 的 `CfirAnnotationMetadata` 读取；artifact snapshot 继续不可变 |
| `cfir/providers/.../MacroFragmentParser.kt`：MacroFragmentInput | 带 fileFacts、annotationSnapshot，无 writer | 显式增加宿主事务参数，生产 parser 不允许漏传 |
| `compiler/frontend/.../MacroFragmentParserConfiguration.kt:104,367` | 创建 PSI/LT raw builder | 使用 input 的宿主事务，辅助 parse* 方法完整传递 |
| `compiler/frontend/.../MacroExpandPhase.kt:516,635` | 展开/重解析前从 session 取 annotation snapshot | 用当前 surface 对应文件事务；不查询其他文件/代次 |
| 同文件约 565–570 | stable splice 后 session `migrate` | 按每个 slot 的宿主事务迁移，slot 不跨事务 |
| 同文件约 833：MacroFragmentInput 创建 | 未传 metadata writer | 从明确 preFile 传入事务 |
| 同文件约 1282、1319：registerCacheKeys/hashSurfaces | session-wide metadata 查询 | 每文件只读事务视图；字段 completion / metadata migration 后计算最终 key |
| 同文件约 393 | 冻结整个 session metadata | seal 本次文件事务，形成结果；不碰其他文件 |
| `compiler/frontend/.../CfirFrontendPipelinePhase.kt:120` | raw 前 ensure 可变 session registry | raw 前创建每文件事务；session 配置只准备 published index |
| `cfir/entrypoint/.../pipeline/analyse.kt:58–98` 两 pre build 入口 | builder 批量复用、file/surfaces 平行列表 | PSI/LT 每文件事务随 typed pre product 传递，避免数组错位与 builder 跨文件写入 |
| 同文件 `finalizeIdentity`、`resolveAndCheckCfirAfterConstruction` | expansion 后 record；严格早失败 freeze session | 成功路径交付 sealed 产品；早失败 abort 本次事务；不冻结共享索引 |
| `cfir/providers/.../RecordExpandedRawFilesOnce.kt` / `CfirProviderImpl.recordExpandedFilesOnce` | batch source provider 最终注册 | 在同一成功发布边界先发布 sealed metadata，再让文件图可见；维持 EMPTY -> FINALIZED 单次状态机 |
| `analysis/low-level-api-cfir/.../file/builder/LLCfirFileBuilder.kt:29–56` | raw + expression surfaces 直接返回缓存 | 私有事务 raw -> 真实 construction -> materializer -> seal -> publish -> callback return；不得对 LL 调 batch provider 单次-finalize API |
| `analysis/low-level-api-cfir/.../lazy/resolve/RawCfirNonLocalDeclarationBuilder.kt:45–79` | 重建整个 raw 文件，然后 finder 取声明 | 新事务；完成结构在 finder 前。metadata 在新声明对外返回/采用前发布，旧图索引保持可用 |
| `analysis/low-level-api-cfir/.../lazy/resolve/CfirLazyBodiesCalculator.kt` rebuild 采用路径 | 将重建内容并入既有 resolve 图 | 审查被采用子树的实际 slot owner/attribute；不因源 PSI 相同覆盖旧 signature metadata |
| `cfir/checkers/.../ErrorNodeDiagnosticCollectorComponent.kt:230` | `session...snapshot(annotation)` | 保留查询形态，但只能读 published index；内部不得触发 raw 构建 |
| `cfir/checkers/.../CfirCustomAnnotationPlaceChecker.kt:48` | 同上 | 同上，保持完整 annotationSource |
| `cfir/checkers/.../CfirBuiltInAnnotationSemanticsChecker.kt:361` | 同上 | 同上，保持 provenance |
| `cfir/checkers/.../CfirImportsChecker.kt:608` | 同上 | 同上，保持 qualifiedName 事实 |

## 8. LL 发布与失效的具体约束

初次文件构建保持 `LLCfirFileBuilder -> ModuleFileCache.fileCached -> computeIfAbsent`。
publication 的小临界区不改变 Kotlin cache owner；不同文件仍可并行。
真实 service 若可能递归请求当前文件，应先完成依赖图/构造层面的协议设计，不能把 pre 文件塞进普通 cache 解除递归。

non-local rebuild 的新 whole-file 图是临时产品：先私有完成结构，然后 finder 找到目标。
只需为实际返回/采用的子树发布 metadata；不可变 owner 表随相应声明存活。
若实现先 seal 完整 rebuiltFile，随后选择其子树发布，发布产品须显式选择实际保留 owner，不能按相同 PSI 自动替换原文件表。
未采用兄弟声明及临时根应能回收。

结构变更引起的 session 失效仍由 `LLCfirSessionInvalidationService` 管理。
in-block 失效仍由 `FileStructure.invalidateElement` 在 write lock 下清除对应结构元素；metadata 不以新增独立 modification counter 绕过现有机制。
旧合法代次的 annotation 若仍被有效调用栈/缓存持有，应继续可查询；引用消失后 weak index 不阻止其回收。

参数注解解析 owner 与槽位 owner 的差异必须出现在测试中，不能通过测试构造时令两者相同来回避。

## 9. 与共享字段 completion 的顺序

完整成功路径：

```text
分配文件事务
  -> raw 收集事实/annotation/surfaces（私有）
  -> classification + macro execution/reparse
  -> stable splice + annotation metadata migration
  -> 共享 primary field materializer + derived metadata
  -> 最终 cache key
  -> seal 为不可变 owner 表
  -> 发布 metadata 索引
  -> batch provider 或 LL cache/重建采用发布最终图
```

不能把 materializer 放到 registry.publish 中：那时 cache key 应已完成，且 publisher 不应改 AST/CFIR 结构。
不能把字段延迟到 STATUS/BODY，否则 LL finder 与 declared-member scope 已缓存错误成员集。

失败返回不沿上述成功发布路径推进。允许的 degraded 结果也必须明确完成其最终 placeholder 图再 seal，不能公开仍有待执行 splice 的图。
后续字段 materializer 使用最终 owner 的 `symbol.constructType(typeArguments)`，不要直接使用目前始终生成 ConeClassLikeType 的 providers `defaultType()` 代替 struct 的 self type。

## 10. 同时需要跟踪的相邻生命周期，不能误报已解决

metadata 改造本身并不完成 LL macro construction 接入：

- 当前 LL 只调用 `recordRawExpressionMacroSurfaces`，声明/参数宏尚无真正 construction；本设计要求由宿主经 providers 的 `MacroConstructionService` 抽象供给服务，LL 不依赖 compiler/frontend 实现。
- `MacroExpansionRegistry` 目前也被 service 直接 `session.register`。LL 多文件接入前必须给予它匹配的文件/代次发布与 source mapping 所有权，不能仅修 annotation 表后继续覆盖该全局实例。
- `CfirRawMacroSurfaceUsageStore` 当前以 session 强 `IdentityHashMap` 累积 CFIR/PSI 身份；不能把它当作新 metadata 索引，也不能宣称本文的弱索引已解决该独立 store 的生命周期。
- `RecordableRawCfirFiles` 是 batch 的完成证明；LL 文件缓存不具有 batch `CfirProviderImpl` 的 EMPTY/FINALIZED 状态机，不应机械套用全模块一次注册。

上述事项作为接入门禁保留，不使用 Identity 冒充真实完成，不新建“找不到服务则继续普通 resolve”的兼容分支。

## 11. 全量接入的测试与验收清单

1. 事务单测：record、同槽重复拒绝、derived slot 独立身份、migration 前后 carrier/current annotation、seal 后写入拒绝、abort 后拒绝发布。
2. provenance 测试：迁移后原 surface 的 cache hash 仍读取登记事实；已删除/未采用 fragment 不进入 published index。
3. 并发测试：同 session 两文件并行、先发布 A 后完成 B、A reader 与 B publication 并发；A metadata 不变且 B 仅完整可见。
4. 失败/取消测试：raw 或 fragment 或 splice 失败不改变已发布 A 的索引；失败事务无 session 强引用。
5. GC 测试：去掉旧文件/声明引用后，weak key 和 weak value 可回收；保留新代声明时 metadata 稳定可读。不能用仅 weak key、value 强引用 annotation 的实现通过测试。
6. owner 测试：参数 annotation 的 containing symbol 是父函数，但 snapshot.owner 仍为参数；字段派生 annotation 使用最终字段 owner；不同 generation 同名/同 offset 不串表。
7. LL 测试：首文件查询、第二文件查询、复制 PSI 文件、in-block rebuild、session 失效重建、临时 whole-file 重建只采用目标子树。
8. PSI/LT 真实 fragment 测试：custom annotation、declaration、parameter、嵌套 annotation arguments 继承宿主事务；不得为 fragment 安装另一 session registry。
9. raw fixtures 迁移：`CompileTimeVisibleParameterRawCfirTest`、`EnumConstructorAnnotationRawCfirTest` 从显式私有 writer/view 读取；不再用 session 索引检查未完成 raw 产品。
10. 包装调用方迁移：providers `MacroConstructionArchitectureTest` / `TokenBackedMacroFragmentParserTest`，frontend `FrontendMacroConstructionExecutionTest` / `FrontendMacroConstructionServiceTest` / `MacroPackageCompilationOrchestrationTest` / architecture guard，analysis-tests `CfirMacroAnnotationSourceModuleTest` / `CfirDeclarationOwnerTest` / `AbstractCfirAnalysisTestCase`，PSI raw `AbstractRawCfirBuilderTestCase`，LT `CfirOrganizationReexportTest`。所有构造 pre/input 的 fixture 必须显式交付实际 writer；纯手工无 annotation 图可显式创建空事务，不能由生产默认值兜底。

验收应先完成 provider 事务/发布测试与 raw 双入口测试，再运行 frontend 宏测试、完整 LL；最终 primary completion 仍需原设计中的 allkinds、字段关系与全量 CFIR 验证。
metadata 阶段的已执行范围见开头；真实 LL 宏构造、全部旧图强引用 owner 的回收及完整fragment construction不计为已完成。
