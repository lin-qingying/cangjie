# CJMP SOURCE 类型 decoder 接续（2026-11-19）

> 第五轮6c839ad6实际SOURCE18/18，2026-11-19T08:22:24.306Z至08:24:14.120Z，原SOURCE14＋4reference全部通过；six-fixture两parser的新consumer实际走fresh CJO→公开frontend→Library sourceGraphOf→typed canonical正文/绑定/位置/loop/Return/模式检查。9899输入前后SHA同为29BD5E31263A61356E9386BF05F6A236C0F86E4536D4BED5D71DB0B2950FEE98；四原XML4601字节全部真实窗口。[完整证据](../build/cjmp-s5-type-20261119/source-consumer-fifth-results.json)。这是正式消费子要求的有限交付，不关闭全H1/H9/S5/S6、R5、版本和失效合同。

> 前一第四轮adbc8121实际52项50通过/2新operator拼写断言失败，34个constructor/header/type/SDK全部通过、Source16/18。9899同SHA5B0840…，[第四证据](../build/cjmp-s5-type-20261119/source-consumer-fourth-results.json)独立保留。实际CFIR constructor语义结果现复用owner从t池获取的self（class/struct/enum），不将native FuncBody.retType误当语义结果；两Enum构造项及普通构造器共用此合同。第五仅修canonical operator name↔保存token的标准投影断言，原wire字符串和原fixture未降低。

> 原ID交付与集中拒绝项／必要consumer见[交付板](cjmp-source-delivery-board-20261119.md)。C4/D5、E7/E12、G3的独立固定源码/正确数据准备已并行派发，输出落build/cjmp-parallel-20261119；生产仍由主线程按共享写集顺序集成。原70前端/P0–P10不按局部比率计完成。

> 第三轮82c26e8a真实serialization34项32通过/2失败，SOURCE任务因前一失败未执行；9899输入同SHAD42AF4…，原XML独立保存在[第三轮结果](../build/cjmp-s5-type-20261119/source-consumer-third-results.json)。两失败说明constructor的CFIR语义结果必须是owner self而非FuncBody.retType/FuncTy返回槽的任意值。已在class-like owner上下文保存实际t池self，在三个constructor入口复用同一self Cone并保留独立TypeRef；第四轮扩大回归已提交，输入见[第四轮冻结](../build/cjmp-s5-type-20261119/source-consumer-fourth-inputs.json)，未预记通过。

> 正文首轮d79a9f6e真实18项6通过/12失败，9899输入同SHA282CDE4…，失败均在临时consumer未实际使用导入而产生正确UNUSED_IMPORT；原wire/fixture断言已到达，不能把这轮计作完成消费。临时consumer已改为实际参数/返回使用库接口；另修共享pattern binding登记与新增catch-prefix/tail断言。第二轮a21f0c78真实6项4通过/2失败，9899输入同SHA63E73E65…；两parser的declarationsCalls/controlFlow已通过完整正式SOURCE graph消费，exceptionsResources失败在构造器returnType重新创建Cone导致同一t身份断裂。原CJ及全部原期望未改变。

> 普通constructor、enum的FuncDecl/VarDecl现改为统一CJO type reader消费实际保存类型，并删除旧return-self重构helper。第三轮已提交扩大SOURCE18＋类型/header/SDK回归，事前输入见[第三轮输入](../build/cjmp-s5-type-20261119/source-consumer-third-inputs.json)；未核终态前不记通过。第一、二轮原XML/gzip与逐case见[首轮](../build/cjmp-s5-type-20261119/source-consumer-first-results.json)、[第二轮](../build/cjmp-s5-type-20261119/source-consumer-second-results.json)，各自独立不拼计数。

> [完整decoder Gate10](cjmp-source-helper-20261117/s5-source-decoder-kotlin-parity-gatekeeper-20261119.md)与[当前限定复审](cjmp-source-helper-20261117/source-decoder-gate-current-20261119.md)仍REJECT完整图；F3/F10仅原拒绝点窄门接受。F1/F2的完整owned root/binder计划、F4/F5跨provider发布与精确classifier、F6外包Position计划、F7公开named header、F8四种string kind、F9 selector ctx仍待完整实施/验收；不能以四个正文消费通过关闭整层。

> 后续正文候选已接入正式 library session 的 importedSourceGraphProvider/sourceGraphOf 能力；SOURCE30+3 使用同一 CfirSourceGraph，保留 typed declaration/type、Return/loop/map 身份、mixed d/e、参数与真实 NamedFunction desugars、原行列和类型尾槽。新增 pattern 常量缓存使用 raw union 与对象壳 memo 保留 allValues 共享/cycle。221faaea 首次 entrypoint/闭包编译退出0；之后的字段/常量/测试修订由2bc4253c独立compileTest窗口验证中，不预记行为通过。原73窗口先于正文候选，不证明当前字节。

> 新测试在原 SourceExpressionCjoHandler 的 fresh SAVE_CJO/原严格 wire 断言之后，用临时 UTF8 import consumer 再运行公开 frontend；通过实际 Library module session 请求 sourceGraphOf，再比较 typed 图和原 snapshot。未注入 source 符号或改正确 fixture/原期望。正文完整独立 gate 尚待本候选审查；Library CFIR body/完整分析能力、统一 reader/encoder 收敛和S5/S6仍不关闭。

本记录接续 [类型使用图](cjmp-source-type-use-continuation-20261119.md) 与 [SOURCE 完整关闭清单](cjmp-source-chain-delivery-20261116.md)。唯一实现树与全部历史未提交修改保留；原70项/P0–P10不缩减，H6排除余额保留分母、不实施，v10待办不阻断其它授权工作。没有联系停止的会话，没有提交、归档或关闭目标。

## 本次生产修订

- TypeKind.Type 按真实 Alias FullId、classifier 与有序实参恢复；RHS仍由 Alias 声明单独持有，不在类型reader展开。
- owner 的全部 own TP 先建立真实symbol与参数壳，再读取同表GenericTyInfo和原constraints。GenericParamDecl.type缺失时，仍通过实际同表GenericTyInfo的FullId索引恢复；没有临时同名symbol、recursive fallback或默认Unit。
- CURRENT/外包TP均回指真实声明。声明索引包含成员、自有泛型、参数的实际owner边，canonical入口先恢复owner，拒绝拥有环。
- Generic类型先登记身份再读上界，合法F-bound保留共享回边；纯结构循环明确失败。GenericTyInfo最终上界与原constraints分组分开保存，重复、位置和implicit标记不丢失。
- 同一context使用可重入事务。types/declarations/constraints在私有staging完成后一次发布CompletedGraph；失败清除本次新增状态，公开缓存只读。未以BODY phase、空CFG或body=0声称正文完成。
- imported FullId在exportId查找前使用请求输入模块的真实dependency/refinement闭包选目标snapshot。多候选无唯一证据时失败；后续provider按所选精确路径取得canonical声明。
- VArray要求真实ArrayTyInfo，真实size=0保留，缺字段不补零；pointer缺pointee不补Unit，wire类型0明确Invalid。
- 扩大回归暴露内建RawArray/VArray/CFunc的normal CFIR隐式Any上界丢失。已在内建声明创建owner按既有BuiltinKind恢复正式factory的header合同；native最终上界产品保持独立，CPointer继续保留显式CType。原正确测试期望未改。

实际生产修改为serialization/deserialize中的CfirTypeDeserializer、CfirDeclDeserializer、CfirDeserializationContext、CjoFullIdResolver，以及provider中的AbstractCfirDeserializedSymbolProvider、CfirDeserializedSymbolProvider。未修改生成器、正确SOURCE程序或旧fixture期望。

## 固定依据与审查

使用新的Mapper/Authority/TestWriter/Gatekeeper，Kotlin固定3c7a5041f238102285764d370665f56f587f9dc4、native固定14276740ffec1262b81b6b5236b1d840af270b37，逐文件git show读取。

- [Kotlin框架映射](cjmp-source-helper-20261117/s5-kotlin-framework-mapper-20261119.md)：FirTypeDeserializer先reserve own TP再填bounds，outer binder引用真实符号。
- [native类型身份合同](cjmp-source-helper-20261117/s5-type-identity-native-authority-20261119.md)：SetGenericTy先存身份再读取上界，原constraints独立恢复。
- [完整SOURCE loader合同](cjmp-source-helper-20261117/s5-astloader-source-native-authority-20261119.md)：33角色、d/e/t、decls→refs→post-reference与正式consumer，供后续正文实现直接使用。
- [首次限定REJECT](cjmp-source-helper-20261117/s5-type-loader-limited-gatekeeper-20261119.md)的snapshot first与VArray默认零已修；[限定复审](cjmp-source-helper-20261117/s5-type-loader-r1-r2-recheck-20261119.md)及[实际测试断言复审](cjmp-source-helper-20261117/s5-type-loader-r1-r2-final-gate-20261119.md)只接受R1/R2对应源码门，不替代完整S5或最后Builtin修订审查。

## 实际验证窗口

add987db生产编译失败（真实import/paramLists API），f35d86a1生产编译退出0；b01931f8测试编译失败（新增测试的type扩展导入），762c70d3测试编译退出0。编译成功不计行为通过，未声称这些编译有完整事前冻结。

6624e429第一次行为窗口实际55项：51通过、1失败、3跳过；新类型图13/13，唯一失败是内建normal隐式Any合同。9894输入前后同SHA 7BD1A5E57BA78A3B2241F5D7015BC90E0AF0B6CD2F62FC767782D314C7B65F1C；七份原XML均在真实queue窗口，原字节gzip/SHA验证与逐case台账见 [第一次结果](../build/cjmp-s5-type-20261119/smoke-results.json)。三个原有SDK跳过真实保留，未删除或计作通过。

Builtin修订后的联合窗口a26341c6实际DONE/退出0，2026-11-19T05:39:23.314Z至05:45:56.557Z。73项70通过、0失败/错误、3原有跳过；新类型图13/13，原SOURCE14/14及新四reference4/4，serialization扩大回归52通过/3跳过。9894输入SHA前后同为27736B6BC3E2965CFDAC1CC1F10FC3137069A00988377EFF4C7A1D6C38045AF1；十一份原XML共24521字节全部在真实窗口，并已压缩往返核验。见 [事前冻结](../build/cjmp-s5-type-20261119/final-inputs.json) 与 [完整逐case/原XML终态](../build/cjmp-s5-type-20261119/final-results.json)。未拼接第一次55或其它历史窗口。

最后Builtin修订的新增独立复审因服务HTTP502终止，没有产生ACCEPT；保留已完成的R1/R2有限源码门和上述真实行为证据，不用测试替代缺失复审。后续继续复审normal implicit bound与native final bounds的独立合同。

## 尚未闭合与直接接续

完整S5/S6仍OPEN：Alias精确classifier绑定/正式展开使用视图，33角色统一正文materialization，局部decl/lambda/Return/loop及所有非拥有边，同代位置与身份，Header-only/Source能力、普通/common/depends-on与CLI/LL真实正文消费。当前Library函数body仍null，四种annotation expression reader尚未退休，不能把新类型decoder当完整SOURCE恢复。

S4/T6的SOURCE最终语义上界、原constraint组完整normal生命周期与strict capture，继承H1–H3、late ctor D1/D3/K01、R5全域未接受边界，以及原计划其它余额继续保留。本轮不新增原条目整项分数或整体完成率；原SOURCE14只作为必要回归。
