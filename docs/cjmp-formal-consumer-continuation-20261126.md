# CJMP B1/B2 正式消费接续（2026-11-26）

> 当前终态和后续修订见[2026-11-27消费修订](cjmp-source-consumer-repair-20261127.md)：r30-c111/111、r30-d505/466/39分别封存；r31编译和r32 selector/raw-slot修订继续，不按下方历史“正在编译”或r28结果替代当前事实。原70/S1–S7完整余额保留。

唯一实现树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。保留全部历史未提交修改、原70项/P0–P10、H6排除分母和v10待办；当前候选不授完整方案关闭。

上一已封存行为窗口仍是r28-d：498/481/17，SOURCE18含原14及copy/Published46均通过，完整serialization434/417/17。不得拿该窗口认证以下新消费者字节。

本轮从 `build/cjmp-invisible-peer-20261125/r28-normal-consumer-implementation-mapper.md` 直接接入生产，固定K/N责任映射和normal header边界补考证分别在 `build/r29-header-lifecycle-mapper.md`、`build/r29-declaration-completion-boundary-authority.md`。

实际新字节包括：

- CjoSourceContentRoot增加TypeMetadata、已登记SelectedExpression和DeclarationAnnotations；请求保存实际default/initializer/annotation/body选择，三域不混同。
- Context持有pure header registry并随CompletedGraph根发表；删除旧sourceOwnershipIndex/currentSourceOwnership/withSourceOwnership。canonical declaration、type缓存命中前检查冻结请求；TP inverse消费registry metadata。
- ClassLike/Generic/constant及声明完成队列保存原selection，根排空时恢复原scope。normal header在原声明上首次升级Unselected内容，旧Complete复用；所选root在队列生产时冻结。
- Loader.content消费显式保存槽，prepare生成不可变CfirBoundSourceContexts；decoder先发布所有实际d/F context/header，再投影本次body/default/initializer/annotation。删除两套borrowed miss扩张。canonical借用产品与ordinary节点/loop target完成前置分别检查。
- BodyState新增selected目录和辅助origin；package/header/selected/e/P来源图随alias同根换代，record/声明/type/F identity保留。公开content/load返回最终完成目录，非工作容器。
- 原normal annotation mapper明确接收默认值完成map。本包使用工作default产品，外包能力只请求实际遗漏参数的ParameterDefault槽；不升级目标自己的annotations/body。Owned/Published getter仍被动。
- provider.sourceGraphOf使用单F root；package/export接口才整包。sourceHeaderContentsOf显式完成default/annotation，frontend准备分别请求header、initializer、body。原LL factory仍沿同provider。
- 三个冷单e用例按独立authority候选改为显式F.body canonical discovery、initializer/loop准备和grouped map roots；保留无普通F.body发布、真实binding/shared/实际拥有检查。两处declaration-completion callback改观察旧完成代，最终真实default/annotation/Class/TP/回滚期待保留。

独立有界gate `build/r29-formal-consumer-gatekeeper.md` 曾拒G1 Generic队列丢scope、G2目标过度normal升级、G3缺ordinary完成证明；主线程据具体拒绝继续修订。最后G1 inverse producer也统一enqueueGenericType并在seed cache前检查授权，pending集合private。gate已限定撤销旧G1/G2/G3源码原因，不授完整B1/B2或新行为通过。

当前未冻结行为输入；最新编译queue84a8d61d / exec54046正在覆盖serialization测试源码、frontend、LL。编译间候选持续变动，须核当前字节终态后再按runner冻结，不能按早先1m15s单模块编译或b73787b0的通过证明全部后续修改。de51dd99编译失败及缺导入/旧interface产物窗口保留。

下一：核编译真实终态，修实际编译问题；用 `build/cjmp-invisible-peer-20261125/run-r26-request-validation.ps1 -FormalConsumer -SourceChain -PublishedTree` 冻结并执行header/request、type-first、BodyIntegration/DeclarationCompletion/OwnedClosure/PackageGraph与两parser原SOURCE矩阵；逐新key/原XML记录实际失败。随后完整serialization与正式frontend/LL消费继续。normal初始化依赖及匿名F/全部caller完整门仍须当前字节验证，不按局部源码或旧绿关闭。H6/v10边界不变。
