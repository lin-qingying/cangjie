# CJMP SOURCE 父图产品与正式消费接续（2026-11-25）

唯一实现工作树不变：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10、SOURCE完整关闭条件、H6排除余额保留分母、v10待办均保留。以下是候选与真实证据，不授整项完成。

## 已落盘的生产候选

- `CfirDirectSupertypeProvider` 按原symbol区分完成产品与未发布；完整refs、declared/extend边及cycle状态独立保存。marker-only确定循环可发布true，但原始未解析引用不授完整产品或false循环结论。
- `CfirSuperTypeGraphStore` 声明与extend都按symbol记录。ClassId名称查询多owner时明确拒绝歧义；断环入口用原symbol。工作修改不泄漏进已发布产品。
- 默认Object真实引用进入`storeSupertypes`之前，supplier、断环拓扑、AST与发布产品保留同一引用。缺Object发布`DiagnosticKind.NoCoreObject`错误首槽，factory为`NO_CORE_OBJECT`，消息为官方的“`core` package should be imported”。同输入重放不追加第二个错误。Object自身的Any保留实际stdlib显式引用，不按缺concrete父类重复补。
- scope/typed不再查询时构造phantom Object/Any；`ImplicitObject(ownerClassId)`来源与无根据统一SKIP退休，正式引用保持`Declared(sourceTypeRef)`。
- Source读取原owner发布产品，CJO读取自身完成header，显式graph固定消费指定产品。Source循环过滤读取发布结论；生产分类显式消费本次工作循环状态。固定symbol不经ClassId重查。
- typed记录声明、目标及alias各自实际Source session依赖；stamp纯观察当前provider/graph身份与版本。alias展开使用实际选择的symbols及发布RHS。
- scope父准备保留原symbol链，未绑定nominal头在scope拥有域固定且保留全部args/attributes/flags；Source root/target/alias epoch进入同代依赖。结构原请求的alias失效后从原请求重新准备，不在旧alias seed内部永久重试。

这批代码尚未完整验收。extend准备之外的名称查询仍动态调用目标/alias代换（`CfirExtendMemberScope.membersForScopeView`、`ownerInstantiationFunctions`、`isApplicableAtReceiver`）；最后独立审查据此保留R7 REJECT。下一步在原ScopeInput/准备链内完成可审计匹配及实际owner映射，不能保存live resolver回调或改正确重载/private期望。

## 固定源码与审查

K=`3c7a5041f238102285764d370665f56f587f9dc4`，N=`14276740ffec1262b81b6b5236b1d840af270b37`。原证据位于`build/cjmp-source-owner-20261124/supertype-kotlin-map.md`与`supertype-native-authority.md`。根引用应由producer生成、consumer读取的职责已考证。K无仓颉extend，CJ语义差异须按固定N证明，不把本地ScopeInput称为上游同名类。

新审查逐轮撤销已修R1–R6的原具体问题，但R7外层展开/旧epoch重试/query时alias读取连续发现新缺口；不能以旧PASS或生产compile代替当前批次审查。2026-11-25新建mapper与authority正在核对冻结extend匹配的准确职责，不联系已停止会话。

## 独立终态与当前窗口

- 旧正常SDK`4f723f48`实跑169项159通过/原10失败，SOURCE32含原14全部通过，属于父图候选之前的字节，不替代新验收。
- `a39e6213`父图探索窗实际72项68通过/4失败，原5份gzip XML与终端保存于`build/cjmp-source-supertype-20261124/`。生产/测试修订与该窗口重叠，没有事前冻结，不声称同输入验收。失败包括raw builder漏source、禁止phase回退、断环应观察error delegated Object，以及alias/cycle模型发布。前三测试装配及alias正式错误RHS已修，尚无本轮通过证据。
- `52fbb643`于本次接续后启动，正式执行原5组父图/graph/typed/extend index/scope cache回归；当前应以实际exec/queue终态核验，不预记通过。所选9,991输入事前SHA `6A61D56AA249910BEFA951EFD8ADC706CA8BCCCCB76F29F740E4B7F467F64FB4`，使用明确SDK标准库搜索根。该选定算法不覆盖generators/top-level脚本，不能冒称全仓冻结。
- `52fbb643`最新已DONE/退出1：resolve测试编译失败，raw UserTypeRefBuilder的source不能为null；resolve行为0。providers实际9/9，两个原XML/终端/窗口独立保存。已用明确的Future source模型修复该装配，未删marker-only断言。
- `d51c92f7`初次extend冻结候选的providers生产/resolve测试源码编译成功，DONE0；不含行为，不替代后续修订。
- `dcd7bae0`正在执行父图五组与完整SOURCE32（包括原SOURCE14）。scope生命周期测试还在追加，终态需核实际输入窗口；不预记通过。
- `dcd7bae0`最新已DONE/退出1，实际111项108通过/3失败：resolve父图64/64、SOURCE32/32，providers scope15项12通过/3失败；十五原XML独立保存。本窗口与新scope测试/生产精准bare修订重叠，不声称最终代码同输入验收。两primitive fixture的nominal dispatch槽错误和fully-instantiated Bool被误判deferred已分别修正，正确语义断言未翻转。
- `0a7ab4b7`正在当前代码复跑scope生命周期组，旧三失败窗口保留；不预授源码完整ACCEPT。
- `0a7ab4b7`最新DONE/退出0，scope生命周期15/15、零失败/错误/跳过，一份原gzip XML、逐case与完整terminal独立保存。新准确bare/self检查与primitive nominal dispatch槽修订都在本窗口实际验收；与旧111窗口不累计。源码完整审查仍REJECT peer域/不可见表，不能据15绿关闭整项。

## 本次实际extend冻结修订

`CfirExtendMemberScope.ScopeInput`新增plain replacement map、EXACT/INAPPLICABLE/DEFERRED_BARE、Rscope/Rcandidate各自ReceiverKey、已捕获view成员序列、member投影、peer匹配与分类。名称查询的members/applicability、owner表代换不再重新create/find匹配；runtime substitutor缓存不参与data输入相等性。准备前建立alias callback/epoch依赖，实例化父表使用准备的owner map；ordinary移动成员也进入投影，不按名称或private删除。

独立门仍REJECT。已按具体意见修复EXACT普通成员缺owner mapping时不得退模板、成员map只聚合active owner、stored structType使用准备的owner receiver。仍OPEN的根缺口：peer分类使用canonical service输入，尚未准备source-aware peer关系事实；constraint-invisible peer的独立检查成员表未实现，不能把严格candidate匹配表当peer召回或将invisible表塞入最终发布。

下一完整写集已用固定K/N重核：common service的关系facts DTO/纯裁决、semantics.api具体准备输入、resolve唯一事实生产器/裁决算法及normalizer、成员映射与applicability分层、scope独立peer域visible/invisible产品和正式checker消费。旧两个参数service入口与scope显式来源须共享同一算法，不能复制判据或增加适配兜底。范围仍是原方案SOURCE/声明级实现关系所需，不扩为无关全编译器工作。

下一完整验收仍是当前候选修复、独立审查、受影响模块与原SOURCE14/正式consumer、完整frontend92及原条目全部要求。局部回归不关闭SOURCE/70；所有历史修改、失败、正确源码及期望保留，未提交或归档。
