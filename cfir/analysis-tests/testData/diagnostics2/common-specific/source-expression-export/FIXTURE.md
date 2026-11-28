# SOURCE expression export：v1.1.3 正文 fixture

本目录是 CangjieTestSemanticsWriter 交给主线程的独立原生源码输入和图约束。四个 `.cj` 文件分别构成一个包，均为合法正例，期望无错误、无警告。接口成员有正文时具有原生 DEFAULT 语义，因此这些正文实际满足 SOURCE 导出选择条件。没有伪造 `SOURCE` 注解、测试指令或私有 encoding。主线程负责接入 harness；本次没有运行本仓解析器、测试或 Gradle，也没有据其输出制定期望。

## 官方证据身份与核验边界

核验日期：2026-11-14。已先读取 `kotlin-cangjie-frontend-parity/SKILL.md`。本角色只准备语言语义 fixture；没有宣称生产实现通过 KotlinParityGatekeeper。只读查看 Kotlin `compiler/testData/writeFlags/jvm8/defaults/all-compatibility/defaultImplementations.kt` 的接口正文 fixture 形态，不移植其 JVM DefaultImpls、默认实参或字节码语义。

- SDK：`C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe`。
- 实际版本输出：`Cangjie Compiler: 1.1.3 (cjnative)`，`Target: x86_64-w64-mingw32`。
- SDK 可执行文件 SHA-256：`dfebed3139afe00ee642f75322ab82d1d97651a5e9f10ebf4f6d86abac061257`。
- 官方源码只读目录：`D:/code/intellij/cangjie/external/cangjie_compiler`。当前 checkout 是 1.3 开发版本，本次所有协议判断读取 `git show v1.1.3:<path>`，固定 commit `14276740ffec1262b81b6b5236b1d840af270b37`。
- 解码工具：本工作树 `flatbuffers-gen/build/flatc/flatc.exe`，实际版本 `25.2.10`，SHA-256 `1688ff18cb5368133cbdf1e100d6992c33eb64733cdfdddec122cff447df811a`。
- 解码 schema：从该官方 commit 的 `schema/ModuleFormat.fbs` 经 apply_patch 保存到 `build/source-expression-export-v113/schema/ModuleFormat.fbs`；保存文件 SHA-256 `86153fd08c4c0de6e1c913378a36c93a6f9fd349fa9823f232cb645c5168ee75`。它的标识是 `CJOF`，不是 CHIR 的 `PackageFormat.fbs`，也不是本仓当前 ModuleFormat 的未经核对副本。
- 文档 MCP 已读取：`manual_source_zh_cn_interface`、`manual_source_zh_cn_handle`、`manual_source_zh_cn_typecast`、`manual_source_zh_cn_expression_break_与_continue_表达式_12`；lambda 语法另由 `manual_source_zh_cn_lambda` 搜索结果核对。这些文档描述语言规则，固定 v113 的 SDK 与源码决定本批具体图。

四个最终源文件均已实际编译为静态库，退出码全部为 0，stdout/stderr 全部为空；四个 CJO 均由上述 flatc 解码成功，`version="1.1.3"`、`cjoVersion={0,1,0}`。总计五次源码编译：首次 declarationsCalls 因 lambda 间接捕获可变变量而失败，修正该真实语义问题后只复编译此文件。其余三文件各一次；没有扩大 SDK 矩阵。

证据根目录为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie/build/source-expression-export-v113/`。表中的相对位置均以此为根；`.a`、CJO、解码 JSON、stdout.log、stderr.log 及各阶段官方 AST 均在相应目录。AST 是官方编译产物，本次图判断主要核对 CJO JSON，没有做完整 AST 文本逐节点比较。

| 源文件 | 最终证据子目录 | CJO / JSON 文件名（只差扩展名） | allDecls / allExprs |
| --- | --- | --- | --- |
| declarationsCalls.cj | declarationsCalls/final | source_declarations_calls | 20 / 32 |
| controlFlow.cj | controlFlow | source_control_flow | 10 / 73 |
| operatorsConversions.cj | operatorsConversions | source_operators_conversions | 22 / 93 |
| exceptionsResources.cj | exceptionsResources | source_exceptions_resources | 29 / 163 |

| 文件 | 最终源 SHA-256 | CJO SHA-256 |
| --- | --- | --- |
| declarationsCalls.cj | bf8f88e2398ba1f562f848ab0814f68cef90c7803b96c3b1bc6394d145ad9002 | 0b574839813a640f3a330128624d59d529c49090b972dfb227ac2fb84eb089a6 |
| controlFlow.cj | 1d83b1b49f11f98adcb5c5a9e025dc1993ae396436633983624b5960c811930d | e872f6eae620aa79716898e64e121232ef38a2862cacf8a337e35f63e6b2d2f4 |
| operatorsConversions.cj | 5b237eb7082ec4d061f9cff7c341dfc8c39ee0af81018db489f0d5be0d2acb98 | af44dd5dc2007e24590d3e78a7346fbaf65d92643f5929f2860377453ab168da |
| exceptionsResources.cj | 4a017a550e5cd25d828c2750872bf2c1053746bd5e064a0252f7de4cc49d4858 | f4d9de7867e2fd41e366c0293ba83842f3947f29805729fb22c8087f7099aa3c |

## 图的共同约束

官方依据：v113 `include/cangjie/AST/Utils.h:174–195` 的 `IsDefaultImplementation/CanBeSrcExported`，`src/Modules/ASTSerialization/ASTWriter.cpp:1256–1293` 的 `SaveFuncBody`，以及 `src/Modules/ASTSerialization/ExprWriter.cpp` 的各节点 writer。导出选择仍要求真实 exportContent/exportFuncBody 阶段、正确类型和有效正文；不能因为某个函数有源码就导出所有函数正文。

所有索引都是对应表的 1-based ID，0 表示缺失。下文数字是本次固定产物的定位证据，不作为跨运行稳定 ID；harness 应按包、声明、节点和边对应关系比较，保留同一声明及表达式的共享身份。

- 从被选择函数的 `FuncInfo.funcBody.body` 进入 `Block`。Block.operands 混合声明与表达式 ID，必须用 BlockInfo.isExpr 的逐项标签区分表；其最后一个布尔值是 unsafe 标记，长度必须比 operands 多 1，不能当成额外子节点。
- 声明中的 initializer、函数和 lambda 的 FuncBody、pattern 子树和引用 FullId 都是图边。不能只递归 Expr.operands，也不能将 VarPattern.exprs 中的声明 ID 当成表达式 ID。
- `CallExpr.operands` 按 baseFunc、实参 wrapper 排列。v113 的 `SaveFuncArg` 没有设置 kind：本批实际 wrapper 是 `kind=InvalidExpr + info_type=FuncArgInfo`，每个显式实参 wrapper 含一个表达式 operand，`withInout=false,isDefaultVal=false`。这个有协议的 wrapper 不能被删掉，也不能发明 ExprKind_FuncArg；其他无协议的 InvalidExpr 不能因此获准。
- `MemberAccess.operands=[receiver]`，ReferenceInfo 包含被解析的函数/成员身份、instTys 和 matchedParentTy。当前包 FullId 的 pkgId 为 -2；0 不是当前包编号。
- `GetRealExpr` 跟随 desugarExpr，并去除 ParenExpr 包装。不能要求每一个表面语法都保留同名 SOURCE ExprKind；官方生成节点的源码行列可能为 0，不能用人工位置代替官方证据。

## declarationsCalls.cj：局部声明、引用、receiver、lambda 与返回

`evaluate` 将普通 private 函数作为函数值存入 `step`，声明可变局部量及局部函数，将调用局部函数的 lambda 存入 `transform`，再赋值并通过 receiver 调用成员。lambda 经 nested 只捕获不可变参数 input；可变 local 不进入捕获链。`implicitValue` 最后一个表达式产生返回值，`explicitUnit` 的裸 return 返回 Unit。

实际 SDK 图：evaluate 的 Decl#1.body=Expr#1，Block.operands=`[5,7,8,10,11,17]`，isExpr=`[false,false,false,false,true,true,false]`。Decl#5 step.initializer=Expr#2，RefExpr("privateStep").target=`{pkgId:-2,index:6}`；Decl#7 local.initializer=Expr#3；Decl#8 是 nested；Decl#10 transform.initializer=Expr#4 LambdaExpr，其 FuncBody.body=Expr#5。Decl#6 privateStep 的签名和返回类型存在而 body=0；本产物 nested.body=0、SourceReceiver.add.body=0，不能据源码存在发明它们的非零正文，也不把本例推广成所有局部函数正文必须为 0。

evaluate 的 ReturnExpr#17 → CallExpr#18 → MemberAccess#19 → receiver RefExpr#20，成员目标是 Decl#12 add。实参 wrapper Expr#21 → transform 调用 Expr#22，保留内层调用与 local 的声明引用。函数值调用在本次官方产物的 callKind 为 CallStructCreation，不能凭名称改写成 CallFunctionPtr。

implicitValue 的 Decl#13.body=Expr#26，Block 内先是 copy 声明，再是 SDK 生成的 ReturnExpr#28 → RefExpr#29(copy)。explicitUnit 的 Decl#16.body=Expr#30 → ReturnExpr#31 → Unit literal Expr#32。源码的 implicit/explicit 区别不能用于丢弃其最终 ReturnExpr。

原始失败证据保留于 `declarationsCalls/stderr.log`：`sema_func_capture_var_cannot_assign`，源位置 22:43，消息为 `lambda captured a mutable variable transitively, lambda cannot be assigned to a variable`。该日志属于修改前源码，不属于最终文件的诊断期望。

## controlFlow.cj：分支、三类循环与循环身份

walk 的 while 每轮先递增 index，跳过 2，超过 4 时退出；do-while 至少执行一次；for-in 遍历半开 Int64 区间 `0..limit`，在 1 时 continue，达到 3 时 break。choose 的最后一个 if/else 的两个分支均为 Int64，因此整个函数返回 Int64。所有 break/continue 均处于对应循环正文内，具备 Nothing 语义。

实际 SDK 图：walk Decl#1.body=Expr#1，choose Decl#8.body=Expr#66。WhileExpr#4.operands=`[5,8]`（condition, body）；DoWhileExpr#27.operands=`[28,32]`（body, condition）；ForInExpr#35.operands=`[37,0,46]`（inExpression, absent guard, body），ForInInfo.forInKind=Range、pattern=VarPattern，模式绑定必须指向 element 声明。区间及其生成节点按官方展开图读取，不发明 RangeExpr writer。

JumpExpr#17/#23 的 mapExpr 都为 WhileExpr#4；#52/#63 的 mapExpr 都为 ForInExpr#35。前者 isBreak=false，后者 isBreak=true。该字段复用为 refLoop，是回边；不能复制成不同循环身份，也不能作为普通树无限递归。没有 else 的 IfExpr 必须保留第 3 个 operand=0；有 else 的分支都必须存在。

本 SDK 把源码 index++ 导出为 AssignExpr#9，op=AddAssign、isCompound=true；不是 IncOrDecExpr。其余 +=/-= 同样保留 AssignInfo 的复合赋值信息。不能仅为覆盖 IncOrDec schema 而改造这个实际结果。

## operatorsConversions.cj：内建运算、tuple、数组、match 与转换

calculate 使用 Int64 一元负号、Bool 逻辑非、普通赋值、复合赋值、tuple 及其固定索引、数组字面量与数组读写；`Int32(...)` 和 `Int64(...)` 是语言支持的显式数值转换。整数运算及窄化可能在运行时溢出，这不使合法源文件成为负例；本次不执行函数。

实际 SDK 图：calculate Decl#1.body=Expr#1。AssignExpr#3 是 op=Assign、isCompound=false；#7 是 AddAssign、true。UnaryExpr#5.op=Subtract，#12.op=Not；保留 BinaryExpr 的左右 operand 顺序。ArrayLit 存在并携带真实 initFunc 身份。tuple 读取 SubscriptExpr#15/#35 的 isTupleAccess=true。Array<Int64> 的读写已展开为调用，不能要求这些操作也变成 tuple SubscriptExpr。TypeConvExpr#34/#40 各含一个 operand，目标类型由 Expr.type 表示，不虚构额外 cast info。

inspect Decl#8.body=Expr#47，结果是 `(Bool,Option<ConversionLeaf>)`。`is` 展开为 MatchExpr#50.operands=`[51,52,55]`，成功 type-pattern 分支返回 true，wildcard 分支返回 false；`as` 展开为 MatchExpr#58.operands=`[59,60,66]`，成功绑定分支调用 Some，失败分支为 None。两者 matchMode=true。这一规则由官方 `src/Sema/Desugar/AfterTypeCheck/IsExpr.cpp` 和 `AsExpr.cpp` 的变换及实际 CJO 共同确认，不使用假 IsExpr/AsExpr encoding。

select Decl#13.body=Expr#69：`let (left,right)=pair` 在 CJO 中是 Decl#15 VarWithPatternDecl，irrefutablePattern=TuplePattern，其中两个 VarPattern.exprs 分别引用 Decl#16/#17；initializer=Expr#70。此声明不是 LetPatternDestructor 表达式。后接 MatchExpr#72，包含 selector 和两个 MatchCase，case 的 guard=0 占位及模式、正文都须保留。predicate Decl#18.body=Expr#81，MatchExpr#83.matchMode=false、operands=`[84,90]`，为两个 MatchCaseOther，没有 selector。源码 `_` 的最终图还出现 WildcardExpr，不能套用有 selector 的 MatchCase 布局。

## exceptionsResources.cj：catch/finally、资源声明与展开图

SourceResource 实现真实 std.core.Resource 的 isClosed/close 契约，read 返回 7；无外部 I/O。recover 抛出可被 catch 捕获的 Exception，finally 在正常路径和异常路径均执行。withoutFinally 是合法 try/catch 值表达式，两分支分别返回 4、5。withResources 顺序创建 first、second，资源退出顺序为 second、first；catch 处理正文或资源操作产生的 Exception，finally 不改变资源表达式的 Unit 类型。语言规则由 handle 文档及官方 `src/Sema/Desugar/AfterTypeCheck/TryExpr.cpp` 核对。

实际 SDK 图：recover Decl#1.body=Expr#1，其 TryExpr#3.operands=`[4,17,22]`（try, catch, finally），catch pattern 为 ExceptTypePattern，内层 VarPattern 绑定 error 声明。withResources Decl#5.body=Expr#28，外层 TryExpr#30.operands=`[31,146,151]`；正文内部还有完整的异常保存、重新抛出、isClosed/close 调用、生成 MatchExpr 和嵌套 TryExpr。资源 first=Decl#9、second=Decl#13 均保留初始化器（Expr#33/#38）和真实构造函数引用。

官方 AfterTypeCheck 将 resourceSpec 移入局部 Block，并清空 resourceSpec、设置 desugarExpr；因此本批七个 TryExpr 的 TryInfo.resources **实际均为空**。资源语义由展开图中的声明和调用承担。不能为使表面语法与 schema 对称而往 TryInfo.resources 人工填入两个 FullId。本例已经核验资源语法与其完整展开；非空 TryInfo.resources 的直接 SOURCE 写出尚未获得本批 SDK 见证。

withoutFinally Decl#26.body=Expr#157，TryExpr#159.operands=`[160,162,0]`，最后的 0 不能删除。ThrowExpr 必须保留抛出值的单 operand；catch 模式中的声明与正文引用必须共享身份。SourceResource 的 init/read/isClosed/close 在本产物均 body=0，本例不要求通过 SOURCE 导出其普通方法正文。

## 主线程接入与尚未核验

以上是实际 SDK 已核验的源码与图，不是已接通的自动测试。主线程接 harness 时应保持四个源文件为原生合法正例，并对完整正文、类型表、声明表、引用、模式、占位和循环回边制定断言；不得为了本仓当前实现通过而删节点、放宽诊断期望或跳过不支持的子图。证据中的固定 ID 只供定位，必须在同一次导出的对应表内解释。

本批没有独立构造 const/frozen 正文，也没有默认实参 helper 的独立 clone/rebind 或跨包消费运行测试。本批 SDK 未观察到 PrimitiveTypeExpr、IncOrDecExpr、ArrayExpr、PointerExpr、PerformExpr、SpawnExpr、LetPatternDestructor 的 SOURCE 正文节点；它们的 writer 支持应沿 Authority 给出的源码协议实现，不把上述语法的展开结果改成这些节点。MatchCase/MatchCaseOther 和 FuncArgInfo wrapper 已实际观察到。没有为 IfAvailable、Resume 或 autodiff 的 schema-only 项增加任何 encoding 或 fixture。

后续复核可直接读取本目录源文件，无需生成临时源码。等价的实际编译参数为 `cjc <绝对源路径> --output-type=staticlib --output-dir <build下该例目录> -o <例名>.a --dump-ast --diagnostic-format=json`，先执行该固定 SDK 的 envsetup.ps1，工作目录设为同一输出目录。实际解码参数为 `flatc --json --strict-json --defaults-json -o <该例目录> <证据schema绝对路径> -- <CJO绝对路径>`。flatc 对官方 camelCase 字段产生的命名风格 warning 不来自源码编译诊断，不应修改官方 schema。
