# CJMP peer 完成事实与纯裁决接续（2026-11-25）

唯一实现树不变，原70/P0–P10、完整SOURCE和H6排除/v10待办均保留。本批完成事实准备与裁决职责迁移，不关闭完整R7/声明继承检查/原条目。

## 实际实现

Core新增`CfirExtendMemberPeerFacts`和三个独立boolean关系，声明按物理身份比较，完整ordered matrix参与equals/hash；输入集合和输出冲突witness嵌套冻结。具体Cone、Source/provider、捕获target/接口/params/bounds、alias callback放semantics.api准备输入，不向Core泄漏实现。

默认service与scope显式source输入共用`prepareExtendMemberPeerFacts → extendMemberPeerDecision(facts)`。纯裁决不读resolver/provider/header/callback。所有target递归展开后才取实际key/声明symbol；接口的递归展开和分类在同一producer。源码别名旧节点缓存不能提前决定EXCLUDE；same ClassId不同fixed声明不能假定同一目标。

normalizer显式入口只使用捕获参数、bounds及本次alias callback。owner接口按currentArgs→peerArgs顺序逐步投影；结构递归重复TP使用首映射，不要求所有TP覆盖，也不授候选适用性。equal短路实际subtype请求；两个方向独立、last witness与跨row冲突规则保持固定N。

ScopeInput保存完成facts及disposition，名称查询不重新生产顺序事实。canonical typeContext仍独立登记自身来源/版本，不能把selected graph当canonical判据。private/ordinary成员和原正确body/期望保留。

固定K=`3c7a5041f238102285764d370665f56f587f9dc4`/N=`14276740ffec1262b81b6b5236b1d840af270b37`。新mapper、authority、test writer与gate工作完成；当前facts slice源码APPROVE，范围不含invisible表。报告见`build/cjmp-peer-facts-20261125/`。

## 独立真实窗口

- `8bbf5f8a`：初次生产编译成功，早于后续修订，行为0。
- `91582b43`：编译失败，原Scope投影loop漏type扩展import，行为0；真实终端/队列保留。
- `e5883d22`：57项56通过/1失败，缺少新peer参数域Parent<PeerU>的fixture父图。EXCLUDE正确期望保留，只补实际peer-domain根接口及Child父引用。
- `6ec227c4`：编译失败，跨module公开nullable属性无法smartcast，已改local val捕获，不放宽类型。
- `450af98d`：92项91通过/1失败：SOURCE32/32、scope15/15、Index24/24、facts21中的20通过；LL生产编译成功。失败发生于新Source alias模型缺source元素，尚未到producer。已补有效model source，不改21语义断言。十三原XML与终端独立保存，不以该窗口授新修正通过。
- `06f540f9`：当前修正实跑60/60，facts21、Index24、scope15，零失败/错误/跳过；三份原gzip XML及全部逐key保存，同选定输入窗口单独记录。与旧92窗口不累计。

## 继续完整实施

当前`matchedPeers`仍是严格candidate匹配，invisible检查表未接。下一必须从原桶召回peer，独立保存peer→currentTarget结构映射、实例化可见性、接口与直接成员表。visible/invisible分表归并，按每个当前声明更新前保存检查输入；仅visible进入requirement和最终发布。不能用late generic碰撞、默认空表、去掉约束或把invisible塞published表替代。

正式consumer是`CfirExtendInheritanceDeepChecker.check → checkExtend`的kind/return/property一致性检查，须消费映射后的完整签名而非重读symbol返回类型；要求所有成员种类，不把现函数投影冒称全部MemberMap。独立源证据/具体接口接线见`invisible-peer-consumer-authority.md`。完整前端92及CLI/LL/版本/原70最终验收仍待完成，目标active，未提交或归档。
