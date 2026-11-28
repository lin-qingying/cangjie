# CJMP SOURCE 拥有闭包与 F 绑定接续（2026-11-20）

本批继续原 P8/H1/H9 及 E6/G1 必要前端产品，唯一工作树及全部历史修改保留。原70项/P0–P10、H6排除余额和v10待办不变，不以本批通过关闭完整S5/S6或原条目。

## 实际生产修订

- CjoSourceOwnershipPlan 从真实 top-level roots 沿 nominal/enum/property/Block/F/Pattern/annotation/resources/future 的拥有槽建立完整 d/e/F/P 计划。重复同 parent 槽保留源序；不同 owning parent 拒绝。map、Jump、普通 FullId 不取得所有权。
- SOURCE references 只查询已登记 d/F/e；取消全表 FuncDecl 非局部创建入口。全局/成员 initializer、独立默认值、绑定模式和 annotation e 进入同一闭包。Return 保留变量/参数表达式 context，并绑定实际 F.body 域。
- 内联 F 使用同一 loaded package 内真实 table 地址，不用名称或 body 下标代替。context.sourceFunctions 与 d/t/header 同次完成发布；首次 local/TP/helper/匿名参数 header 查询只物化实际拥有链，后续 SOURCE 复用原 symbol/F/参数。
- Header 的纯拥有索引不读 map、Ref/Member.target、Array.initFunc、Anno.target。完整 SOURCE 验证仍严格读取这些借用字段；坏 borrowed target 不阻断可定位 header，随后 SOURCE 必须失败。
- 匿名 F 的 body=0 是真实不可用槽；保留参数分组、header、实际 function/symbol 和词法 F 关系，不造空 Block。统一 F encoder 保留 imported header 的原分组、0 helper、retType 有无及 body0。
- Property accessors 在共享声明缓存中保留真实 accessor/参数角色；pattern binding 沿已有共享 binding d，不再造 field-variable 替身。helper 的参数拥有边、词法 context 和 native 第一参数表 ownerFunc 分开。
- SOURCE 类型引用在缓存命中前核验实际 CURRENT d 及递归 typeArgs/Func.ret/Generic.bounds；无合法 owning root 的 TP/owner 不得被普通类型 binder 创建。公开 SOURCE 图保存拥有关系、派生 CFIR 词法 F 和 canonical annotation e。

固定依据为本批 [Kotlin mapper](../build/cjmp-owned-closure-20261120/kotlin-framework-map.md) 与 [native authority](../build/cjmp-owned-closure-20261120/native-owned-closure-authority.md)。Kotlin=3c7a5041f238102285764d370665f56f587f9dc4，native=14276740ffec1262b81b6b5236b1d840af270b37，runtime=fc9f54db5d92079eb1845968abe98e52df92d70d，docs=70281ea0ecb4adb86260ac233cf87fb4dbb23bc9。固定 native 不建立 F.outerFunc；本批外层 F 明确是 CFIR 从真实拥有域派生的关系，不冒称恢复未保存字段。lambda defaults 非法，新正控使用 helper 返回 lambda / lambda 内 nested named function 的 defaults。

## 真实失败、审查与终态

修改前 c94ac244 实际8/8失败；9907输入同SHA，13431字节原XML完整保存于 [red-results.json](../build/cjmp-owned-closure-20261120/red-results.json)。新增 ownership plan 在该轮尚未接入 loader，未用新代码的结果冒充修改前失败。

首次编译3d134045失败于 ClassLikeOwnerContext 参数列表过窄；改为真实 CfirTypeParameterRef/symbol 合同。169f12b5随后编译成功，无行为验收。两轮分别保存 first/second-compile-result.json。

首联合 fbfbcde3 实际80/80通过（serialization40、providers40），SOURCE18因两处 nullable API 测试接缝编译失败未执行。[完整失败窗口](../build/cjmp-owned-closure-20261120/first-joint-results.json)保存同SHA输入/原XML；providers snapshot 类实际34项，不能继续使用旧30项预计数。两个 harness 只对已断言非零正文使用 checkNotNull，正确源程序/位置/表数/类型/目标期望未变。

独立首gate拒绝R1–R3；第二gate消除R1/R3及R2首次身份链，保留R2-H。修订后 [第三gate](../build/cjmp-owned-closure-20261120/owned-closure-third-gate.md)独立有限ACCEPT，未新增本批 production REJECT；旧拒绝报告完整保留。gate范围不替代完整S5/S6/normal-body或统一常量reader。

最后 c1cb0c46 于2026-11-19T19:42:49.895Z至19:46:37.775Z实际DONE/exit0，**99/99**、无失败/错误/跳过：owned12、public6、type13、position10、原SOURCE14+4refs、providers34+6。9907输入前后均 **EF745B2CBD3C9C256AF18EDB14ABF29E792FC9F5D3002DCB61C79547BFCCFFEC**；10份原XML22115字节全部属于真实窗口，gzip解压长度/SHA核验一致。[事前输入](../build/cjmp-owned-closure-20261120/second-joint-inputs.json)与[终态/逐case/原XML](../build/cjmp-owned-closure-20261120/second-joint-results.json)保存。

## 接续与完整余额

下一集中修 VarInfo/allValues/SOURCE 统一 typed constant materializer，按真实成员类型与 FullId classifier、同 context v identity、binary16 bits 及失败发布验证，退休 DD 自有语义 reader。本批 [constant native](../build/cjmp-owned-closure-20261120/constant-native-map.md)/[Kotlin](../build/cjmp-owned-closure-20261120/constant-kotlin-map.md)报告和13项候选测试补丁已准备，尚未实施或执行。固定 native writer不写 VarInfo.value/allValues，SOURCE ConstPattern 不写 values；值池新例明确是 typed schema 产品验证，不冒充官方 SOURCE 生产证据。

统一 encoder 退休 imported/annotation legacy、专用多 parent Position/composite 后置失败、全域 provider/cache/tag 与跨 context 调度、正式 ordinary/common/depends-on/CLI/LL 消费及原 H1/H9/H10/H13/E6/G1、P0/P10/版本矩阵仍需完整验收。原其它前端余额按交付板继续，不将99绿色换算为原70项完成度，目标保持active。
