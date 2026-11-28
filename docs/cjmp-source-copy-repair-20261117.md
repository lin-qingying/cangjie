# CJMP SOURCE 复制层接续（2026-11-17）

> 后续S1已接construction/provider/TYPES/STATUS/BODY/LL，原SOURCE4/14，两parser十四生命周期14/14；最新实际代码、同代证据和R5完整REJECT余额见 [生命周期接续](cjmp-source-helper-lifecycle-20261117.md)。下面“R6/S1尚未接线”是本记录旧时点，不覆盖后续生产接线，也不等于完整S1/R5已关闭。

唯一实现树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。本次从交接九文件候选继续修改，没有重新应用旧 patch、回退历史修改或联系停止的会话。原70项、P0–P10、H6排除余额和v10待办保持原合同；完整S1–S7、R5/R6仍OPEN，不提高原计划整项分值。

## 实际修订

- `OwnedGraphCopyPrinter` 使用生成模型真实的 `ClassOrElementRef` 名称合同；不向 `TypeRef` 增加虚构API。构造参数与 `ImplementationPrinter` 同用 `isParameter || !isFinal`，resolveState从phase独立初始化。同名intermediate builder出版接口，没有copy DSL；Main显式提供这些名称，复制器按实际concrete构造函数分配壳。正式generateTree成功，nullable ThisReference.boundSymbol由模型生成安全调用，没有手改gen。
- 手写This/InaccessibleReceiver、SmartCast、ConstantValueExpression保持concrete kind与所有拥有字段。SmartCast全部Cone槽走显式remapper。独立常量值图先登记合法壳再回填；类型、Object字段符号及Enum constructor使用同一协议，保留共享/cycle；VArray壳构造即满足非空长度不变量，发布后容器只读。
- metadata按完整注册key分类。default helper/Evaluated.result拥有；correspondingField/ValueParameter只借用，单端复制不扩域、双端显式roots才重绑。未知key仍由显式扩展协议处理。candidate派生态在收集时拒绝，不构造base代替resolve产品。
- 符号重绑核验kind、fresh owned destination和唯一owner；内部target统一延后绑定并核验新owner。两个immutable empty singleton明确共享，其余节点保持actual concrete。窄Simple/VArray类型槽有显式约束。
- 独立复审随后指出五处R4漏洞，已修复：destination不得复用owned输入旧symbol/node；外部函数owner重绑必须显式提供Return target；常量字段重映射不得碰撞或改变槽数；remapDeclaration强转前核验concrete；外部loop接受显式目的循环并核验kind/isDoWhile。对应五个回归已新增。
- 复制结果记录analysisOwners与resolvedOwnersRequiringAnalysisRebuild，**仅表示结构复制与待构建产品**。完整resolve重建/消费门尚未实现，单根API不能据此冒称分析就绪；phase/正文类型没有回退、旧CFG没有共享。

## 固定源码与限定复审

本次新建KotlinFrameworkMapper、CangjieSemanticsAuthority、CangjieTestSemanticsWriter、KotlinParityGatekeeper；主线程修改生产与测试。参考均使用固定对象git show：Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`，官方仓颉v113 `14276740ffec1262b81b6b5236b1d840af270b37`。没有把官方当前checkout当v113。

Kotlin `generators/tree-generator-common/.../TypeRef.kt` 的名称属于Importable/ClassOrElementRef；`DeepCopyIrTreeWithSymbolsPrinter.copyValue`按模型种类及typed symbol角色分派。通用复制参照在IR tree，未声称存在可照搬的FIR copier。FIR candidate/完成writer和CFG仍由resolve拥有。官方Clone.cpp:1552–1695的CloneWithRearrange只重排已在复制域内的引用；ComputeAnnotations.h:20–46及ComputeAnnotations.cpp的值缓存、执行后字段读取支持独立值图与对象共享，不支持按构造实参推断存储。

Gatekeeper第一次限定认可R1–R3，拒绝上述五处R4漏洞；修订后再次只读审查明确“R1–R4本次补丁源码门可接受”，五处无剩余阻断。此结论不包括R5/R6、S1生产接线或完整S2验收。行为验证仍按实际轮次分别记录。

## 已结束的验证窗口

[verification.json](cjmp-source-copy-20261117/verification.json)保存六次真实queue终态、初始六文件SHA、564文件后采样、SOURCE联合窗口前后6815文件聚合SHA，以及七份原XML的gzip/base64、原长度/SHA、解压核验和classname+testcase-key结果。它对应**五处R4追加修订之前**的候选，不能代表追加修订后的行为验收。

- queue `8594a660`：正式generateTree退出0。
- `1ff21a38`、`ee95fa05`：生产编译失败，分别暴露resolveState参数与不存在的QualifiedAccess copy DSL；没有行为通过。
- `a443b857`：生产及依赖闭包编译通过，原六测试三处对只读statements使用+=导致compileTestKotlin失败；改用正式replaceStatements，保留原断言。
- `a31edfc3`：复制合同14/14，零失败/错误/跳过；原六＋新增八实际执行。其564文件采样发生在终态之后，不冒称事前冻结。
- `5d7e859d`：S0 36/36，原SOURCE2/14，联合50项38通过/12失败，零errors/skipped，退出1。6815个选定输入事前/终态聚合SHA均为`94E23CBB30DB2949BEE127F1FDD2D7568E7AD2772ECDB65D5040C46BD5CF409D`。7份原XML共62447字节逐份解压核验；其中tree14属于独立前轮，不与本联合50项合并计数。

原SOURCE十二失败仍是walk/calculate/evaluate/recover及value正文不可表示，helper独立声明期望5/实际4。正确仓颉fixture、正文选择和严格表/边期望未改，没有body=0降级。R4追加修订后的19测试复验另行记录，不能拿上述14/14替代。

## R4最终修订的实际联合终态

`d1d9cdfe`：追加五回归的复制测试19/19，退出0。随后 `a7aed1e9` 在最终代码上重新执行三个任务：复制19/19、S0 36/36、原SOURCE2/14，共69项57通过/12失败，零errors/skipped，退出1。最终结果没有与前轮14或50项拼接；[r4-final-verification.json](cjmp-source-copy-20261117/r4-final-verification.json)保存两queue、6815输入事前/终态聚合SHA、七原XML共63248字节、无损解压SHA及逐case台账。全部XML timestamp落在本联合queue真实窗口内；输入前后SHA均为`DAC9CCDBCC35AAF5B85F3024F4AE48422066E7AB3A5E4D95C1C0C1A5B0C566C2`。

R1–R4限定源码门通过，并有上述行为证据；完整S2仍不关闭。R5须正式完成分析重建与消费门，不能以当前owner列表或非空CFG冒充就绪；R6/S1的绑定前helper准备与正常完整生命周期仍未接线。

## 下一生产工作入口（已读实际代码）

S1准备发生在 `MacroConstructionService.successOf/degradedOf` 的 `pre.sealAnnotationMetadata(files)`之前，两parser/Identity/真实macro共用；provider注册时metadata已经发布，不能在那里生成helper。已有 `CfirAnnotationMetadataBuilder.recordDerivedSlot`要求源slot与新owner/index/annotation真实匹配，新图须先在开放事务中登记。

`CfirProviderImpl.recordDeclaration`显式沿参数metadata登记helper文件及enclosing class，isTopLevel=false；ownerFunc不能塞进只接受class-like的callableOwnerMap。TYPES的transformValueParameter进入helper普通header，STATUS要覆盖transformValueParameterStatusWithoutPhaseGuard真实专用入口。BODY须切回helper的真实词法outer再走完整普通named-function事件；当前withNamedFunction在参数container下会storeFunction，没有现成metadata-helper outer切换API，不能直接在原参数scope内解析helper。

固定TypeChecker.cpp:2397–2444的CollectGenericParam仅复制owner自有函数泛型，按序T$0/P$1及其bounds，外层泛型保持lexical可见。helper根owned图中RefType/RefExpr原名→新名替换没有shadow/local/target过滤，不改局部声明名或MemberAccess/QualifiedType.field。MakeDefaultParamFunction:2462–2519生成paramName.ordinal、全部前序fresh参数、独立assignment及显式Return，ownerFunc=owner、outerDecl=owner.outerDecl；构造器STATIC，MUT/frozen按原生事实。GetSingleParamFunc:2523–2568的IMPORTED/HAS_INITIAL与operator/OPEN/ABSTRACT/DEFAULT门保留。

LL的 `CfirLazyBodiesCalculator`仍持有真实lazy default物化入口；S1必须接续其正常原始身份重建与metadata事务，不能用私有重parse或把Lazy拒绝当完整LL消费。完成S1/R5/R6后继续canonical SOURCE图、统一encoder/decoder、正式消费与原阶段验收。
