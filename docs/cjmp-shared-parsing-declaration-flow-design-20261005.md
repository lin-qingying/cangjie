# CJMP 剩余五项语法诊断：声明生命周期设计取证

本文件为只读实现复核后的设计输入，不表示迁移已经完成。依据为固定官方
`external/cangjie_compiler` 的 `v1.1.3`（使用 `git show`），以及当前工作树
`CangJieParsing.kt`、`CangJieExpressionParsing.kt`、`CfirCjmpParseRulesChecker.kt`。
没有修改生产代码、测试或执行 Gradle，没有重跑已有 native probes。
完整历史版本和独立探针仍见 `cjmp-parsing-remaining-rules-evidence-20261005.md`。

## 两层检查时机必须分别保留

固定 `ParseDecl.cpp:69–103` 的普通 handler 分支在完成声明之后，先
`SetDeclBeginPos`，再 `CheckCJMPDecl`。主构造器不走该 handler 收尾：
`ParsePrimaryConstructor:1043–1107` 在设置 annotations/end 之后自行调用
`CheckCJMPDecl`，随后才报告返回类型非法。不能因此重复检查主构造器。

class/interface 的成员又多一层父容器处理：
`ParseClassBody:1121–1175` / `ParseInterfaceBody:1177–1212` 每次 `ParseDecl`
返回后立即设置 `SetMemberParentInheritableDecl`，然后调用函数或 property 的
abstractness 检查，再插入父 body 的 decls。该检查不是父 body 结束后统一执行。

因此应保持下面的时序（名称是设计术语，不是已经存在的生产 API）：

1. 声明入口建立 own frame；保留注解及原始修饰符，不在公共入口统一发布有效属性。
2. 声明 handler 在自己的原生时点执行 modifier projection，完成真实签名、body、参数及其自身直接成员。
3. 仅原生调用 `CheckCJMPDecl` 的 handler 路径执行 own frame 的同序检查；不能给提前返回路径新增收尾检查。
4. 返回实际 class/interface 成员循环；针对该直接函数/property 完成 abstractness 检查。
5. 将真实声明事实加入父容器直接成员集合。
6. 父声明 handler 完成后，父 own frame 执行自己的第 3 步。

不把孩子诊断失败自动传播为父失败。父子角色一致性属于第 6 步，不能提前在孩子
检查中报告并据此跳过父后续规则。static init 的自身失败也不免除父对它的 mismatch。

### 2026-10-06 续接：handler 时点和重复 occurrence

再次固定读取 v1.1.3 后确认，projection 不能统一放在声明头：普通 FUNC 在签名前
（ParseDecl.cpp:2023）；secondary 在 ParseFuncBody 后（1019→1021）；primary 在签名/body 后
（1068/1073→1078）；finalizer 为 972→973；MAIN 为 1979→2002；普通 VAR 在 ParseTypeAndExpr 后
（283→284）。因此 primary member 参数自己的 projection 可以先于 primary 发布角色。
共享框架应提供 handler 显式调用的投影操作，不能移动这些实际调用时点。

raw 可保存全部 modifier occurrence，但投影只消费每种 modifier 的首次 occurrence，再按源码位置排序：
ParseModifiers:1918–1926 对重复 modifier 报错后不插入 set，Node.h:510–513 的 comparator 只按 kind。
不能让重复 occurrence 重复进入 collision/warning 遍历。

固定源码全部 `CheckCJMPDecl` 调用点为普通 LookupDeclHandler 分支（ParseDecl.cpp:102）、
primary 自调（1098）和 get/set 自调（411）。enum constructor、macro carrier、finalizer、
const-without-let 的提前 return 不经过通用收尾。它们仍需真实声明事实，但没有额外 CJMP 检查调用。

## 语法事实的所有权

当前 `parseMemberDeclaration` 有 `decl` marker 与 own `ModifierDetector`；
`parseClassBody` / `parseMembers` 只传 `tokenId` 和父 detector，没有实际成员事实返回值。
应由真实文法分支产出同一声明事实，成员循环收集直接结果；禁止遍历函数体、嵌套类、
quote 或 macro input 来补齐它们。

必要的 own facts 是声明文法种类、实际名字及名字边界、声明起止边界、原始修饰符、
合法发布属性、own generic、真实 builtin annotation provenance、实际函数 body、
property getter/setter、变量 initializer、直接 constructor 标志，以及参数列表和参数事实。
它们必须来自解析中的实际分支，不来自 CFIR resolved status、隐式构造器或 synthetic getter。
文法种类与诊断展示名称分离：后者进入本地化资源，不在分派分支写英文字符串。

原始修饰符与合法属性必须是两个事实。官方 abstractness 使用
`HasModifier(member.modifiers, ABSTRACT)` 和 `outer.modifiers` 的 ABSTRACT / COMMON /
SPECIFIC，但 member 自身 COMMON 和 Java mirror 使用已发布 Attribute。
当前 `checkCjmpFileRole` 会 `ModifierDetector.clear()`；这个 detector 不能同时充当
被清空的属性集合与永不丢失的显式修饰符记录。原始语法表面保留不是重新发布非法属性。

### 2026-10-06 固定源码复核：声明种类与 scope 的前置分支

独立 authority 再读固定 `ParseModifiers.cpp`、`ParserModifierRules.cpp` 及宏入口确认：
不能只将 detector.clear 改成清空另一个集合，仍在所有入口提前检查文件角色。
`CheckDeclModifiers` 的顺序是 UNKNOWN_SCOPE 直接映射 raw modifiers；无该 DefKind/scope
rules 返回空；无 modifiers 时 class/interface scope 发布 IN_CLASSLIKE；空 rules 报第一个
非法 modifier 并返回空；随后才检查 CJMP 文件角色并调用 GetModifierAttrs。
最后一步中非法/冲突 modifier 仍会映射 Attribute，不能自行筛成“无错误 modifier”。

尤其 FINALIZER 只在 CLASS_BODY 登记 EMPTY rules，common/specific finalizer 应先非法
modifier 返回空，不能先发布文件角色错误。interface/enum/extend secondary 及无对应 scope
rules 的 primary 也不能沿统一提前 check 路径报告。最终 handler attrs 是自己按顺序发布的
intrinsic/预置 attrs 加本次 modifier projection，返回空 projection 不清除已有 attrs。

应迁移的真实入口包括 top/member/no-parens macro input 三处提前 file-role 检查，局部声明、
foreign function 合成 FOREIGN、带已解析 detector 的 function 试探分支，以及 finalizer
直接 shared signature 入口。试探回滚不能残留属性或文件角色状态；quote 隔离继续生效。
macro input own detector 与宿主 carrier detector 分离；ENUM_CONSTRUCTOR 的提前分支不能
无条件先做文件角色检查；括号输入不发布声明属性。

函数 operator 解析、COMMON/SPECIFIC 返回类型要求及 COMMON 缺体、common pattern/static
自身 gate 使用有效 attrs。文件角色检查读取 raw COMMON/SPECIFIC；explicit abstract 同时
读取 raw member/outer 与有效 COMMON/mirror。primary 参数 detector.count、CONST/UNSAFE
原始文法用途保持 raw 判据。不得在 shared body helper 用 effectiveCommon || rawCommon
修补 secondary，而要保留其 handler 预发布 COMMON 的真实时序。

### 2026-10-06 完整 modifier projection 与实际 handler 例外

独立 authority 固定读取 `ParserModifierRules.cpp:390–445,531–570`、
`ParseModifiers.cpp:50–137` 及实际 handlers。未修改代码/运行构建或重跑 probes。
实现须直接复核这些固定源码表，不以当前 detector 或 CFIR status 代替真值。

| DefKind | 已登记 scope → rule table（table 名实际带 `_MODIFIERS`） |
|---|---|
| FUNC | TOPLEVEL、CLASS_BODY、INTERFACE_BODY、STRUCT_BODY、EXTEND_BODY、FUNC_BODY、ENUM_BODY 各自 FUNCDECL |
| MAIN / MACRO | TOPLEVEL → TOPLEVEL_MAINDECL / TOPLEVEL_MACRODECL |
| CLASS / INTERFACE / ENUM / STRUCT | TOPLEVEL → 对应 TOPLEVEL 类型表 |
| VARIABLE | TOPLEVEL/CLASS_BODY/STRUCT_BODY → 对应 VARIABLE；FUNC_BODY/MACRO_BODY → FUNC_BODY_VARIABLE；PRIMARY_CONSTRUCTOR_BODY_FOR_CLASS/STRUCT → AGGREGATE_BODY_INSTANCE_INIT |
| EXTEND / TYPE | TOPLEVEL → EMPTY / TOPLEVEL_COMMON |
| CONSTRUCTOR / PRIMARY_CONSTRUCTOR | CLASS_BODY/STRUCT_BODY → AGGREGATE_BODY_INIT / AGGREGATE_BODY_INSTANCE_INIT |
| PROPERTY | CLASS_BODY/INTERFACE_BODY/STRUCT_BODY/EXTEND_BODY/ENUM_BODY → 对应 PROP |
| FINALIZER / PACKAGE | CLASS_BODY → EMPTY / TOPLEVEL → PACKAGE |

省略组合是无 rules，不是 EMPTY；UNKNOWN_SCOPE 先于查表直接映射 modifiers。
STATIC/PUBLIC/PRIVATE/INTERNAL/PROTECTED/OVERRIDE/REDEF/ABSTRACT/SEALED/OPEN/FOREIGN/
MUT/UNSAFE/OPERATOR/COMMON/SPECIFIC 映射同名 Attribute；CONST/INOUT 均不映射。
CONST 属于 handler 原始语法事实；INOUT 映射数组中的 STATIC placeholder 不是真实映射，
前置 INOUT 分支已返回 null。

rule collision 是“当前 modifier 对此前已遍历 allowed modifiers”的方向性集合，不能
改成统一无向冲突组。illegal modifier 报告后仍映射 attrs，但不加入 traversed；
collision 新旧两项仍映射 attrs，仅从 warning-valid 集合移除。warning table 是另一个
协议：TOPLEVEL CLASS/INTERFACE 的 PUBLIC/OPEN、INTERFACE FUNC/PROP 的 PUBLIC/OPEN
分别按固定 priority/ILLEGAL 无条件 redundancy 处理，不混入 collision 或 scope error。
raw header 必须保存顺序、位置及重复 occurrence；去重 kind 查询仅供真实语法分支使用。
当前 ModifierDetector 丢弃已被 lexer 消费的 INTERNAL/OVERRIDE，也没有位置/重复记录。
INOUT 尚不在一般 modifier tokens 中，不能带着这些遗漏迁移完整框架。

实际 handler 的专用发布不能由通用表覆盖：

* EXTEND 虽登记 TOPLEVEL EMPTY，ParseExtendDecl 不调用 CheckDeclModifiers。恰一个
  raw COMMON/SPECIFIC 时检查文件角色，但忽略 false 仍发布该 role；其他非空 modifier
  使用 ExpectNoModifier。不能将 extend 改走 EMPTY projection 而丢掉角色。
* MACRO DefKind 是 macro definition，另发布 MACRO_FUNC；macro carrier 不走该 projection，
  不复制 input constructor/common attrs。前置 modifier 传给 input ParseDecl，carrier 只在
  原生 enum 条件成立时发布 ENUM_CONSTRUCTOR。
* enum constructor 不走普通 projection，非空 modifier 使用 ExpectNoModifier；
  ParseCaseBody 后继承父 SPECIFIC，否则 COMMON，与 child raw modifiers 分开。
* value parameter 没有通用 PARAM DefKind。非 member 参数使用 ExpectNoModifier，
  不执行 CJMP；primary let/var member 参数才用 VARIABLE×对应 primary class/struct scope。
  raw CONST 单独报告后仍 projection；空 raw modifiers 根本不进入该参数 projection。
* main 只从 modifier projection 发布 UNSAFE，不统一追加所有 attrs。
* secondary 预发布 CONSTRUCTOR/raw COMMON 后追加 projection；primary 签名后发布
  IN_CLASSLIKE 或 IN_STRUCT、CONSTRUCTOR，再追加 projection，raw CONST/UNSAFE 仍各有文法用途。
  finalizer 先发布 FINALIZER，再追加 projection，COMMON 不豁免缺体。

实际 scope context 须由文法进入/退出，不从祖先、SyntaxOnly 或 raw builder猜测：
文件 TOPLEVEL；名义 body 分清 class/interface/struct/extend/enum；primary member参数分清
class/struct primary scope；普通函数/secondary/finalizer/main/accessor 的块内声明进入
FUNC_BODY；macro definition 直接 body 为 MACRO_BODY，内部普通块仍按 FUNC_BODY。
foreign 函数继承真实外层声明 scope，不能新增 FOREIGN_SCOPE。宏 input 继承真实接入 scope，
但与 carrier 属性 owner 分开；UNKNOWN 仅用于原生真正忽略 scope 的 libast 调用。
DeclarationParsingMode.MEMBER 不能统一映射 CLASS_BODY，MAIN_BODY/PROP_MEMBER 参数模式
不能被用作块内声明 scope。copy、by-clause parser、local/quote试探回滚都须保留正确 owner。

### 2026-10-06 scope owner 的实际接入与 lazy 重解析门槛

独立 Kotlin mapper 将同层接口定位到 `AbstractCangJieParsing.kt` 的不可变
`ParsingContext`，不是一个已存在的独立 ParsingContext.kt。Kotlin 的对位是实际声明入口
独立 detector、body 明确进入、局部试探成功后 close/失败 rollback 的调用层级；Kotlin
没有原生仓颉 DefKind×Scope/CJMP/MACRO_BODY projection，不能声称这些表是 Kotlin 原有 API。

实际接入点为普通 `parseFile/parseOnlyAnnotationFile` 的 TOPLEVEL；
`parseClassBody→parseMembers→parseMemberDeclaration` 按 keyword 区分名义 scope；
`parseEnumBody` 使用 ENUM_BODY；`parseFunctionBody/main/get/set`、
`parseInitFuncRest→parseInitFunctionBody→parseInitFunctionBlock` 内部声明进入 FUNC_BODY；
`parseMacro` 的直接 body 为 MACRO_BODY；`parsePrimaryInitFuncValueParameterList→parseValueParameter`
只对实际 let/var member 参数进入对应 class/struct primary scope。
`parseForeignBody→parseForeignFunction` 保留真实 outer scope；macro 无括号 input继承接入scope，
local declaration 文法许可与 lexical scope分开，quote/括号 token input不发布宿主属性。

新增不可变 scope 会被现有 `copy(...)` 保留，但三个 file entry 当前重新建立
DEFAULT/ANNOTATION_ONLY：普通完整文件应显式 TOPLEVEL，`parseMacroExpansionFile` 必须接收
生成重解析的实际接入 scope，同时不能承接宿主父属性/member frame。
`CangJieParsing.create/createForByClause`、expression parser create 及 truncated builder
必须沿同一调用 context，不能给新 parser 统一默认 TOPLEVEL。

另有不能遗漏的 lazy chameleon 路径：`BlockExpressionElementType.parseContents` 调用
`CangJieParser.parseBlockExpression`，当前只传 moduleName 并以 DEFAULT 重建解析。
宏定义直接 body 的 MACRO_BODY 必须由原始 parser 保存真实重解析事实并传入该调用；
内层普通块仍是 FUNC_BODY。不能最近 PSI 祖先推断或把所有 lazy body 一律改为 FUNC_BODY。
这项事实保存/失效协议尚未设计实现，不能仅 eager parser 成功就声明 scope owner 完成。

2026-10-06 独立 mapper 续查 Kotlin `BlockExpressionElementType.kt:41`、
`LambdaExpressionElementType.java:50`、`KotlinParsing.java:1979–2000` 和
`KotlinExpressionParsing.java:1135`：它们只有 chameleon 文本建 builder、对应 parser
入口和 marker collapse，没有可直接照搬的 scope/module/options 快照协议。
仓颉 `parseBlock:501`、case body `:1543`、`parseFunctionLiteral:2488` 也未发布事实。
候选边界是 original parser 成功完成节点、`CangJieParser.parse` 的 treeBuilt、真实
LazyParseablePsiElement、element type 的 parseContents/isParsable；但 marker 到节点的
metadata 接口与局部 replacement 传递/失效接口仍待 SDK 取证，不能宣称已有支持。
只在 heavy AST 添加 user data 不完整：light 文件同次事实与嵌套 lazy 解析也必须一致。
快照不能含宿主 member frame/attrs；完整重解析和宿主文法种类、module、sourceKind、
options 变化必须淘汰旧事实。需要真实编辑 gate，覆盖初次 heavy/light、首次 lazy 展开、
正文局部编辑和 macro 宿主改成普通函数，验证 MACRO_BODY/FUNC_BODY 不被旧事实混淆。

SDK 253.29346.379 gate 续查证实 marker/StartMarker 无 user data 发布接口，
`PsiBuilderImpl.collapseLeaves` 只创建 type/start/end 对应节点及 SDK 自有 lazy token 数据。
局部 replacement 的准确 hook 是 `IReparseableElementTypeBase.isValidReparse(oldNode,newNode)`：
`BlockSupportImpl.tryReparseNode` 在 isReparseable、createNode、DummyHolder 接入之后、
merge/lazy 展开之前调用它，允许从确切 old node 向确切 new node 显式传递已校验事实。
原有 isParsable 只有 parent，不能替代这个节点对。
但 `PsiBuilderImpl.buildTree` 遇 originalTree 会 merge 并抛 ReparsedSuccessfullyException，
不返回 AST；所以单纯 treeBuilt 返回后遍历发布会漏掉增量 merge 路径。
LazyParseableElement.ensureParsed 也不自动继承调用方 metadata。
完整初次绑定、light-tree merge 的复用/新增/替换及上下文失效协议仍未闭合；
只证明上述 old→new hook 可用，不能宣称 lazy scope 方案已完成。

`parseDeclaration/parseLocalDeclaration` 与 quote macro 试探会 rollback。scope 本身不可变
不会泄漏，但 raw/effective attrs、直接成员 facts 和文件角色变更必须与真实 marker 成功
同步 commit，回滚不能保留试探事实。新增 shared lifecycle 必须覆盖这些实际入口。

### 文件角色失败时的属性与构造器例外

补读固定 `ParseModifiers.cpp:50–82`：正常已登记 scope rules 下，
`CheckCJMPModifiers` 失败直接返回整个空 attrs 集合；不是只删除 COMMON/SPECIFIC。
UNKNOWN_SCOPE 在此检查之前从所有 modifiers 直接映射 attrs；无 scope rules/空 rules 又
有各自提前返回。因此不能把文件角色检查统一提前到所有文法入口，假定它等价于原生
`CheckDeclModifiers` 的实际位置。普通声明清空有效 modifier attrs 的方向有源码依据，
但原始 modifiers 从不被删除。

secondary init 是已证实的例外。`ParseDecl.cpp:1000–1042` 在解析签名前根据原始
`HasModifier(COMMON)` 预先 `EnableAttr(COMMON)`，并无 STATIC 同类预发布；随后调用
`CheckDeclModifiers` 得到空 attrs 并不清除此前 COMMON。所以文件角色失败的 common
secondary init 仍保留 own COMMON 属性及缺体豁免；如果 STATIC 也失去有效属性，则不能
仍以原始 STATIC 触发 CJMP static init 规则。当前提前 clear detector 后
`parseInitFunc(... isCommon = detector.isCommonDetected)` 不等价。

primary constructor 没有上述提前 COMMON，COMMON 来自 CheckDeclModifiers，失败后不
保留该豁免。function 同样先发布 CheckDeclModifiers 的 attrs，再检查 COMMON/SPECIFIC
缺返回类型，故该处应使用有效属性。不能为修 secondary init 把所有 body exemption
改用 raw COMMON，也不能以统一“清空所有属性”抹除文法分支预发布的内在属性。

`ParserImpl.cpp:281–301` CheckConstructorBody 在 macro / 非 class-struct scope 缺体且
非有效 COMMON 时报告；普通 class/struct secondary constructor 稍后由成员循环
CheckInitCtorDeclBody 再查 COMMON/JAVA_MIRROR/OBJ_C_MIRROR。这又是两个有不同父事实
可见性的时机，不宜只保留一个通用 isCommon 参数模拟全部检查。

最直接的设计结论是：header 保存 raw modifiers；有效 attrs 是文法 handler 按原生顺序
发布的独立集合，文件角色失败使当前 CheckDeclModifiers 返回空结果，不能反向清除
已经由该 handler 发布的 intrinsic/预置 attrs。class/interface abstract 后置检查读取
raw outer modifiers 与有效 mirror/member COMMON 的各自真实事实。

父 class/interface 签名在进入 body 前确定自己的修饰符、identifier 和 Java mirror
provenance。官方 FFI signature 提前发布 JAVA_MIRROR / JAVA_MIRROR_SUBTYPE；后者包括
JavaImpl，也包括带继承的 JavaMirror。限定名规则取实际 builtin 语法身份，不以末段名、
ClassId 或注解解析后的推断替代。

父 frame 嵌套须有严格进入/退出边界；回滚不能把尝试分支产生的成员或诊断事实留在
真实父集合中。suppressed diagnostics 不得改变文法节点消费、真实成员顺序或 frame 退出。

## CheckCJMPDecl 的同序控制流

固定 `ParseCJMPDecl.cpp:142–188,240–277` 的次序必须成为同一个 own frame 的收尾流程：

1. 两种编译模式均关闭时返回。
2. 对 own COMMON/SPECIFIC，依次处理 generic Frozen、COMMON pattern 声明、static constructor。
   任一前置检查失败立即返回；common pattern 和 static 的先后不能颠倒。
3. 遍历全部直接成员执行 mismatch。每个 mismatch 可以失败，但必须继续检查后续成员。
4. 任一 mismatch 失败即返回，跳过该父 default 发布、specific interface、参数、ctor presence。
5. 发布 own/common 容器的 COMMON_WITH_DEFAULT 语法事实，按原生规则使用已完成的子事实。
6. 按声明文法种类互斥分派：interface 检查实现；primary constructor / function 检查参数；
   COMMON class / struct 检查构造器存在。

generic Frozen 前置实际报告的是 `sema_common_generic_frozen_not_supported`，不能把它
改造成 PARSE 工厂或丢掉它导致后续语法诊断增加。迁移需要明确这项源语法 gate fact 与
现有语义报告流水线的职责：保持自身后续语法短路，语义诊断仍由语义通道报告，不能让
CFIR 回头计算剩余五项语法规则。该桥接尚未实现，不能声称删除 checker 即完成对齐。

历史差异由已有 LanguageFeature 默认/override owner 投影为独立能力：common-in-specific、
参数匹配及 mixed-file 不是一个布尔值。default 与显式 override 的优先级不变。
固定 1.1.3 common inner 要求 outer COMMON；alpha20 以后才接受 outer SPECIFIC。

## 五项报告的实际落点

| 规则 | own/frame 检查点 | 范围与条件 |
|---|---|---|
| static init | own 前置，父 mismatch 之前 | 整条真实 static constructor；own 有效 COMMON 优先于 SPECIFIC。 |
| mismatch | 父 own 前置检查通过后，全部直接成员；单参数列表另按 func/primary 自身收尾 | 整条 inner 或参数；参数的 outer 是该函数/主构造器，不能借 enclosing class 角色。COMMON 判据按独立版本能力。 |
| specific interface 实现 | 该 interface 自身所有 mismatch 通过后 | 整个 member；own SPECIFIC 先接受，否则仅 FUNC 的真实 body、PROP 的任一实际访问器、VAR 的 initializer；其他种类返回 false。 |
| common ctor presence | COMMON class/struct 自身收尾通过后 | identifier 精确范围；直接显式 CONSTRUCTOR 或 PRIMARY_CONSTRUCTOR，static init 计数；abstract class 不豁免；宏 carrier 不解包 input ctor。 |
| explicit abstract | 真实 class/interface 成员循环，member 已完成，进入下一成员之前 | function 整条成员；property 该成员刚解析后的 `lastToken.End()` 点；own ABSTRACT、非 own COMMON，且父原始修饰符不满足 ABSTRACT+COMMON/SPECIFIC，且无相应 Java mirror provenance。 |

explicit abstract 不经 CheckCJMPDecl 编译模式开关。JavaMirror 对 property 的提前返回在
报告之前；JavaMirrorSubtype 也免该报告；ObjCMirror 的 return 在报告之后，不能免该报告。
函数 body 存在也不是这条使用限制的豁免，因为检查先于 body 存在的 return。
primary constructor ASTKind 不是 FUNC_DECL，因此不应套普通函数 abstractness 分支。
struct/enum/extend 的成员循环不能凭“最近类容器”扩大成 class/interface 后置规则。

## 宏输入与生成声明

当前无括号宏输入直接调用 `parseFunction` / `parseProperty` / `parseVariable` /
`parseClass` / `parseInitFunc` / `parsePrimaryInitFunc`；仅增加
`parseMemberDeclaration` 收尾会漏掉这些入口。它们应进入共享 own 声明生命周期，
但 input fact 归 macro input，不能成为宿主直接成员。括号和 quote 输入同样不得计数。

原始直接宏 carrier 才是父 member；原生 GetMemberDeclPtrs 不解包 invocation.decl。
carrier 仅复制 ENUM_CONSTRUCTOR，不复制 CONSTRUCTOR/PRIMARY_CONSTRUCTOR。
因此 `@M init()` 输入解析有 ctor fact，但不能满足宿主 common ctor presence。

native ReplaceEachMacro 的生成 reparse 只传实际 scope、primary declaration 名、编译选项、
carrier 自身 modifiers/annotations；第一个生成声明承接 carrier 表面。它不传父角色、
ABSTRACT/mirror 属性或父成员表。解析后才设置 generated.outerDecl，且不重跑原父检查。
故生成完整 class 正常检查自己的成员；直接生成函数/property 不执行宿主成员后置
abstractness，也不补父 mismatch/specific interface/ctor presence。自身 static 检查仍适用。
不能让 ambient parent frame 自动覆盖这一入口边界；需要明示“输入归属/完整生成容器/
直接生成声明”的事实所有权，不能根据 PSI 祖先猜测。

## 范围发布仍是独立未闭合项

五项多数要求整声明范围，不能以 zero-width 或 modifier/name 范围替代。将整个声明
标记为 ERROR 会影响现有直接声明 getters、raw 身份与 stub dangling；仅“测试读得到消息”
不能证明框架完整。当前方案必须先证明 heavy/light 统一、重解析有效、源范围不陈旧的
报告载体，并验证真实声明结构仍存在。

ordinary builder.error 的零宽点会受 installed IntelliJ whitespace balancing 影响。
property 的 native lastToken.End() 不能简单等同当前 offset（它可能跳过后续空白），
也不能用父右花括号或 next member 起点代替。应捕获真实成员最后 token 边界，并按
该 point 的发布协议验证 heavy/light/CLI/macro 同一结果。

尚未解决范围载体时可以实现被验证的声明事实生命周期，但不能删除 CFIR 工厂后
用错误范围宣布完整迁移，也不能保留 CFIR 语法报告作为兜底。

## 当前 checker 不可作为迁移真值

实读 `CfirCjmpParseRulesChecker`：有 own CJMP 修饰符才进入多数检查；实现检查在
mismatch/static 之前；普通父容器的带 CJMP 子成员仍由最近类型查找；static 排除父
mismatch；HasDefault 其他种类返回 true 且缺 own SPECIFIC 优先分支；COMMON ctor
范围为 declaration.source；abstract 用 resolved status/JavaMirror 近似。这些与上述固定
源码不一致。迁移应替换它的语法职责，不能逐函数搬进 parser 后保留同一错序算法。

CFIR 中独立语义规则 COMMON_OPEN_CLASS_NO_INIT、EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY、
CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER 的修正仍属原任务，不能因这五项
parser 工厂删除而一并删除或重命名冒充语法迁移。
