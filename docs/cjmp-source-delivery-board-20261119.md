# CJMP SOURCE 正式消费交付板（2026-11-19）

> 最新[赋值事件接续](cjmp-assignment-occurrence-continuation-20261123.md)：092c9286实际415/415、同10032输入/SDK84SHA，源码门接受精确mini事件/共享克隆/首次二次映射/真实NamedFunction/helper fork。本片未完成value/Binding/运行顺序/typed setter与正式Source格式收口，CLI/LL与原整项继续OPEN；旧405/377等时点不替代新代码或被重复相加。

> 2026-11-23后续48/48、SOURCE联合377/377及未闭边界见[G2/flow接续](cjmp-g3-flow-variable-continuation-20261123.md)。Flow八表/factory仅为基础产物；公共analyzer、正式sparse replay、完整SOURCE消费与原70继续OPEN，以下旧表按历史时点保留。

> 2026-11-21最新[省略签名接续](cjmp-source-signature-continuation-20261121.md)：06ac310a实际373/3失败，SOURCE18、consumer2、canonical8、snapshot40/copy21通过；正式binder签名/候选门限定ACCEPT。完整annotation及两实际输入缺口、generated owner旧失败、跨模式/LL/CLI/原70项仍OPEN，旧时点计数不覆盖本轮。

> 当前[统一encoder接续](cjmp-source-unified-encoder-continuation-20261120.md)保留955eeb4a实际287/9失败、SOURCE16/18、完整原XML及第一gate的R3/R4/R6/R9修复队列；新保存计划/属性type/选择闭包修订正在编译。旧SDK266与SOURCE18不替代当前字节验收，完整原交付保持OPEN。

> 最新[统一值/精确类型接续](cjmp-source-values-continuation-20261120.md)记录8121a14a SDK联合266/266、原SOURCE18、9908/84SDK同SHA及全部失败/gate。SOURCE/imported unified encoder退休legacy及正式跨模式/LL消费继续OPEN；旧表各时点不覆盖最新已交付子要求。

> 2026-11-20最新实际状态见[拥有闭包/F接续](cjmp-source-owned-closure-continuation-20261120.md)：联合99/99、原SOURCE18、9907同SHA，第三gate消除R1–R3/R2-H指定拒绝；下一统一VarInfo/allValues typed reader。下面各行旧时点状态保留，不覆盖最新产品与完整S5/S6余额。

本板不替代[原70项/P0–P10](cjmp-framework-review-and-fix-plan-20260928.md)或[S0–S7关闭条件](cjmp-source-chain-delivery-20261116.md)。以下按原前端消费者组织完整产品批次，保留所有正确源码、期望和历史失败；H6后端余额不实施、不计完成。没有完整原验收的项继续OPEN，不用局部通过率计算条目完成。

## 正文产品集中关闭清单

| 批次／原归属 | 完整关闭合同与实际阻断 | 实际证据与状态 | 下一统一修改／必要验收 |
| --- | --- | --- | --- |
| owned closure／P8 H1、H9；E6前端 | 从真实top-level/nominal/FuncBody/Block/Pattern/资源/annotation roots建立拥有计划；d/e/F及params/helpers先登记后填。引用者不能认领borrowed目标，Return绑定实际F，map/loop只指已有拥有域。 | Gate F1/F2及R3/R4 OPEN；Body接口中的declarationsCalls/controlFlow两parser已消费通过，但不覆盖独立全局/成员initializer、同名local函数、多F和失败域。 | 同一root plan集中修loader、declaration binder及F registry；测试全局/成员lambda initializer、两个同名local函数、匿名F helper、嵌套initializer Return、无合法owned root的回边。此批次完成后才发布“完整内容闭包”。 |
| 公共canonical载荷／P8 H1、H10、H13 | 同一公开图保存named/anonymous F原header、参数分组/对齐desugars/retType有无/capture/always；四值StringKind；Pattern ctxExpr、类型前缀/尾槽、原位置和值能力。 | F3/F10原拒绝点源码窄门通过；F7/F8/F9 OPEN。named header目前被内部wrapper截留，外部graph缺字段；StringKind压成bool；selector非拥有边未发布。 | 一次扩充现有SourceGraph产品并接decoder/encoder/公开provider，退休信息压缩；按公开API测试两参数列表、空helper槽、retType0且signature非0、四种字符串、两个不同Match的相同shape patterns。 |
| 正常语义类型与保存槽／P8 H1；E6必要header消费 | constructor的CFIR结果取owner self，FuncBody.retType是独立原生保存槽；class/struct/enum及构造项使用同一t池／TP binder，不因框架重构造丢identity。 | 首消费exceptionsResources 2失败为fresh self Cone；第三窗口34项32通过/2正确constructor语义期望失败，SOURCE未执行。现已记录owner self来自t池、独立TypeRef；第四窗口运行中。 | 合并同一合同一次验证constructor、enum Var/Func形式、泛型owner/前序params、普通header与正文身份；正确原assert不翻转，不再逐case补类型。 |
| 精确来源与失败发布／P8 H1/H9/H10/H13；G1必要消费 | FullId、classifier、Position均使用实际依赖选中的snapshot；内部查询不得通过公开provider/tag提前发布staging；取消/后置失败保持旧对象并可安全重试。 | TP/普通FullId已按实际module dependency选择；F4/F5/F6仍OPEN，const classifier与Position走公开first。context容器原子提交不代表provider/tag完成。 | constant与position读同一个transaction-owned binder／snapshot registry；完成对象才进入公开cache/tag。测试两个同包同class/field/FileIndex parent，交换顺序；后置失败同时核context/provider/tag/重试identity，不只检查sourceGraph=null。 |
| 正式前端消费／P8 H1/H9；E6/G1前端 | fresh SOURCE→SAVE_CJO→ordinary/common/depends-on正常加载→header/source能力→真实consumer；CLI与LL按同一产品读取，分析需求另由真实readiness门证明。 | 新consumer已走公开frontend和实际Library provider；原SOURCE14＋4refs仍是必要底线。两fixture双parser已消费通过；完整ordinary/common/depends-on/LL、取消/失效、R5实际依赖及49版本/P0/P10未验。 | 完成上述产品后集中执行原SOURCE18＋生产reader/canonical合同及实际前端两段式／LL模式矩阵；原v10逐key暂缺不阻断其它授权工作。 |

原关系：完整CJO/SOURCE前端恢复是H1/H9/H10/H13的要求，默认实现跨SOURCE/CJO关系落E6，正常CLI/LL接入落G1。R5仅按这些真实consumer的分析需求确定，不因完整SOURCE图读取自动扩大整个DFA/后端；现有完整REJECT边界不删除。

## 不依赖正文decoder的并行原余额

| 原ID | 本轮并行产物／实际范围 | 修改整合约束 |
| --- | --- | --- |
| C4/D5 | 固定源码核对共享member shadow判据、owner/extend的声明级实现关系，核磁盘并准备正确红数据。 | 产物先落build/cjmp-parallel-20261119；生产shared scope写集主线程顺序整合，不与当前decoder冻结输入并写。 |
| E7/E12 | 固定禁止注解集／实际checker dispatch形状，核对是否仍有跨checker抑制及缺公共调度产品。 | 只做原ID所需的checker/annotation消费者，不扩所有检查器；正确测试先准备，生成器与生产按队列合批。 |
| G3 | 正常LL文件→module真实归属，核test-only provider与production差距及模块身份。 | 与decoder关系只按实际消费者判断；新增数据准备不改变正在验证的输入。 |

以上是固定源码考证与测试准备的授权工作分解，不是已经完成或通过的宣称。各完整原验收从原计划逐条保留；成果、阻断和真实终态落盘后才能将相应子要求改为交付。
