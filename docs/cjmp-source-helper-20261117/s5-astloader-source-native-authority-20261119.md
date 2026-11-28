# S5 完整 SOURCE ASTLoader：固定 native 权威报告（2026-11-19）

角色：CangjieSemanticsAuthority。先读 [continuation](../cjmp-source-type-use-continuation-20261119.md)、[source-chain-delivery](../cjmp-source-chain-delivery-20261116.md)。按主线程优先级先交付的 [类型身份报告](s5-type-identity-native-authority-20261119.md)已覆盖 Type(28)、Generic(29)、全部 TP 身份先登记再 bounds、跨包 canonical 与原 constraints/final bounds。本报告补 SOURCE expression、owner/control、schema33 与正式消费。

证据唯一版本：`D:/code/intellij/cangjie/external/cangjie_compiler` 的 `14276740ffec1262b81b6b5236b1d840af270b37`；所有 native 内容来自 `git show <SHA>:<path>`，下文坐标是固定对象行号。没有 checkout 或读取 native 工作文件冒充固定对象。只新增授权 docs 报告；未修改生产/测试，未联系旧会话，未运行 Gradle/cjc，未实施后端。以下是源码合同，不是本库 S5/S6 已通过的验收证据。

简称：L=`src/Modules/ASTSerialization/ASTLoader.cpp`，E=`…/ExprLoader.cpp`，R=`…/ReferenceLoader.cpp`，W=`…/ExprWriter.cpp`，H=`…/ASTLoaderImpl.h`。所有链接固定同一 SHA。

## 1. LoadExpr、basic、node 壳与阶段合同

| 固定坐标 | 实际行为与 S5 合同 |
| --- | --- |
| [ModuleFormat.fbs:11–18](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L11)、664–674 | 正式 CJO AST 是 `CJOF`，d/e/t 分表。索引0为无效，非零为表 offset＋1。注释要求重建 symbol table，不在 loader 内重做 loaded module 的类型推断/检查。不要混用 NodeFormat（语法树缓存）或 PackageFormat（CHIR）。 |
| [H:146–166](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoaderImpl.h#L146) | `MakeOwned<NodeT>` 创建真实 concrete node。e 壳立即登记到 allLoadedExprs，并读 overflowPolicy；d 壳立即登记到 allLoadedDecls；随后恢复 begin/end。handler 然后递归装拥有子树。basic 不解析符号、不构造最终 Ty、不选 overload。 |
| [E:418–442](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprLoader.cpp#L418)、[H:308–340](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoaderImpl.h#L308) | LoadExpr(0)返回空 owned ptr；非零按 kind handler 建真实 concrete。不支持 kind 走 E:25–30 的 unsupported 诊断与 InvalidExpr。合法 supported 节点不可退化成普通 RefExpr/annotation 壳。 |
| [L:968–979](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L968)、343–386 | LoadDecl 同样按 concrete kind；包入口只扫 isTopLevel roots，再沿 nominal body、FuncBody、Block、Pattern 等拥有边建非 top-level nodes。局部 decl 不是额外顶层声明。 |
| [R:240–265](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L240) | 顺序为所有已加载 decl refs/dependencies → 所有已加载 expr refs → dummy 包装类型 → 全 expr 的 PostLoadReference。decl.ty、expr.ty、FullId、map/loop 回边不在拥有子树递归期间重新解析。 |
| [R:586–650](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L586) | 首先 expr.ty、mapExpr；再 target、instTys、matchedParentTy、Array initFunc、lambda body.ty、literal 值和 Jump.refLoop；随后补非 Expr 辅助节点 refs。 |
| [H:342–347](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoaderImpl.h#L342)、[L:532–569](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L532) | GetExprByIndex 只取已登记同 e 身份；GetDeclFromIndex 的正式合同要求全部 decl 已加载，按当前包 d或目标包 exportId 路由。回边不能调用 LoadExpr/LoadDecl 重建拥有副本。 |
| [R:172–200](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L172) | Call.resolvedFunction 最后从 baseFunc.GetTarget 取真实 FuncDecl，不能在 base refs 尚未完成时选一个名字匹配函数。Array/Pointer FuncArg.ty 从完整 arg.expr.ty 回填；Call FuncArg.ty 另按其自身 wire type 读取。 |

**native 壳的边界：**它是拥有树递归中“建 concrete 后立即登记”，不是“先遍历 allExprs 无条件给每槽造 owner”。LoadExpr/LoadDecl 的 owning 入口本身没有从 allLoadedExprs/allLoadedDecls 复用 owned ptr 的分支，map 的 emplace 也不是完整 ready-state 协议。JVM 的共享物化应保持真实 owning edge 唯一、非拥有回边复用同一 identity，并按自己的正式图上下文完成 reserve/fill；不能宣称 native 在拥有边的任意重复请求下提供通用 memoization。

E:431–438对“以普通 Expr 形式读取 Block”建立 COMPILER_ADD RefExpr/desugarExpr 包装，原 Block 仍登记为 wire e 身份；源码注释明确这是 CHIR goto 使用。E:295–299针对 If.elseBody 恢复普通 Block 条件。它是 native 下游适配，不是 SOURCE 壳缺失时的替代实现；本任务不实施后端包装。

## 2. local decl owner、参数/helper 与 Return/loop/target

| 固定坐标 | 身份/归属恢复 |
| --- | --- |
| [E:361–380](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprLoader.cpp#L361)、[W:520–548](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprWriter.cpp#L520) | Block 每个 operands[i] 由 isExpr[i] 决定读 e 或 d，保留真实 mixed 顺序；尾 isExpr[size] 是 UNSAFE 标量，不是子节点。local d 的拥有者由此结构建立，不能按 operands 名称全部读 e。 |
| [E:577–584](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprLoader.cpp#L577)、[W:752–765](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprWriter.cpp#L752) | VarPattern.exprs[0] 实际为拥有的 VarDecl **d索引**；loader 设置 varDecl.parentPattern=实际 pattern。pattern.ty 的 t索引另从 types[0] 读。 |
| [E:303–327](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprLoader.cpp#L303)、250–262；[W:444–451](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprWriter.cpp#L444) | Try.resources、Spawn.future 是当前表域内的 local owned d，FullId.index 用于 LoadDecl；不是外部 borrowed target，不能走按名字寻找的跨包兜底。Try catch patterns/Block 与 resource d 保持独立边。 |
| [L:763–791](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L763) | paramLists 按顺序读 params[i] 与对齐 desugars[i]；desugars[i]=0 表示无 helper。ParamInfo.defaultVal 独立恢复原 assignment，不能拿 helper.body 代替它。 |
| [L:794–834](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L794) | FuncBody.funcDecl 指向同一函数；own Generic 的 TP owner 是该函数；helper.ownerFunc 在826行指向它，并继承 frozen。helper 的 outerDecl 与 ownerFunc 是不同角色。 |
| [Utils.cpp:233–259](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Utils.cpp#L233)、L:817 | SetOuterFunctionDecl 以真实 FuncBody 为 root；对 FuncParam 与嵌套 FuncDecl 设置 outerDecl 并跳过其子树。lambda 不是 Decl，不能当 native outerDecl。**该实现没有给每个 local VarDecl 写 outerDecl**；不能照注释扩大成通用 local-var owner walker。 |
| [L:139–147](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L139)、847–855、888–898；R:25–62 | nominal member outerDecl、accessor.propDecl、helper 的外层 nominal 归属按真实拥有成员恢复；refs 后再设置 parentClassLike/Struct/Enum。外层类、直接函数、lambda FuncBody、helper owner 不合并。 |
| [E:50–55](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprLoader.cpp#L50)、[488–509](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprLoader.cpp#L488) | Return 只拥有 expr；没有 ReturnInfo/FullId target。选中 FuncBody.body 加载后 Walker 回填 refFuncBody，遇嵌套 FUNC_BODY 跳过，让内层自己回填。普通函数、嵌套函数、lambda 各自实际 body 身份不可串绑。 |
| [W:59–84](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprWriter.cpp#L59)、[R:641–645](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L641) | Jump.mapExpr 槽复用为实际 refLoop 的 e index；refs 完成后从登记 map 取同一个 loop，转入 refLoop。isBreak 仅是 break/continue 标量，不能据它或当前 walker 猜最近 loop。 |
| [R:592–610](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L592)、[W:136–175](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprWriter.cpp#L136) | Ref/Member target、instTys、matchedParentTy 分槽；名字用于显示，target 从 FullId 绑定。Ref writer 可选择 target.genericDecl 的实际身份；reader 不再次 overload resolve。Call 的结果 expr.ty 与 base signature、instTys 也不合并。 |
| [R:732–742](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L732) | MatchCase 自身 ty 与各 pattern refs 后补；pattern.ctxExpr 是同一个 Match.selector，不是复制 selector。 |

**local owner 的正式边界：**Decl wire（ModuleFormat:277–303）没有独立通用 outerDecl 字段。Block/Pattern/Try/Spawn 的拥有关系、实际 FuncBody、TP owner、函数/member/helper 关系共同恢复；普通局部变量的 native lexical scope 另由 [Collector::CollectFuncBody:321–339](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Collector.cpp#L321)、CollectVarDecl:377–383、AddSymbol:43–58建立。AddSymbol 写 scopeName/scopeLevel/symbol，不写 VarDecl.outerDecl。CFIR 若要求 containing symbol，必须从已恢复的真实拥有域建立自身 owner 合同；不能凭空引用 native 的通用 setter。

## 3. schema33 逐槽角色

这里的 **schema33** 是固定 writer 的 **30 个普通表达式＋3 个辅助记录**，不是 CJO format 版本号。计数依据 [ASTWriterImpl.h:263–299](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriterImpl.h#L263)。辅助记录同样放在 allExprs，却不是 AST::Expr；不能用普通 kind dispatcher 替代其 owner 入口。以下 `e`/`d`/`t` 指各自表域的 wire 索引，`P` 是内联 Pattern，`F` 是内联 FuncBody。槽编号均从0起。

| 序号 / 记录 | operands/拥有槽角色 | info/回边角色 | 固定 loader / writer 行号 |
| --- | --- | --- | --- |
| 1 WildcardExpr | 空 | expr.type=t | E33–37 / W105 |
| 2 PrimitiveTypeExpr | 空 | 从 expr.type 所指 SemaTy.kind 建 primitive concrete | E39–47 / W97 |
| 3 ReturnExpr | `[e:value]`，必需 | refFuncBody 不在 wire；按所属 F 回填 | E50–55、488–509 / W113 |
| 4 JumpExpr | 空 | JumpInfo.isBreak；mapExpr=非拥有 loop e | E58–64、R641–645 / W125、72–74 |
| 5 MemberAccess | `[e:base]` | ReferenceInfo.reference/target FullId/instTys t[]/matchedParentTy t | E67–78、R592–599 / W136 |
| 6 RefExpr | 空 | 同上；this/super 由 reference 识别 | E81–89、R602–609 / W159 |
| 7 CallExpr | `[e:baseFunc, auxiliary-e:FuncArg…]` | CallKind/hasSideEffect；resolvedFunction 最后取 base target | E92–115、R660–673、172–182 / W179 |
| 8 UnaryExpr | `[e:operand]` | UnaryInfo.op | E118–127 / W203 |
| 9 IncOrDecExpr | `[e:operand]` | IncOrDecInfo.op | E130–138 / W218 |
| 10 LitConstExpr | 空 | literal kind/string kind/strValue；值按完成 ty 初始化 | E141–151、R635–639 / W233 |
| 11 BinaryExpr | `[e:left,e:right]` | BinaryInfo.op | E154–163 / W246 |
| 12 SubscriptExpr | `[e:base,e:index…]` | SubscriptInfo.isTupleAccess | E166–183 / W264 |
| 13 AssignExpr | `[e:leftValue,e:rightExpr]` | AssignInfo.op/isCompound | E186–195 / W284 |
| 14 ArrayExpr | `[auxiliary-e:FuncArg…]` | ArrayInfo.isValueArray/initFunc FullId；非VArray initFunc可空 | E198–208、R622–629、184–188 / W302 |
| 15 PointerExpr | `[]`或`[auxiliary-e:FuncArg]` | arg.ty 最后取完整 arg.expr.ty | E211–217、R190–195 / W320 |
| 16 TypeConvExpr | `[e:operand]` | expr.type 是结果语义 t | E220–225 / W334 |
| 17 ThrowExpr | `[e:value]` | expr.type=t | E228–233 / W347 |
| 18 PerformExpr | `[e:value]` | expr.type=t | E236–241 / W360 |
| 19 SpawnExpr | `[]`或`[e:arg]` | SpawnInfo.future=当前 local owned d的FullId；task不是本载荷子树 | E250–262 / W373 |
| 20 ArrayLit | `[e:element…]` | ArrayInfo.initFunc FullId；VArray不读 initFunc | E265–272、R612–619 / W395 |
| 21 TupleLit | `[e:element…]` | expr.type=t | E275–282 / W413 |
| 22 IfExpr | `[e:cond,e:thenBlock,e:else或0]` | then以 Block concrete 读；最后空槽保留 | E285–300 / W427–441 |
| 23 TryExpr | `[e:tryBlock,e:catchBlock…,e:finally或0]` | TryInfo.resources FullId[]拥有local d；patterns P[]；最后finally槽固定 | E303–327、R676–682 / W444–474 |
| 24 WhileExpr | `[e:cond,e:bodyBlock]` | mapExpr为普通非拥有表达式回边 | E330–337 / W477 |
| 25 DoWhileExpr | `[e:bodyBlock,e:cond]` | 顺序与While不同 | E339–345 / W492 |
| 26 LambdaExpr | 空 | LambdaInfo.funcBody=F，supportMock；F.ty=expr.ty | E348–358、R632–634 / W507 |
| 27 Block | `[e或d:statement…]` | isExpr[i]是每槽表域；isExpr[size]是UNSAFE | E361–380 / W520–548 |
| 28 MatchExpr | matchMode=true:`[e:selector,aux-e:MatchCase…]`；false:`[aux-e:MatchCaseOther…]` | MatchInfo.matchMode；case/pattern ty与selector ctx另补 | E383–401、R685–699 / W551 |
| 29 LetPatternDestructor | `[e:initializer]` | LetPatternDestructorInfo.patterns P[]；pattern refs另补 | E404–414、R714–724 / W579 |
| 30 ForInExpr | `[e:inExpression,e:guard或0,e:bodyBlock]` | ForInInfo.pattern P/forInKind；有效canonical kind的inExpression按Block读 | E460–475、R702–705 / W598–617 |
| 31 FuncArg 辅助记录 | 普通:`[e:expr]`；default placeholder:`[]` | kind默认InvalidExpr，靠FuncArgInfo识别；withInout/isDefaultVal；自身type=t | E516–531、R660–672 / W667–686 |
| 32 MatchCase 辅助记录 | `[e:guard或0,e:bodyBlock]` | MatchCaseInfo.patterns P[]；case.ty=t；pattern.ctxExpr=selector | E445–457、R732–742 / W620–644 |
| 33 MatchCaseOther 辅助记录 | `[e:condition,e:bodyBlock]` | case.ty从该记录.type补读 | E478–485、R695–698 / W647–664 |

公共 `Expr.begin/end/type/overflowPolicy/mapExpr` 在 H146–166、R586–589恢复。operands 不可整体改为“去重的依赖列表”；长度、顺序、重复值、固定0槽、mixed d/e角色都属于原合同。

**计数外的固定对象事实：**schema 中仍声明其他 kinds（ModuleFormat:333–373）；reader map 额外登记 ResumeExpr，但 E244–248只创建空 Resume 壳，没有读 withExpr/throwingExpr。writer map263–294没有 Resume 项，也没有 SaveExpression(ResumeExpr) 重载。这不能算完成 Resume 的双向 SOURCE 支持；已有 SOURCE33 也不能扩大为全部 schema enum 已通过。IncOrDec 在本固定 writer 中确有正式项，即使当前 canonical 输入常由解糖得到 Assign，不能从33表删掉它。

### Pattern、Generic 与 F 的附加槽

- Pattern.types[0] 总是 pattern 本身的 semantic t。Except/Command 的 types[1…] 才是原捕获 Type 选择的 t槽，R765–781逐个 WrapType；不能把首槽与尾槽合并。VarPattern.exprs[0]为d；ConstPattern.exprs是literal/operator call的e；EnumPattern.exprs[0]为constructor e。P.patterns 是内联拥有子pattern，R745–787补完整递归 refs。TypePattern runtime两个布尔标记在 E597–604恢复。
- F.paramLists 是 params d[]与desugars d[]的对齐记录；F.retType=t；F.body=e Block或0；always与captureKind是独立标量。没有 wire ret Type 语法节点时，声明的函数签名返回类型和F.retType的0槽不能合并。R481–491还有“非generic ctor且无已加载body”的ret node排除，不能推广为全部constructor/body缺省。
- Generic.typeParameters d[]、Constraint.type/uppers t[]、GenericTyInfo.upperBounds t[]及真实FullId的完整顺序已在类型报告逐槽列出，不能让 expression materializer重推一遍。

## 4. header-only 与 SOURCE 消费：正式开关及调用链

| 固定坐标 | 模式/正式接线 |
| --- | --- |
| [ImportManager.h:282–294](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/Modules/ImportManager.h#L282)、407 | importSrcCode默认true，公开 SetSourceCodeImportStatus(false)供LSP。native固定src/include搜索没有该 setter 的内部调用者；公开入口注释不能冒充已观察外部LSP实际调用。 |
| [CjoManager.cpp:493–510](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/CjoManager.cpp#L493) | 一次获取CJO buffer（或已缓存buffer）创建真实loader，509行正式传入importSrcCode；不是每个type/body请求重新开文件。H87的loader初始false被这里显式设置。 |
| [E:488–513](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprLoader.cpp#L488)、[H:135–136](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoaderImpl.h#L135) | 先加载全部paramLists，正文门为 `importSrcCode && (body.always || isInline || isConst)`；进入才读F.body/captureKind并回填Return。**此固定版本isConst默认true**；L806只传isInline，Lambda E354只传body，均采用这个默认值，不能误报native此处已把普通函数体排除。 |
| [ASTWriter.cpp:1258–1291](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1258) | writer另行选择内容：exportContent && exportFuncBody && (!fd || CanBeSrcExported || isGenericCJMP)，validBody还要求正确Ty和实际body；always写false。header/source读取能力与writer是否真的选中body是两项事实。未选中body的合法0不能伪装成选中body丢失。 |
| [L:703–735](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L703) | Var initializer门为 `(importSrcCode && !COMMON) || deserializingCommon`；VarWithPattern的local initializer直接读取。Param default assignment同样不走F.body门。**header-only不等于所有e都被删掉或全部type不读。** |
| [H:203–218](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoaderImpl.h#L203) | 按真实代码：MAIN_ENTRY忽略；!importSrcCode时忽略GENERIC_INSTANTIATED和PRIVATE且非IMPLICIT_USED；macro还另排除实例化/非MACRO_FUNC。不能用该方法旧注释扩大成“所有非EXTERNAL都忽略”。 |
| [CompilerInstance.cpp:1010–1015](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Frontend/CompilerInstance.cpp#L1010)、[ImportManager.cpp:540–565](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ImportManager.cpp#L540) | 正式frontend ImportPackages路径调用BuildIndex→ResolveImportedPackages：先common文件与依赖header，再LoadPackageDeclsOnDemand。558行有CANGJIE_CODEGEN_CJNATIVE_BACKEND构建条件，应如实保留；这里考证的是native构建里的前端加载，不实施CHIR/codegen。 |
| [CjoManager.cpp:345–394](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/CjoManager.cpp#L345) | common与所需依赖包先全加载声明，再全LoadRefs；最后按fromLsp是否替换Alias使用视图。source能力不是另一套annotation decoder。 |
| [L:794–815](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L794)、343–386；[ImportManager.cpp:946–959](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ImportManager.cpp#L946) | 已加载正文声明发布SRC_IMPORTED/srcImportedNonGenericDecls；包发布hasSourceImportedDecl，BuildIndex把这些imported packages加入正式packages列表。不能只在test-only decode工厂里返回一棵树。 |
| [TypeChecker.cpp:2015–2032](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L2015)、2086–2125、2170–2177 | 正式ASTContext存在才进入PrepareTypeCheck；common合并先于Collector.BuildSymbolTable；TypeCheck明列source imported decl与imported generic member的消费。不能把schema“loader不重推断”误写成“后续正式frontend完全不消费loaded source”。 |
| [Collector.cpp:641–650](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Collector.cpp#L641) | FROM_COMMON_PART文件中的Expr在Collector专门跳过；generic/type/member仍按各自入口处理。common、ordinary的语义消费不能仅用一个“imported”布尔概括。 |
| [ImportManager.cpp:968–1008](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ImportManager.cpp#L968) | 公共LoadPackageFromCjo先加载header和依赖header，再调用LoadPackageDeclsOnDemand({package},true)，最后AddPackageDeclMap。fromLsp=true控制Alias替换；它本身没有设置importSrcCode=false，两个模式参数不能混成一个。 |

common前置文件路径：[L:256–327](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L256)设置deserializingCommon，在当前specific Package下建common File与allFileInfo位置，标记FROM_COMMON_PART/COMMON；后续common loader的d/e/t表域仍独立。不能因最终挂在同一Package，就把common输入索引与ordinary输入索引混用。

## 5. SOURCE位置、annotation 与恢复能力的边界

[L:405–434](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L405)为ordinary imported file调用 `sourceManager.AddSource(path,"",packageName)`，注册文件身份而不重读源程序。[L:476–500](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L476)按Position.pkgId/file不同索引约定映射实际FileID，再返回line/column；当前包file非零减1，外包pkgId减1但外包file直接索引。**Position.pkgId约定不同于FullId的-1/-2/-3**（ASTSerializeUtils.h:37–39），不能复用同一个index转换。固定LoadPos返回值没有读取ignore标量；本轮只陈述这一行为，不虚构已恢复全部PositionStatus。L364/386 AddCurFile使真实子节点指向所属File。

[L:687–700](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L687)的annotation argument也是调用同一个LoadExpr；非Annotation kind要求constant literal。[R:453–465](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L453)另恢复custom annotation constructor target。annotation不是普通SOURCE正文的简化替代reader；新增统一图materializer应让这些正式owner共享d/e/t上下文，而不是四种reader互相兜底。

完整SOURCE恢复指 **已选择的canonical SOURCE语义图**：concrete nodes、拥有字段、共享符号/TP/Alias、完整类型槽、local/pattern/helper、Return/loop/target回边与位置/表域。native writer的GetRealExpr（W45–56）会沿desugar并剥Paren；不能把这个合同扩大为原源文件每个token/语法节点的完全重建。

## 6. S5/S6 可采用的交付边界

类型报告的身份/bounds barrier完成后，expression层必须在同一正式输入上下文恢复本表所有可达拥有节点；非拥有边和type refs指向同一已登记身份，辅助记录由实际owner入口读取，最后补完成依赖字段。合法未选中body与header-only能力应显式保留；实际选中且非零body不能变空，不能以Unit/Ref/annotation placeholder降低结构。

正式ordinary/common/depends-on/frontend/LL消费者必须从正常package provider/context请求这份完整产品。native此处证明了ImportManager/CjoManager/frontend调用与common模式；**没有native Kotlin LL API的同名实现，也没有本库LL当前接线验收**，不将native公共LSP入口当成本库LL已完成。生产、测试、真实新CJO行为窗口与全S5/S6关闭仍由主线程完成。本报告不实施后端，不调整原分母或旧执行证据。
