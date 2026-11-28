# CJMP 类型使用图实施记录（2026-11-19）

> 最新410451a5/53162真实48/48，7282输入SHA前后同为68FBA2B874395128179C4A75922967ACB10F177D0CF6F03AC414E1AFFA112010，10原XML均属真实窗口。[catch类型槽最新证据](cjmp-source-helper-20261117/source-catch-type-roles-results-20261119.json)。前一f614538d独立48/48与同SHA8F6DFD…见[继承类型窗口](cjmp-source-helper-20261117/source-inherited-types-fourth-results-20261119.json)。原SOURCE14及扩展SOURCE18、Alias/ABI4、TypeUse16、C extras10均再次通过；仍不关闭完整S4、D1/D3/K01、继承H1–H3、Generic/strict capture、S5/S6或原70前端/P0–P10。

> 最新真实行为为39579531/68828联合111项83通过/28失败，7274输入SHA前后同为88A9FF4A8FFCD12802E4C69DC9BCCAF8AB0866A882A144F32287897639448236，14原XML均在真实窗口。[完整证据](cjmp-source-helper-20261117/type-use-c-ellipsis-joint-first-results-20261119.json)。Tree25/S040全部通过；C extra完成正例两入口已过。随后生产继续修类型dispatch、constructor/enum返回节点、真实默认ctor正文、callee签名与call结果、延迟Unit和发布jar闭包，尚未重新取得行为终态；完整S4/S5/S6继续OPEN。

本记录接续[冻结 encoder](cjmp-source-frozen-encoder-continuation-20261118.md)，不替代[原70项/P0–P10](cjmp-framework-review-and-fix-plan-20260928.md)、[前端子集账](cjmp-frontend-subset-audit-20261117.md)或[SOURCE关闭清单](cjmp-source-chain-delivery-20261116.md)。唯一实现树、历史未提交修改、后端排除与 v10 待办边界均保留。

## 最新已核验失败

6a7fdb7f第二联合实际227项215通过/12失败，7282之前的7278同SHACD5BB…；Source18/C extras10/Tree25/providers154全部通过。12失败为Paren checker生成注册和零属性vector，已修并正式生成。59b38753第三窗口48项46通过/两Alias失败，原XML真窗口；前后SHA漂移如实保留。将完整48条声明shape逐键比较，唯一差为Carrier缺native Object父类。随后S3新增完成继承TypeRef产品并通过f614538d48/48。两手工fat jar已实际构建并核验class及命名空间资源，版本字节为1.1.1：[jar证据](cjmp-source-helper-20261117/source-producer-version-jar-evidence-20261119.json)。

39579531是d03a4068实际全部生产及新增tree/analysis测试编译通过后的独立行为窗口，不和482e6a09合并。原SourceExpression的十个失败主要为FuncBody.retType=0；新Lifecycle源八例由参数Paren未resolved在storeResult阻断。Alias已经越过RHS及ctor顺序，实际下一失败在carrierUse的callee签名ClassThis标记false；call结果的普通Carrier类型应继续保留。ABI已到完整declarationShapes，新增比较器仍将合法genericDecl INVALID无引用槽当成external，随后仅精确修复该槽。C extras负例主诊断种类符合，新增handler内部factoryName的CFIR_前缀比较错误由作者修订；Unit正确一字符源范围保留，由生产定位owner修正。所有正确CJ、原结构/类型期望未降低。

恢复读取原句柄85212，实际 queue a39e14e2 已 DONE/退出1；两 parser 的 orderedAliasRoles 各失败一项。新正式 CJO 的版本字段已到达1.1.3，当前具体失败为 Alias RHS 期望 Type(28)、实际 Tuple(18)，位于新套件 nominal/assertAliasRoles。D/E 数量检查已经越过，不能据此称整个结构相等。[完整逐case及原XML](cjmp-source-helper-20261117/source-alias-version-recovered-results-20261119.json)的两XML均在实际队列窗口；旧 input manifest 早于后续修订，unchanged=false，如实保留，不能作为当前字节无漂移证明。

原184/184及29/29属于各自6849输入的历史窗口。11月19日新增来源、原型、类型替换、版本发布及新测试，不使用这些旧通过证明新字节。此前30540ffe的85项真实81通过/4新fixture失败见[type-origin首轮证据](cjmp-source-helper-20261117/type-reference-origin-first-results-20261119.json)，完整S4及目标没有关闭。

## 当前生产候选

- TypeRef 与 QualifierPart 的 SOURCE/SEMANTIC/COMPILER_ADDED 已进入生成模型、正式生成文件、raw PSI/LT、正常成功/错误解析和复制协议。ParenthesizedTypeRef 保留外层与独立 inner。公共原型 helper 取代 completion 的两份私有 helper；显式 type argument 替换保留原 ref，推断创建发布 SEMANTIC。数值类型转换发布 SOURCE，注解名称解析辅助 ref/qualifier 发布 SEMANTIC，lambda omitted 事实由两个 raw owner 完整发布。
- TypeExpansionUtils 删除依据 nominal typeArguments 为空的错误早退，参数替换器访问 Tuple/Function/Pointer 的完整 RHS 子槽。新 Alias fixture 的 TYPEALIAS_UNUSED_TYPE_PARAMETERS 及 tuple TYPE_MISMATCH 已在后续实际前端窗口消失；源和正确结构期望未改。
- CjoSchemaProfile 发布固定 compilerReleaseVersion，producer 写 package.version。固定 native ASTWriter.cpp:726 写 CANGJIE_VERSION，与格式版本及语言选项不同；目前1.1.3候选已越过版本断言，profile全矩阵仍待复验。
- providers 新增 CfirExportTypeUseGraph，在 linkage/IMPLICIT_USED 收敛后，分别冻结每个原类型节点、声明类型、Alias RHS、显式 FuncBody.retType 与 inheritedTypes。有序子选择与原语义 Ty 分开；当前节点COMPILER_ADD停止，Alias资格只取 isExported/IMPLICIT_USED，EXTERNAL不是资格。TypeAliasDecl.self保持自身Type。表达式语义类型、instTys与matchedParentTy仍走其独立语义入口。
- serialization 消费上述选择后应用固定TP投影，再进入同一t池。类型节点的捕获域与当前owner投影域分开；不在encoder构造Cone、重解析Alias或查询provider。没有原ret节点时FuncBody.retType保持0，声明签名返回独立保存；没有删除已选择正文或伪造body=0。

31439/482e6a09已DONE/退出1，实际42项40通过/两Alias失败，6855输入SHA一致，4原XML真实窗口。[首轮证据](cjmp-source-helper-20261117/type-use-graph-first-results-20261119.json)。这轮证明Alias self/RHS越过；下一为constructor列表顺序，不能据此宣称后面全部类型槽验收。后续111窗口单独保留。

## 后续生产修订与当前编译

最新S3继承TypeRef产品已经由c7a559e7正常编译，f614538d实际48/48。随后ExceptTypePattern增加真实TypeRef尾槽：语义type前缀继续直接编码，原catch Type节点按同代TypeUse选择；Graph防御复制、capture、TypeRef context入口和encoder均接入。8de1f653真实生产与测试编译退出0，410451a5实际48/48。新专用catch Alias输入仍在准备，不把既有48当其验证。

[继承类型gate](cjmp-source-helper-20261117/source-inherited-types-production-gate-20261119.md)仍REJECT H1 JavaMirror/JObject、H2 Alias/class-parent分类、H3生成binding的发布边界；普通Object正例通过不关闭这些拒绝。[晚constructor生命周期考证](cjmp-source-helper-20261117/cjmp-late-constructor-lifecycle-authority-20261119.md)要求resolve-owned正式preparation，在最终family/索引发布前完成merge/interop/native ctor资格、真实TYPE/STATUS/归属，退休TYPES与matching的两旧fallback工厂；D1/D3仍待实施。

[Generic/strict capture框架map](cjmp-source-helper-20261117/generic-constraint-capture-framework-map-20261119.md)已按固定源码考证原/派生constraint分组、subject/RHS绑定、final semantic bounds、源码/loaded CJO角色及所有生命周期。T6 Generic、capture默认fullyExpandedType/provider物化尚未退休，完整上界/原组与decoder生产消费继续OPEN。当前没有运行中的本轮Gradle；后续必须冻结新候选再验证，不能用旧48/227代替。

- [TypeUse复审](cjmp-source-helper-20261117/type-use-graph-recheck-20261119.md)限定关闭T1/T3/T4/T5原拒绝点；T2构造器与T6仍开放。已补Specific/TYPES对全部raw Type角色的明确dispatcher，不能只转换children而保留未resolved外层；constructor与payload enum在语义创建owner发布COMPILER_ADDED返回ref，enum使用独立冻结返回入口。
- 默认constructor新增共享tree builder，两个raw及宏最终缺失constructor路径共同消费。实际空Block参与正常TYPES/STATUS/BODY，不制造空CFG；Synthetic.ImplicitConstructor直接发布COMPILER_ADD/IMPLICIT_ADD，SOURCE PRIMARY_CONSTRUCTOR不再从CFIR isPrimary无条件投射。宏finalizer保留同一节点或在最后显式init被删除后新建；COMMON/specific和已有interop排除条件继续核验，不能因普通Carrier通过关闭全部资格。
- [This角色gate](cjmp-source-helper-20261117/source-call-instantiation-role-gate-20261119.md)确认resultType被写入callee签名。Candidate现分别发布DynamicBindingThisType签名和GetCallTy结果；applied reference中substitutedReturnType为签名、callSiteType为表达式类型。工厂、ResolveUtils、owned-copy、PCLA、payload、capture/validate两角色全部接线；SOURCE不重算候选。
- C ellipsis有独立fixed数量/有序extras产品，Candidate、CType约束、completion expected map、正常/错误ArgList、source遍历、owned-copy/状态恢复与SOURCE DECLARED已接入。direct C的Unit义务保留到Mode.Normal最终actual type检查，候选诊断、错误引用和完整实参列表同时发布。Unit定位按固定调用诊断begin锚首字符，直接literal和deferredUnit正确期望均不改。
- schemaProfile绑定producer版本的旧候选职责错误已退役。独立compiler/compiler.version模块按固定Kotlin资源发布形状生成真实Gradle构建版本，当前资源实际为1.1.1；Package.version独立于schema/language/cjoVersion。native归档验SDK1.1.3，JVM产物验真实build VERSION。两手工fat jar列表已包含新class/resource模块，尚需实际jar闭包检查。[发布/ordering/C01审查](cjmp-source-helper-20261117/source-order-version-production-gate-20261119.md)保留原拒绝与后续边界。

59098941实际generateTree执行，随后tree源码copy分派缺抽象穷尽分支导致compile失败；已补明确unsupported concrete拒绝，未恢复降普通impl。8086c1cc的checkers诊断生成执行，resolve仅两个import失败随后修复。d03a4068实际全部生产与新增tree/analysis compileTestKotlin退出0。cf26317b后续新工厂status builder名称编译失败，已改用真实CfirDeclarationStatusImpl。当前句柄33285/queue0f26d7eb在编译最新signature/constructor/Unit候选及Analysis API诊断生成，未终态；不能用旧编译或旧XML代替。

## 仍需修复与验收

[来源复审](cjmp-source-helper-20261117/type-reference-origin-recheck-20261119.md)限定确认G1/G4/G5/G6原缺陷静态修复；G2仍REJECT：ResolvedImplicit非法source迁移不得no-op，普通resolved分派应保持concrete合同。G3正常Paren/Qualifier链已修，G7仍REJECT缺损括号内层：不能将必需内层缺失写成SEMANTIC implicit，两parser必须保留实际损坏节点/来源。未取得G1–G7全组行为ACCEPT。

[C ellipsis语义](cjmp-source-helper-20261117/c-variadic-source-authority-20261119.md)和[固定Kotlin框架映射](cjmp-source-helper-20261117/c-ellipsis-kotlin-framework-map-20261119.md)确认下一阻塞为独立extras的完整生命周期：fixed映射、CType约束、正常completion、成功ArgList及owned-copy/状态恢复/SOURCE均须接入；C foreign调用的SOURCE kind为DECLARED。functionAbi正确源仍保留，WRONG_NUMBER_OF_ARGUMENTS不能靠放宽arity一处解决。后端promotion不在实施范围。

TypeUse完整独立审查/生命周期、Generic最终语义upperBounds及原始constraint分组/位置/来源、严格capture退休lazy展开、S3其余canonical、S5统一decoder/materialization、S6 ordinary/common/depends-on/LL正式正文消费均OPEN。TypeKind.Type decoder、recursive fallback、旧annotation reader和库函数body缺失未据本候选关闭。原前端其余余额、49版本/P0/P10与v10继续，H6后端余额保留分母不实施。
