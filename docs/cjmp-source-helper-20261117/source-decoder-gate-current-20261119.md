# KotlinParityGatekeeper：当前 SOURCE decoder 限定审查（2026-11-19）

**REJECT；正文 S5/S6 保持 OPEN。** 33 role 已有分派，资源拥有入口、同包缓存和完成发布已接通；不能据此接受完整 owner/binder/FuncBody/位置/值闭包。以下是当前源码可修阻断，不是测试执行结果。

审查树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`；候选包含未提交源码，文末记录实际文件 SHA256。仅写本报告；未修改生产/测试，未运行 Gradle/cjc，未联系或停止旧会话，未扩展后端。独立 fresh SOURCE18 第二窗口正在执行（用户给定状态，本轮未获取结果）；旧 73 窗口先于正文候选，不能关闭当前正文 S5/S6。

基准为 [native authority](s5-astloader-source-native-authority-20261119.md) 和 [Kotlin mapper](s5-kotlin-framework-mapper-20261119.md)。必要争议点已通过 `git show` 复核固定对象：native `14276740ffec1262b81b6b5236b1d840af270b37`、Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`，读取自 `D:/code/intellij/cangjie/external/`，未拿工作文件或本审查树的 external 路径代替固定对象。

## 坐标简称

| 简称 | 当前文件 |
| --- | --- |
| D | [CfirCanonicalSourceDecoder.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirCanonicalSourceDecoder.kt) |
| L | [CfirSourceGraphLoader.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirSourceGraphLoader.kt) |
| V | [CfirSourceConstantDecoder.kt](../../build/cjmp-owned-closure-20261120/documentation-link-repair-map.md#missing-source-constant-decoder)（原链接路径：`../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirSourceConstantDecoder.kt`；历史源码原字节缺失） |
| P | [CfirImportedSourceGraphProvider.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirImportedSourceGraphProvider.kt) |
| DD | [CfirDeclDeserializer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt) |
| A | [CjoSourceGraphConsumptionAssertions.kt](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CjoSourceGraphConsumptionAssertions.kt) |

仅为核对上述入口的实际合同，追读其 context/provider/graph/index/position/value 接缝；不对这些模块作全面审查。固定 native 坐标简称 E=`ExprLoader.cpp`、R=`ReferenceLoader.cpp`、W=`ExprWriter.cpp`、N=`ASTLoader.cpp`，均在 `src/Modules/ASTSerialization/`。


## 第二轮：原 Gate10 finding 状态

复用 [原 Gate10 审查](s5-source-decoder-kotlin-parity-gatekeeper-20261119.md) 的固定映射，复读实际最新接缝。第二窗口仍在执行；临时 consumer 已实际用 I 接口作参数/返回类型（A60–64、88–109），消除真实 UnusedImport，原 SOURCE 表/正确 fixture 期望保持原合同。本次未再写生产/测试，也未重跑或中止窗口。

| 原门 | 当前判定与坐标 |
| --- | --- |
| F1 | OPEN：L29–35、DD191–218/1757；见 R1，另保留 R3/R4 的实际 F 域缺口。 |
| F2 | OPEN：L24–37、D52–53/96–120、DD887–888；独立 top-level/nominal initializer root 未进入完整计划，见 R7。 |
| F3 | ACCEPT（登记/复用源码窄门）：DD2382–2388 查询已有 binding 且校验 concrete kind；DD2423–2428 绑定后登记 context.declarations。撤销旧“binding 未登记、必然新造两份”阻断。binding-first 的 owner reservation 仍归 F1；A243/542–582 尚不直接比较 normal pattern.bindingVariable 与 graph/ref 的 symbol，行为验收未关闭。 |
| F4 | OPEN：V64→ToSymbolUtils.kt:60–66→AbstractCfirDeserializedSymbolProvider.kt:92–111/276–284；context 完成门不能撤回 provider/tag 的 staging 发布，见 R9。 |
| F5 | OPEN：V60–69、CfirTypeDeserializer.kt:154–166、Abstract…:94；composite classifier 仍丢失 selected FullId/snapshot 选择，见 R10。 |
| F6 | OPEN：L64–68、CjoSourcePositionResolver.kt:25、CjoManager.kt:64–65；位置仍取首个包快照，见 R6。 |
| F7 | OPEN：D31–36/130–156、P10、Abstract…:466–474；命名 F header 未公开，见 R7/R4。 |
| F8 | OPEN：D197–201、CfirSourceGraph.kt:137；四值 StringKind 压成 bool，见 R11。 |
| F9 | OPEN：D297–302/329–353、CfirSourceGraph.kt:89–100；pattern selector ctxExpr 未恢复，见 R5。 |
| F10 | ACCEPT（断言源码窄门）：A556–581 已按 semantic-prefix/捕获尾槽拆分，并核对 pattern begin/end、values 数量；不预记第二窗口通过，也不据数量检查关闭值内容/共享回边，见 R8。 |

## 33 role 核对

本表只确认当前分派和槽角色；不授予完整恢复 ACCEPT。e/d/t 是不同表域；Ptn/F 是内联 Pattern/FuncBody。分母是 30 普通表达式 + 3 辅助记录，Resume 不在固定 writer 的这 33 项中。

| # / role | 当前坐标 | 槽角色核对 |
| --- | --- | --- |
| 1 WildcardExpr | D167 | 无拥有槽 |
| 2 PrimitiveTypeExpr | D168、61 | 无拥有槽，记录语义 t |
| 3 ReturnExpr | D169–172 | value=e；FuncBody 域阻断见 R3 |
| 4 JumpExpr | D174–177 | map 槽解释为 borrowed loop e |
| 5 MemberAccess | D180–182、316–326 | base=e；target/instTys/parentTy 分槽 |
| 6 RefExpr | D179、316–326 | target=borrowed d FullId；R1 |
| 7 CallExpr | D184–187 | base=e，后续为 FuncArg 辅助 e |
| 8 UnaryExpr | D189–191 | operand=e，op 独立 |
| 9 IncOrDecExpr | D193–195 | operand=e；未从 33 分母删除 |
| 10 LitConstExpr | D197–201 | kind/text/string 标量；不是 Pattern.values |
| 11 BinaryExpr | D203–205 | left/right 有序 e |
| 12 SubscriptExpr | D207–209 | base/index… 有序 e，tupleAccess 独立 |
| 13 AssignExpr | D211–214 | left/right 有序 e，op/compound 独立 |
| 14 ArrayExpr | D216–221 | FuncArg 辅助 e；initFunc=borrowed d |
| 15 PointerExpr | D223–225 | 0/1 FuncArg 辅助 e |
| 16 TypeConvExpr | D227 | operand=e；结果 t 独立 |
| 17 ThrowExpr | D228 | value=e |
| 18 PerformExpr | D229 | value=e |
| 19 SpawnExpr | D230–234、124–127 | 0/1 arg=e；future=当前 owned d |
| 20 ArrayLit | D216–220 | element…=e；initFunc 独立 |
| 21 TupleLit | D236 | element…=有序 e |
| 22 IfExpr | D237–239 | cond/then/else，固定 0 else 保留 |
| 23 TryExpr | D241–249、124–127 | try/catch/finally=e；Ptn 和 owned resource d 分开 |
| 24 WhileExpr | D251–254 | cond/body 顺序正确 |
| 25 DoWhileExpr | D251–254 | body/cond 与 While 区分 |
| 26 LambdaExpr | D256–261 | 独立 F/匿名函数/参数；binder 见 R1、R4 |
| 27 Block | D263–270 | isExpr 分 d/e；末位 UNSAFE 标量保留 |
| 28 MatchExpr | D272–277 | selector + MatchCase 或仅 MatchCaseOther |
| 29 LetPatternDestructor | D279–283 | initializer=e，Ptn 内联 |
| 30 ForInExpr | D285–295 | in/guard/body 固定 0 槽和 kind 保留 |
| 31 FuncArg（辅助） | D305–310 | InvalidExpr+FuncArgInfo；default 0 槽与普通值区分 |
| 32 MatchCase（辅助） | D297–302 | guard/body 与 Ptn 分开；selector 回边见 R5 |
| 33 MatchCaseOther（辅助） | D304 | condition/body=e |

辅助记录的自身 t 统一由 D61 读取；不能把有 role 分派等同于 native 中三个辅助记录都是普通 AST::Expr。Block/VarPattern 的 mixed 表域正确，D346 将 VarPattern.exprs 解释为 d。Except/Command 的自身 t 与尾部捕获 t 在 D341–352 已分开；原 wire 只有语义 t，不据 `WrapType` 虚构捕获 Type 的原语法位置。

## 当前可修阻断

| 门 / 优先级 | 当前坐标、具体后果 | 固定依据与直接修复方向 |
| --- | --- | --- |
| R1 / P1：local owner 与 borrow binder | L29–35 无条件预扫每个带非零 body 的 FuncDecl，再调用 canonical。DD197–199 在找不到 parent 时按普通声明读取；局部函数尚未由 Block 建 owner，DD1757 因 `sourceDeclarationOwner==null` 写成非 local。随后 L46、DD210 的 cache 命中不会修复身份。另 L53–58 将 CURRENT borrowed target 转为 `declaration(index, requesterOwner)`，未知局部/匿名函数参数会被请求者认领；真实拥有入口后来只复用错误节点。CjoFullIdResolver.kt:279–298 只索引 nominal 成员、Generic、命名函数 params，没有 Block/Try/Spawn/匿名 F 的拥有域。 | native N343–386 只取 top-level roots，再沿 E361–380 的 mixed owned edges；Kotlin IrSymbolDeserializer.kt:30–79 统一 declare/reference binder，IrDeclarationDeserializer.kt:540–575 进入实际 parent/scope。先索引真实拥有闭包、reserve d/F/参数身份，再填；局部函数不得通过全表扫描顶层物化。borrowed FullId 只查实际 owner 的 canonical binder，不能传引用者作为 owner。 |
| R3 / P1：FuncBody 域与 control borrow 混入声明域 | D107/113/118 把 initializer/default 的 owner 改成 variable/parameter；D171 要求 Return 的 owner 是 CfirFunction。选中正文内嵌在局部 initializer 的 Return 即使属于外层实际 FuncBody，也会在此报错。D58/177/79–88 对 map/Jump 回边调用与 owning 相同的 reserve，并强制 caller owner 相同；未拥有的 borrowed e 能被登记、排队、填成新拥有内容。A273、388、394、513 跟随同一 owner 假设。 | native E488–509 从选中的实际 FuncBody.body 回填 Return，只跳过嵌套 FUNC_BODY；R586–589、641–645 按已登记 e 取 map/loop。Kotlin IrBodyDeserializer.kt:793–822、895–908 先建 loop，再引用原 loop。将声明 context、实际 F 身份、owned e 域分别传递；Return 绑定 F，map/loop 在拥有闭包 reserve 后只 lookup。当前 graph.finish（CfirSourceGraph.kt:353–357）已检查 loop concrete kind，但不证明其从合法拥有闭包到达。 |
| R4 / P1：匿名 F/header helper 不完整 | DD225–249 新建匿名函数且未使用 `owner` 建外层关系；无 F identity registry。匿名参数走 DD216–218 的 `CfirParameterDefaultValueMetadata(..., null)`，不同于命名函数 DD2638–2655 先读取对齐 helper。D135–145 后补 desugars 列表不能补回已缓存参数的 defaultValueMetadata，也没有明确区分 helper 的外层 owner 与 ownerFunc。 | native N763–791、794–834 分 params/desugars/defaultVal、F.funcDecl 与 helper.ownerFunc；Kotlin IrDeclarationDeserializer.kt:540–575 同一声明 binder 恢复 function/params/body。以匿名 F 的实际来源身份登记函数/参数；对齐 helper 在参数 header 完成前接好，保留 helper outer 与 ownerFunc 两种关系。不要把 lambda 当 native outerDecl；本门不要求虚构 native 通用 local-var outerDecl setter。 |
| R5 / P1：Match selector 的非拥有语义边缺失 | D300–302 建 MatchCase.patterns，但未将 selector 身份补入 pattern 产品。CfirSourceGraph.kt:89–100 的 Pattern 无 ctxExpr/selector 引用字段，203–207 的 MatchCase 也没有该补全产品。A492–495 仅递归 pattern。结构上能从父 Match 找 selector，不等于已发布 pattern 的实际 borrowed ctxExpr。 | native R732–742 明确 `mc.patterns[i]->ctxExpr = &selector`。沿 owning Match→case 闭包补同一 selector 身份并校验；不可复制 selector，也不可由消费时当前位置猜它。 |
| R6 / P1：外包位置绕过同代依赖选择 | L64–68→CfirDeserializationContext.kt:178–179→CjoSourcePositionResolver.kt:21–26 按包名调用 manager.loadPackageSnapshot；CjoManager.kt:64–65 实际取 `firstOrNull()`。FullId/types 却通过 context.importedSnapshot（228–232）使用真实 dependencySnapshotResolver。因此同包多 snapshot 已选第二份时，symbol/type 与 begin/end 的文件表可能来自不同输入。A628 将所有 `file==0` 当缺失，A632 又只接受 pkgId=0；外包合法零基 file=0 被错误排除。 | native N476–500：Position 当前 pkgId=0/file一基；外包 pkgId一基/file零基，与 FullId=-2 分开。Kotlin BasicIrModuleDeserializer.kt:55–73 保留同一 reader。让位置读取复用 context 的已选 dependency snapshot/表域；断言按 Position.pkgId 分域解码，加入外包第零文件与同包多快照选择检查。 |
| R7 / P1：真实 Header/Source 能力未发布完整 | D31–36/143–150 将命名函数原 F header 放在 `CfirDeserializedSourceGraph.functionHeaders`，但正常 provider 返回的仅是 `.graph`（AbstractCfirDeserializedSymbolProvider.kt:466–474），P10 无 header/availability 查询。graph 的 functionBodies 仅保存 e identity；只有 lambda payload 暴露 importedHeader。body=0 的函数在 L33、D103 被略过，保存在 F 中的 header 不能由这个能力查询。L 也仅从 FuncDecl 建 root，独立顶层 initializer/annotation 不在此 root 选择入口内。 | native E488–513 的 header 与 body 选择分开；默认 isConst=true、N806 只传 isInline，不能据 Kotlin inline policy 排除普通选中正文。mapper §5.2/§8 要求 availability、selection、bodyComplete 和同 binder 的产品。正式 provider 应发布原 F header 与可用/未请求/已完成状态；明确 SOURCE root 选择闭包。保持 Library CFIR body=null 与 canonical SOURCE 产品的能力边界，不能凭图查询给 DFA/CFG 就绪授信。 |
| R8 / P2：值能力分 reader，Float16 被当整数 | V16、57–71 对同一 decoder 的 allValues 壳先登记后填，保留局部共享/cycle；DD2074–2083 的 VarInfo 常量仍走另一个 reader/materializer，DD2137–2162 再造 ObjectValue，不共享 V 的 v identity。V26–36 未处理 Float16：schema 中 Int16Value 同时承载 Float16 位值，当前 FLOAT16+Int16Value 落入 INT/BigInteger 分支。A581 仅核对 constantValues 数量，未核对 union 类型、值内容和 allValues 共享/cycle。 | 固定 ModuleFormat.fbs:85–120、102、527–536 确认 union 和 v 表域。但固定 W720–737 不写 ConstPattern.values，E559–568/R745–787 也不恢复这些值，不能声称固定 native SOURCE33 已证明当前新增值缓存往返。候选值能力应共用 context-owned v reservation/fill 与真实字段 type，并按保存类型还原 Float16 位值；独立提供实际含 values 的消费证据。 |
| R9 / P1（F4）：provider/tag 提前发布 staging | V64 在 L22–37 的 SOURCE 根事务内调用公开 toClassLikeSymbol；ToSymbolUtils.kt:64–65 进入 provider 并绑定 tag。AbstractCfirDeserializedSymbolProvider.kt:282 可重入返回 staging 声明，92–111 随即写 classCache/classSymbolsCache；CfirDeserializationContext.kt:125–143 的失败清理未覆盖这些缓存/tag。后置 e/必需槽失败后，context 未提交而 provider 仍持有本次 symbol；重试存在双身份路径。 | 复用原 F4 的单包失败路径和 Kotlin ValueWithPostCompute 完成观察合同。事务内通过 canonical identity binder 取得实际 owner；公开 provider/tag 只发布完成对象，或纳入同一发布事务。验收同时检查 context/provider/tag，不能仅查 sourceGraph==null。 |
| R10 / P1（F5）：composite classifier 重选首个 parent | V60–64 解码 wire.type 后按 ClassId 查 symbol；CfirTypeDeserializer.kt:159–162 将 FullId 仅转换为 ClassId tag；ToSymbolUtils.kt:64→Abstract…:94 的 firstOrNull 绕过真实 dependency/refinement snapshot 选择。V66–69 在错误 classifier 的成员中按 field.name single 匹配，不能修复 owner；同名字段可静默错绑。 | 复用原 F5 的两个同包同 classifier/field-name parent 合同。从实际 CompositeTyInfo.declPtr 和已选 snapshot 的 canonical binder 取 classifier，再绑定真实字段 symbol；交换候选顺序应仍返回同一所选 provenance。不增加名字替身或首项兜底。 |
| R11 / P2（F8）：StringKind 丢失保存信息 | D197–201 只存 strKind==JString；CfirSourceGraph.kt:137 的 Literal 仅有 isJavaString。Normal/MultiLine/MultiLineRaw 均压成 false，无法在正式产品区分；A424–440 的 literal 检查也没有四值字段断言。 | 复用固定 native ModuleFormat.fbs:398–403、W233–243、E141–151 的 Normal/JString/MultiLine/MultiLineRaw 合同；公开 literal 保留完整枚举并严格映射，未知值失败。canonical 去 Paren/desugar 不授权删除这个已保存字段。 |

## 已接通边界与仍缺的验收

- **捕获 pattern 的拆分断言已正确：**最新 A556–576 分别核对自身 t 与捕获尾槽，与 D341–352/native R747–781 一致；A578–581 核对 pattern 原 begin/end 与 values 数量。本项只接受当前源码合同，不预记执行通过；值内容/对象身份仍见 R8。

- **资源已不再只作借用 target：**D124–127/234/249 转到 owned declaration，D107 恢复 initializer；A497–502→528–530→238–247 会递归核对资源 d 与 initializer。仍须 R1 的 owner/binder 修正；不能继续报告“Try resources 没有 owned 入口”。Spawn 的 future 也已接该入口，但 A386–509 没有对应 future target 专项断言。
- **同代同包 identity 路径存在：**CfirDeserializedSymbolProvider.kt:152–177 使用同一 loaded.pkg 创建共享 context；L15–18 的类型/常量/声明读取使用它。provider 先以对象身份核对 function（Abstract…:467–468），再返回缓存图。A103–132 核对重复图、header 参数/类型与 loaded snapshot 相同；不因缺少名为 sourceSnapshot 的字段就断言重读。R6 是明确的外包位置选择缺口。未验收同路径内容替换后的新 session、跨 snapshot raw-index 隔离与失败重试。
- **完成后发布路径存在：**D76 先 builder.finish；L37 才写 context.sourceGraph；CfirDeserializationContext.kt:99–144 在根事务以 volatile completedGraph 同时发布 d/t/source，失败不发布本次图。对照 Kotlin ValueWithPostCompute.kt:55–81，completedGraph 容器自身有完成门；F4/R9 的公开 provider/tag 缓存外泄仍是具体阻断。跨 context 互等、重入与取消仍无本轮行为证据，不能据单包锁注释宣称全部并发安全。
- **control 的窄项可接受：**Jump 使用真实 wire.mapExpr，未按 break/continue 标量猜最近 loop；graph.finish 检查实际 loop kind，A394 核对同一 e identity。Return 在普通直接函数/匿名 F 的主体下能指向相应函数；R3 阻止把此结论扩大到局部 initializer/default 的实际 F 域。
- **fresh SOURCE18 的源码接线可见，结果未判：**A60–109 创建实际以导入接口作为参数/返回类型的新 frontend consumer，经 Library session 正式 provider 请求图；CfirSourceExpressionExportTest.kt:411–417 在 fresh SAVE_CJO、严格 wire 检查后调用 A。A182 的“覆盖全部 allExprs”属于这些 fixture 的断言，不是生产 loader 应无条件物化整张 e 表的依据。A256–268 把命名函数签名 returnType 与 F.retType 直接比较，未核对 D.functionHeaders 的独立保存产品；A445–477 仅对 lambda header 作专项检查。当前测试入口和生产/LL 注册不能证明 ordinary/common/depends-on/LL 已实际消费完整正文。

## 当前候选文件指纹

坐标对应本次读取的内容；不能将工作树 HEAD 或旧通过窗口当这些字节的验证结果。

| 文件 | 行数 | SHA256 |
| --- | --- | --- |
| D | 383 | `64298294DBDBD386149347CDA2DD043215B44197666727C08732754F2F0546A4` |
| L | 76 | `D72DBFD6FA74712B7C8E0C6ACBDB9ADF96098AB722C3B69431E8C57C121B40DB` |
| V | 85 | `7CAEAC7CEC50EB17BF3D969AA2821345581501BF1B92154DE593AFA1CAD48DA6` |
| P | 13 | `BC8B6C55CBF193FC05F4081938D32B5DA49697388CBD99F8C7DADC115C5D81A8` |
| DD | 2773 | `A34EDE7DE2164F80A5D0A889EF47787E8393782BFBB4A2BB96CAA339D1DC3D14` |
| A | 711 | `33D1BA309DFE3949A48F9D4983537B6BC29F8D1EC5C4BE2E8FBEDEE131A6CAA5` |
