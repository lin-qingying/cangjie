# CJMP loaded-snapshot 身份实施接续（2026-11-18）

最新实际窗口5a657814：173/173，0失败/错误/跳过。6843输入事前/事后同SHA 4D2FDC9D64F5B1A018240458EAC44B9914AB762AB25AA7E0E3BBC135A1A7E662，17原XML共36710字节全部在真实队列窗口。完整逐case与压缩原XML见[ledger](cjmp-source-helper-20261117/source-snapshot-joint-173-results.json)。

本轮复验serialization46、owned-copy19、S036、canonical8、helper18、原负例2、SOURCE fixtures14及完整stored-CJO30。原SOURCE14在这一新候选窗口仍14/14；没有把早先101或90与本轮合并计数。原70/P0–P10的前端范围、所有后端排除余额与v10待办继续保留，目标未完成。

## 实际生产变化

- CjoSnapshotTableEntry以实际snapshot对象身份、DECLARATION/EXPRESSION/TYPE/VALUE/FILE域、raw零基下标为键。全部imported d/e/t、declaration origin/counterpart、file、composite value和scan memo迁移，删除路径型表项memo。
- CjoExportSourceSnapshots.uniqueLoadedPackages只复用同一对象；同路径不同读取事务明确冲突，即使内容相同。Frontend输入汇集和Producer初始catalog共用该门，避免path-only distinctBy/associateBy吞掉冲突。
- cjoPackage按每个借用声明自身session/provider查询本代已实际读取的snapshot，再校验同路径输入身份；不重新打开CJO。
- CjoManager持有只读buffer，Package accessor不能修改其读取事务的字节。原pkg/header/sourcePath仍属于同一读取，文件替换必须重建manager/provider/session。
- 额外广域回归发现旧CjoDefaultValueMetadataTest没有Source provider登记，已通过既有registerCjoSourceFixture的正式construction路径登记，正确源及期望未改。随后暴露生产collect(helper,false)把非物理root中发现的顶层helper强制降为成员；现从同代exportAnalysis.isTopLevel(helper)消费真实归属，不按发现路径改变拓扑。

新CjoExportSourceSnapshotsTest五个真实manager用例覆盖同一对象重复复用、同路径相同字节但不同读取、文件替换与旧代稳定性、buffer禁止修改、不同路径的同包输入保持独立。

## 固定源码与审查

继续使用Kotlin 3c7a5041f238102285764d370665f56f587f9dc4和native 14276740ffec1262b81b6b5236b1d840af270b37。身份合同及模块职责复用source-canonical-codec-kotlin-continuation-20261117.md第149行，新独立[限定源码ACCEPT](../build/source-expression-codec-audit-v113/source-snapshot-identity-review-20261118.md)核对声明/reader/cache/module输入职责及实际readonly owner。

ACCEPT只覆盖本轮loaded-snapshot身份源码门，不关闭S4完整。helper顶层发现路径修订随后由真实90及173回归证明；没有把前一源代码报告称为所有后续字节的全域parity。未联系停止旧会话、未修改主仓库或external、未提交或丢弃历史修改。

## 独立窗口

- 338b9fce：90项88通过/2缺source provider失败，6843同SHA，完整原XML在窗口。
- 2bd19af1：90项89通过/1helper顶层归属失败，6843同SHA，正确期望保持。
- cc1e0c85：90/90，6843同SHA，9原XML19703字节在窗口。
- 5a657814：本轮173/173，同代字节完整扩大回归。其input/逐case/XML独立保存，不能把前两失败或前90拼接为173。

## 下一生产工作及仍未完成范围

loaded-snapshot/raw域身份这一S4必要合同已有上述限定源码与行为门。完整S4仍须退休expressionOrNull、活动Cone/provider查询和依active owner变化的type memo；providers须冻结完整有序类型payload、declared self type、alias Type、This、RawArray dims、Generic语义bounds与原始constraint节点及位置。固定common→specific TP身份投影和source/imported共用完整encoder继续实现，不能把查过typeElement再丢弃其结果称为冻结消费。

S3其余optional/String/Lock/effect/composite canonical产品、来源位置/provenance与完整Return/tail合同；S5统一decoder/source materialization，S6 ordinary/common/depends-on和LL正式正文消费；S7原P8/E6/G1及70前端/P0–P10全部验收仍OPEN。header/default可用性、raw重导出数量和本轮173不能替代选中SOURCE body实际恢复/分析。通用R5只按真实前端consumer需求，不扩后端或全框架DFA。

所有此前codec/code/SDK证据见[cjmp-source-codec-continuation-20261118.md](cjmp-source-codec-continuation-20261118.md)。原C3/C4/D5/E6/E7/E8/E9/E12/F4/G1/G3/G5、H1/H9/H10/H13、H6前端门、P0/P10、49版本及v10待办不缩减、不新增整体完成率。
