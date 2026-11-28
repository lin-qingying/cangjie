# CJMP SOURCE 公开载荷接续（2026-11-20）

本轮按[交付板](cjmp-source-delivery-board-20261119.md)集中处理P8/H1/H10/H13的同一canonical产品，不降低原70项/P0–P10或原SOURCE正确期望。唯一指定工作树及全部历史修改保留。完整owned closure、失败发布及S5/S6仍OPEN。

## 实际生产修订

- 公共SourceGraph.functionHeaders保存named/anonymous函数原参数分组、平行desugars的null/真实NamedFunction、原retType有无、captureKind、always、bodyAvailable；body0不会丢掉已保存header。
- 正式library provider增加sourceHeaderOf，复用同一context的声明/type binder及完成header缓存；只查询header不加载body。坏正文传播失败，此前完成的header/签名保持稳定。
- SOURCE literal保存Normal/JString/MultiLine/MultiLineRaw枚举；encoder/decoder完整映射。正常SOURCE producer按真实合法字面量分隔符保留多行/raw形态，isJavaString只作枚举派生。
- 在图的refs完成点建立MatchCase根pattern.contextExpression，引用所属Match.selector；此边不进入owning expressionChildren，case不能属于不同match。两个同型selector和pattern不会按结构合并。
- imported Position通过本context实际依赖snapshot选择，保留Position当前file一基/导入file零基合同；不复用FullId保留值规则。
- composite常量的classifier及字段symbol从实际SemaTy.CompositeTyInfo.FullId和选中context binder取得；不再通过公开ClassId getter重选首个parent或提前发布staging。

原SourceGraphLoader完整root/binder计划（F1/F2）、匿名F/helper外层关系、各provider/cache/tag全域失败发布、值产品统一，以及encoder退休legacy/LL/跨模式/版本等合同尚未据此关闭。

## 已执行验证与输入纠正

943a5465生产及tests闭包编译退出0。356ee2ce首29项25通过/4失败；725dfa50次29项27通过/2失败；各自真实终态与XML分别保存在build/cjmp-s5-type-20261119/public-payload-{first,second}-results.json，不拼接计数。第二轮没有完整事前冻结，不冒称同代行为验收。

新增测试起初错误假设native core含JString及SOURCE ConstPattern保存values。固定native/cjc实证表明native没有JString引用时J字面量语义类型为Invalid，其他三个为真实std.core.String；ConstPattern SOURCE不写values，正式writer应拒绝该新增不合法输入。[JString权威与官方probe](../build/cjmp-s5-type-20261119/jstring-authority.md)保存固定compiler/runtime/docs对象、SDK/core SHA、实际cjc诊断及AST。没有将JString冒充String/Int64、删除第四形态或skip；新增错误恢复输入保留JString形态+真正Invalid，成功定型JString的官方资源仍缺且不算完成。

坏body负例先由正式writer生成合法完整Block/Return/literal，再用生成FuncBody.mutateBody注入单一损坏槽；不让writer提前拒绝而假称材料化被验证。原正确fixtures/期望没有改变。

## 本批实际联合终态

d9fa41cd于2026-11-19T17:05:30.333Z至17:08:22.336Z实际DONE/退出0，47/47（公共载荷6、类型图13、Position10、原SOURCE14＋4reference18），无失败/错误/跳过。9905输入前后SHA均F73979335E8B29CC2835992FAD69127B14ECFC46621F9D1FBFC068AE2204CFBC；七份原XML11204字节在真实窗口且gzip/SHA无损核验。[事前输入](../build/cjmp-s5-type-20261119/public-payload-third-inputs.json)与[逐case/原XML](../build/cjmp-s5-type-20261119/public-payload-third-results.json)保留。

这是上述原子载荷/consumer的实际验证，不是完整H1/H9/S5/S6验收。F7/F8/F9及来源窄点需要最新独立源码复审；新跨parent position/composite/失败注入专用用例尚未覆盖，不能拿47绿色替代这些门。继续集中owned closure与统一reader/encoder、正式跨模式/LL消费，完成后按原ID/子要求收口。
