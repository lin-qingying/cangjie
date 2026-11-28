# 主构造参数字段：构造完成边界（2026-10-06）

这是 `allkinds.cj` 宏测试实际失败后的框架取证、共享字段阶段实现及剩余 construction 设计。
共享字段阶段已通过定向验收，`allkinds` 的 PSI/LightTree 两入口已通过；完整 LL/fragment construction 仍未完成。
完整目标仍为 CJMP 主计划；本项不能用恢复按名/后代搜索来绕过。

## 实际失败链

本节记录修复前的根因；当前两 raw 的这套字段回查代码已经删除。

`cfir/analysis-tests/testData/macro/llt/annotation/globals/allkinds.cj` 自己声明了
`@Annotation class APILevel`，不能因为短名与系统注解相同就把它当内建。
`@APILevel[12] S(@APILevel[12] let c!: Int32 = 9) {}` 的 parser 输入已有真实
PRIMARY_CONSTRUCTOR；两 raw 将它转换为 primary carrier。分类结果是 CustomAnnotation，
slot 为 ANNOTATION，展开只替换注解，不重建构造器。

两 raw 的 addPrimaryConstructorParameterFields 却重新读取原类体的直接 primary 语法节点，
看不到 wrapper 内的构造器，最终没有字段 c，副构造的 c=8 报 UNRESOLVED_REFERENCE。
实际 CFIR constructor 身份与原始语法查找是两个不一致的来源。

## 官方与 Kotlin 证据

以下官方位置均读取固定 v1.1.3：

- MacroExpansion.cpp:345–369：SetPrimaryDecl(outerDeclIdent)、EnableCustomAnno、ParseNodes(scope)，然后替换实际成员。
- TypeChecker.cpp:1604–1636：只遍历 GetMemberDecls() 中实际 PrimaryCtorDecl，首个进入反糖，重复 primary 单独诊断。
- Sema/Desugar/DesugarInTypeCheck.cpp:153–202：最终 param.isMemberParam 决定字段；static 参数不生成字段；outerDecl 为最终 nominal。
- ParseDecl.cpp:1044：primary 身份在具有真实作用域的 parser 中形成，名称不匹配在 parser 报告。

Kotlin raw FIR 的 PSI 路径将实际 primary 的语法参数与 FIR 参数配对；light-tree 路径
通过 PrimaryConstructor/ValueParameter 描述对象保存转换结果及 val/var/source/modifier。
没有转换完再从父类体的任意后代按函数名认领参数的路径。
Kotlin 没有仓颉 token macro；构造完成阶段的额外要求来自上述官方仓颉语义。

## 共享事实与唯一字段构造

- 复用已有 `CfirVariable.isMemberParameter`，由参数转换保存真实成员参数事实；不从 correspondingField 反推。
- `CfirValueParameterImpl.isVar` 恒 false，不得为保存字段 var 而改变参数可变语义。
- 以 typed declaration data 保存字段所需 isVar 与完整声明 status。参数转换当前 DEFAULT status/EMPTY attributes 不足以生成字段。
- name/source/moduleData/type/isTypeImplicit/最终 annotations/containingDeclarationSymbol 已在参数上，不重复保存第二份。
- 唯一共享 materializer 消费最终 nominal 的直接真实 primary 和最终 valueParameters，建立双向身份关联。
- 字段 callableId、dispatchReceiverType、containingClass 来自最终 class/struct；不借原宏输入的名字或 owner。
- 复制最终参数注解，派生 metadata 必须在事务 seal 之前登记。
- 源码 construction facts 与反序列化字段独立；不能给 imported primary 重建字段。

## 所有发布入口必须一起改

| 入口 | 必须完成字段结构的边界 |
|---|---|
| Batch macro | stable splice 与 annotation metadata migration 后，cache key/seal/recordExpandedRawFilesOnce 前 |
| Eager 无宏/历史入口 | Identity construction 完成前，不把无宏路径留在另一套字段算法 |
| LL 初次 raw | LLCfirFileBuilder 的 fileCached 回调返回前 |
| LL 非局部声明重建 | RawCfirNonLocalDeclarationBuilder 的 finder 建树前 |
| PSI/LT raw fixtures及专项字段测试 | 明确 pre 与 completed raw；字段断言必须经过同一 completion，而非接受字段消失 |
| CJO producer | 消费现成完成的成员图，不在序列化时补字段 |

LL 当前没有 compiler macro construction：仅记录 expression surfaces，忽略 declaration/parameter
surfaces；ModuleFileCache、CfirElementFinder、CfirClassDeclaredMemberScope 很快缓存直接成员。
因此不能仅在 frontend service 里加 materializer，也不能等 STATUS/BODY 再补字段。

共享 completion 应放 providers/共享 construction 可依赖层，LL 宿主通过已有
MacroConstructionService 抽象供给真正 classification/splice 服务；LL 不能依赖 compiler/frontend。
Identity 目前只是 successOf，不能把任意带宏文件交给它并宣称完成。
无宏、CustomAnnotation、真实声明宏与参数宏必须在完成后使用同一字段生成入口。
保留 raw 提前发布的旧字段会使后续 splice 留下旧兄弟节点，不是完整方案。

## 同批必须删除/替换的旧路径

- PSI/LT 容器结束时回查原始 primary 参数。
- LT convertTopLevelMacroDeclaration 将同名 FUNC 提升 primary 的 ownerName 分支。
- LT extractClassMembers 把无关键字同名 FUNC 提升 primary 的分支。
- 生成声明 fragment 仅传 package 的不足：应传真实接入 scope/primary 名和 raw owner 身份。
  这些身份不能被解释为继承宿主 parent semantic frame；不补跑官方没有的父后置检查。

## 2026-10-06 共享字段阶段实施与定向验收

唯一实现为providers construction包的`materializePrimaryConstructorParameterFields(file, annotationMetadata)`。
两raw已删除字段发射与primary语法回查，LT两个按ownerName/FUNC提升primary的分支也已删除。
helper只处理FILE直接class/struct，选择首个真实primary，消费最终参数的memberParameterFieldInfo；
字段按参数序append，实际owner产生CallableId/泛型self type，排除STATIC参数，复制最终annotation并登记派生metadata。
在同一writer锁内完成；seal之后拒绝调用。重复调用验证同一primary/参数/类型参数/声明图及关联，不重建或回退旧字段。
Recordable工厂在seal前验证字段已生成；删primary、清空/改变参数列表均不能绕过原有完成状态。

调用点：batch在splice后/cache key前；Identity在seal前；LL初建在fileCached返回前，重建在finder前。
LL仍只处理当前raw图，不宣称已完成声明/参数宏展开。raw fixtures显式选择pre或字段完成图，保留原字段断言。
构造算法版本3。ClassId本仓已限定为顶层类型，不添加Kotlin的isLocal/outerClassId兼容属性。

22113首次仅编译失败（不存在的ClassId属性、constructType宽返回类型），已按现有模型改正。
94807 exit0（7m）：raw22/22（primary字段16、compile-time-visible参数2、nested nominal4），
allkinds PSI/LT两入口2/2；XML13:26:34–37Z、13:28:44/56Z。字段阶段24/24、0跳过，静态gate通过。
同轮独立When逻辑12项通过，另提交，不计入字段阶段验收。广域回归仍需继续。

### 真实 primary 与默认构造器不能混为一谈

本仓沿K2将隐式默认构造器也标isPrimary，但其SourceKind为ImplicitConstructor；
字段反糖对应native PrimaryCtorDecl，统一谓词为`isPrimary && source?.kind != ImplicitConstructor`。
这不是按位置或参数是否为空跳过节点，STATIC也不参与筛选。真实static primary仍生成合法member参数字段；
同时存在的implicit default不接收这些字段。该负例严格检查两种constructor及参数关联。

默认构造器本身的阶段/身份仍未对齐，不能在本批顺手改为primary||!static或直接翻isPrimary：
官方TypeChecker.cpp:2086–2104先primary反糖、CJMP合并，再AddDefaultFunction；HasCtorForTypeDecl:2219仅!STATIC。
Utils.cpp:264–395默认ctor仅class/struct，排COMMON与特定interop，specific依赖已合入的common ctor；
默认ctor是PUBLIC、CONSTRUCTOR+IMPLICIT_ADD+COMPILER_ADD，无PRIMARY_CONSTRUCTOR，append且有空body。
enum没有这种默认init。当前两raw仍提前生成implicit-primary，TYPES的ensureImplicitDefaultConstructorIfNeeded仍可补造；
后续迁移必须连同CfirCjmpParseRulesChecker、modifier/source过滤、Analysis/CJO/stub消费者和CJMP完成视图一起处理。

### 仍待继续

生成primary/成员/参数fragment必须接真实parser scope与CFIR owner，不能继续dummy函数参数包装或仅package上下文；
main materializer不补造缺失member参数事实。真实LL宏服务、MacroExpansionRegistry/surface store生命周期及完整CFIR仍待验证/实现。

## 必须验证

PSI/LT、NORMAL/LAZY_BODIES：普通 primary、内建/自定义/限定名/多层/@!注解、
class/struct let/var/named/default、字段符号 owner 与双向关系、注解 metadata。
声明宏保留/删除/改变 primary、参数宏替换字段参数时，只允许最终参数产生字段。
同名普通函数、仅宏输入中的构造器、非法嵌套 nominal、secondary/static/enum payload 不得提升。
重复 primary 不重复生成字段；LL 首次字段查询、复制文件、失效重建及 CJO 关联需覆盖。
现有 PrimaryConstructorMemberFieldRawCfirTest 与 allkinds 期望保留，不能通过降低这些断言绕过阶段迁移。

## 参数事实基础步：局部验收通过

已新增只读 `CfirMemberParameterFieldInfo`，保存字段 isVar 和不向外暴露的独立状态快照，
`createFieldStatus()` 每次复制。复用已有 isMemberParameter，并以一次发布 API 记录；不重复保存参数名字/类型/注解/owner。
两 raw 的 constructor 转换显式向参数转换传 primary 身份，避免读取尚未绑定的 constructor symbol。
普通/secondary/enum 不获得该描述；真实 LET/VAR 与 CONST 不是同一个事实。
旧字段生产暂保持原阶段，但筛选和字段状态读取改为使用已捕获的描述，不再另用包含CONST的helper判定。
在该基础步中尚未宣称allkinds修复；后续共享字段阶段已关闭其两个batch入口。

两份raw私有cloneDeclarationStatus已抽到cfir-tree共享接口事实复制函数，LT expression builder的6处调用一起迁移。
PSI CjParameter.hasLetOrVar/letOrVarKeyword不再把CONST视为成员声明关键字，source stub版本214使旧布尔缓存失效。
参数描述不通过keyword PSI加载AST；PSI读取已有stub布尔，LightTree读当前语法层的LET/VAR token。

新增tree状态隔离测试、扩展raw字段测试（保留原字段断言），新增真实PSI→stub关键词/owner/FQName往返测试。
组合57478已结束，exit1：tree测试的Source fixture缺source已修；PSI和raw暴露共享parser未消费CONST及非primary LET/VAR。
CONST LET/VAR及非法ordinary/secondary恢复路径也在覆盖内，不能以降低断言绕过parser本身的事实丢失。

### 共享参数前缀修复

两种parser共用CangJieParsing，所有声明参数统一消费modifier与可选LET/VAR。
参数专用modifier集合包含CONST/INOUT，不改const变量声明的前瞻规则；软关键字后随冒号时仍作为名字。
按官方v1.1.3 ParseDecl.cpp:2412/2518/2558/2582记录名字/冒号破损事实，
与已有speculative函数类型回滚结果分开；类型和默认值错误不抑制no-modifier。
非member选首个modifier使用官方TokenKind顺序；member允许CONST和四种访问修饰符，
CONST单独诊断，非法modifier不参与访问冲突；CJMP文件角色门失败时短路general modifier检查。
`public : Int32`是合法软关键字名字，不能再将它作为缺名字样例；缺名字用`const : Int32`。

全token错误使用SDK的pending marker/errorBefore，先完成token范围，再完成MODIFIER_LIST。
PSI修饰符与参数关键词、LightTree修饰符与参数关键词只穿透连续ERROR包装，不搜索注解/类型/表达式/其他声明。
stub modifier mask尾部增加CONST/UNSAFE/INOUT，与hasLetOrVar变更同用尚未提交的214版本。
KotlinFrameworkMapper已静态审查marker、所有者和状态快照；运行验证仍分开记录。

已通过：95914 exit0（4m51s），tree 2/2，XML 2026-10-06T08:04:14Z；
16360 exit0（56s），原字段与扩展raw 12/12，XML 08:07:25Z。
11438 exit0（30s），PSI六类34/34，XML 08:13:19–21Z：新增diagnostics 8组、keyword/stub 3组、
whole-tree 1组、trailing comma 3组、modifier 6组、CJMP function 13组。
总计48/48、0失败/跳过。KotlinParityGatekeeper据静态审查及上述主线程结果通过本批foundation局部验收。
该初次局部验证没有运行完整LL/CFIR；随后LL回归与结果见下节，当时allkinds与最终completion仍未解决。

### 参数stub空容器与完整LL回归

基础步提交为68f9504e5。完整LL61614暴露binary/source参数stub形状不一致（4项）、
mixedDuplicateKind绑定断言（1项）。按Kotlin参数modifier-list协议，无实际modifier时不产生MODIFIER_LIST；
同时删除CJO primary专用空modifier容器分支，所有参数使用一致规则。stub版本升215。
此修复不改全局声明header的既有空容器协议，也不恢复primary-only解析。

组合75419 exit0（9m43s）：完整LL168/168、CompiledStubShapeTest标准库整树1/1、
PSI六类34/34、两raw字段12/12，合计215/215、0跳过。LL XML08:44:32–08:45:06Z。
mixedDuplicateKind本轮通过，未改匹配语义，也未独立证明上轮失败的根因。

### LL接入前新增核定

完整materializer放providers现有construction包；raw-cfir-common已经依赖providers，Identity反向调用raw-common会成环。
字段self type须用tree的`CfirClassLikeSymbol.constructType`保留struct种类，不能用始终返回class类型的providers `defaultType()`。

metadata改造前，LL session级`CfirAnnotationMetadataRegistry`被Identity/Frontend expand整体freeze。
若直接在每个fileCached中调用，第二个文件或失效重建将向已冻结registry写入；并发建文件也没有事务隔离。
因此必须先提供按文件/构建代次的construction事务与已发布metadata生命周期，再接materializer和LL发布。
不能解冻已发布registry，也不能换掉session单例丢失其他文件metadata。

metadata 接口与调用点已实施，具体实现、发布/GC验证见`cjmp-construction-metadata-lifecycle-design-20261006.md`。
本批397项通过（providers55、frontend141、raw22、LL168、CFIR专项11）；LL封闭的仍是当前raw metadata。
后续共享field materializer已实施（见本文前部）；真实LL macro construction、MacroExpansionRegistry逐文件发布与surface store生命周期仍需继续。
