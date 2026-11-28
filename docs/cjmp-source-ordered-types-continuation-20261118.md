# CJMP ordered type 输入接续（2026-11-18）

最新真实窗口5167314c：176/176，0失败/错误/跳过。6845输入SHA EEF0D85AD40A34F5249F3518ED84387C8DB575EBAEA8EFB6A7DF33E962414679 事前/事后一致，17原XML37302字节全在窗口。[完整逐case证据](cjmp-source-helper-20261117/source-ordered-type-self-176-results.json)。原SOURCE14仍14/14；原70前端/P0–P10目标未完成。

本轮已落盘：
- providers新增CfirExportTypePayload，按类型角色冻结有序参数/tuple/实参、重复身份、Generic bounds/explicitBounds、C/closure/variadic、This/isRefEnum、Alias/abbreviation、VArray尺寸、Pointer和RawArray现有角色。
- CfirExportTypeValidation按子类型节点身份核验，而非Cone结构equals；Snapshot统一检查payload、有序子槽、依赖闭包、abbreviation及声明绑定。新增三个回归证明相等primitive替换/重排仍使捕获失效，This标记和classifier单独保留。S0定向39/39。
- Snapshot声明self type携带own TP，Builtin声明保存其真实占位语义类型，GenericParam有自身类型。
- SourceGraphProducer中两个ConeFunctionType第四位置参数错误已改成独立命名isCFunc/isClosureType/hasVariableLenArg。
- Producer声明类型读取capturedDeclarationTypeReference，退休重复builtInDeclarationType；新官方fixture断言GenericCarrier/GenericContract self type实参绑定自己的TP，isThisTy=false。
- bbc64074第一次176/176终态采样晚于声明encoder后续修订，SHA漂移明确保留；5167314c是后续新字节的完整同代176，不拼接历史。

## 完整S4仍未交付

当前typeReference仍丢弃typeElement后使用activeTypeSubstitutor，type()仍读取Cone/symbol.cfir及classifier provider；必须改为完整payload投影、固定common→specific TP身份、绑定和shape key。不允许把本轮输入合同通过当作统一类型encoder完成。

当前Generic payload只诚实冻结normal bounds及explicitBounds；它们不是native最终语义upperBounds，也不是原始constraint节点。必须由resolve共同owner完成assumptions、Generic链暴露和sanity，冻结完整原始constraint分组、位置、implicit来源。
Alias声明self Type、使用槽选择与RHS三者仍须独立。资格是IsExportedDecl或IMPLICIT_USED，不能只看public/linkage。RawArray normal仍class-like，本轮仅冻结原始有序arguments；叶子/dims规范化由resolve发布，捕获器不计算语义。严格capture还需去掉默认fullyExpandedType带来的阶段推进。
独立fixed Kotlin/native报告分别保存在[cjmp-source-helper-20261117/ordered-type-payload-kotlin-authority.md](cjmp-source-helper-20261117/ordered-type-payload-kotlin-authority.md)和[native authority](cjmp-source-helper-20261117/ordered-type-payload-native-authority.md)。其结论是S4进入编码仍REJECT，不冒称新ACCEPT。

下一工作：payload→fixed投影→同一t表编码，退休expressionOrNull和live/source重新查询，完成上述类型语义产品；随后S5统一decoder/materialization和S6 ordinary/common/depends-on/LL正式正文消费。S3其余canonical、S7及原前端所有余额继续开放。

仅修改本记录所指的唯一实现树，保留全部历史修改。更新重启已中断前一轮；最后验证句柄19364/queue5167314c已DONE退出0，未留待恢复的Gradle任务。不联系停止旧会话。固定Kotlin3c7a5041f238102285764d370665f56f587f9dc4、native14276740ffec1262b81b6b5236b1d840af270b37继续Git对象读取；PowerShell7/UTF8/apply_patch/gradlew-queue不变。
