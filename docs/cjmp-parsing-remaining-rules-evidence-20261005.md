# CJMP 剩余语法迁移取证：2026-10-05

本记录属于 P0–P10 整体工作，不替代原始计划或验收台账。剩余五个 CFIR PARSE 工厂尚未迁移。
官方依据固定为 `external/cangjie_compiler` 的 `v1.1.3`，通过 `git show` 读取；不能混用当前镜像
工作树行号。独立官方 cjc 1.1.3 的 29 项探针已执行，源码、命令及原始 JSON 保存在
`.workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/`；具体矩阵见
`docs/cjmp-parsing-remaining-tests-20261005.md`。额外 FFI/语义诊断原样保留，不能将探针视为纯语法用例。
manual 的 common / JavaMirror 文档查询未返回相关内容，当前语义结论依据官方源码。

## 官方声明生命周期

`src/Parse/ParseDecl.cpp:69–103`：注解与修饰符 → 声明 handler（完成全部子声明）→
`SetDeclBeginPos`（包含最早修饰符）→ `MPParserImpl::CheckCJMPDecl`。

`src/Parse/ParseCJMPDecl.cpp:142–188`：

1. 两种编译模式均关闭时返回。
2. 自身 generic Frozen、common pattern、CJMP static init 检查失败时返回。
3. 逐个直接成员检查 modifier mismatch，已有失败不阻止后续成员报告。
4. 任意成员 mismatch 使父声明返回，跳过父 default 标记、specific interface、参数和 ctor 检查。
5. 通过后才进行 default 标记及按声明种类分派的检查。

子声明自身有错误不自动使父检查返回。CJMP static init 的父 mismatch 不能无条件跳过。

## 五项规则的原始报告点

| 规则 | v1.1.3 报告点 | 范围及实际条件 |
| --- | --- | --- |
| specific 接口成员实现 | `ParseCJMPDecl.cpp:304–316` | 整个 member；仅 specific interface 且父 modifier 检查通过。HasDefault 首先接受 own SPECIFIC；否则 function 实际 body、property 任一访问器、variable initializer 才接受；其他种类返回 false。 |
| 成员/外层角色一致性 | `ParseCJMPDecl.cpp:279–290,318–321` | 整个 inner 声明或参数；common inner 要求 outer COMMON，specific inner 要求 outer SPECIFIC。包括 nominal/extend 直接成员及单参数列表的函数/主构造器参数。 |
| CJMP static init | `ParseCJMPDecl.cpp:258–260` | 整个 static constructor；own COMMON/SPECIFIC 且 STATIC+CONSTRUCTOR。 |
| common 构造器存在 | `ParseCJMPDecl.cpp:216–237` | class/struct 名字 Identifier；只查 COMMON class/struct 的直接显式 CONSTRUCTOR/PRIMARY_CONSTRUCTOR，static init 也计数，abstract class 不豁免。 |
| explicitly abstract 使用 | 函数 `ParseDecl.cpp:2143–2159`；property `Parser.cpp:431–453` | 函数为整个声明，property 为 lastToken.End() 点范围；class/interface body 后置检查，不经过 CheckCJMPDecl 编译模式门。own ABSTRACT、非 own COMMON、外层非 ABSTRACT+COMMON/SPECIFIC 且非相应 Java mirror provenance 时报告。 |

Node 报告范围来自 `include/cangjie/Basic/DiagnosticEngine.h:917–924` 的 GetBegin/GetEnd，
不能将整声明范围替换成修饰符或名字。property 与 function 的 abstract 报告范围不同。

`CheckCJMPFuncParams`（`ParseCJMPDecl.cpp:293–302`）要求 funcBody 非空且恰好一组参数；
成员参数的角色与构造器自身比较，不与外层 class 比较。普通非 member 参数不接受这些
modifier，不能用它构造纯 mismatch 测试。

## 必须保留的版本差异

已比较 v1.0.0、v1.1.3、v1.2.0-alpha.19、v1.2.0-alpha.20：

- v1.0.0 没有 COMMON/SPECIFIC token 使用或上述 CJMP parser 检查，这是语法演进；不能
  把不同二进制版本的差异说成 v1.1.3 已有 runtime language-version 分支。
- alpha.19 → alpha.20：文件角色允许同文件混合，只要求处于 common/platform 编译；
  common inner 允许 outer COMMON 或 SPECIFIC；删除函数/主构造器参数一致性检查。
- own SPECIFIC 的 HasDefault 豁免、static init 禁止及 ctor presence 算法未在该差异中改变。

原始计划附录 A.2 要求精确预发布身份与顺序。单个 CommonSpecificDeclarations/enabled 布尔
不能表示这些规则，后续 parsing 行为配置必须来自实际版本身份。

取证时 LanguageVersion 只有 1.0.0–1.0.5、1.1.0、1.1.3；本批已补完整精确身份，详见
交接文档“2026-10-05 精确版本身份与 alpha.20 文件角色门的新输入”。
Java/ObjC/ForeignName 和 CommonSpecificDeclarations 的 sinceVersion 是 1.1.0，可按 customized
features 显式关闭。CjmpParsingContext 把 CJMP feature 压为 enabled 布尔，尚未携带具体版本、
FFI builtin 能力；本批新增 alpha.20 混合角色能力投影。不能把目标语言版本与临时 feature override 混成一个语法门。

## Native FFI builtin provenance

`include/cangjie/Parse/Parser.h:49–63` 的 builtin 表包含 JavaMirror、JavaImpl、ObjCMirror。
`src/Parse/ParseAnnotations.cpp:157–185` 在 annotation parsing 中赋身份；只有 ConstSafe
受 moduleName==std 限制。class/interface signature 在 body 之前调用 CheckClassLikeSignature：
JavaMirror 设置 JAVA_MIRROR，带继承时追加 JAVA_MIRROR_SUBTYPE；JavaImpl 设置后者，即使
声明另因约束标记 broken。

显式 abstract 函数的相关豁免是 JAVA_MIRROR/JAVA_MIRROR_SUBTYPE。property 对 JavaMirror
提前返回；ObjCMirror 的 return 位于报告之后，不能统一作此语法错误的豁免。

四个固定 tag 的 annotation 对比进一步确认：v1.0.0 的 builtin 表没有 JavaMirror/JavaImpl/
ObjCMirror；v1.1.3、alpha.19、alpha.20 均有，未见它们转为 ClassId 驱动平台注解的版本。
限定名以第一段判断，interoplib.interop.JavaMirror 为 CUSTOM，不会因末段名或 resolved class
获得 parser mirror 属性；@JavaMirror.someName 的第一段则命中 builtin，不能整体改判 CUSTOM。
@!JavaMirror 不满足该官方 builtin 或 macro start，最终错误须以 cjc 探针确定；不能套用本地
reserved-name 消息。宏定义保留名有 ParseMacro.cpp:177–181 的实际语法报告点。

当前仓库 registry 实际已有这些 builtin descriptors，resolveLanguageBuiltIn 没有排除 platform
名称。parseBuiltAnnotation 的普通 ANNOTATION 节点不是身份丢失证据，CjAnnotation 的
isBuiltInAnnotation 会通过 sourceLanguageBuiltInDescriptor 查询真实语法 registry。
此前据常量清单猜测旧 facade 缺 JavaMirror 的结论撤回：完整读取发现 CjBuiltInAnnotation
已经是 class，byName 由完整 languageBuiltIns/packageDirectives 生成，fromName 可返回该身份。
真实发布延迟在共享 initializeRawAnnotationSyntax：两种 raw builder 均将 kind/origin/identity
置 null，后续 resolveBuiltinAnnotationIdentity 才设置。PSI annotation stub 已有 builtin kind 字段。
ForeignAndAnnotationParsingTest 把裸/限定 JavaMirror 都期待为非 builtin，与当前 lookup
及 v1.1.3 裸名语义矛盾；仅限定形式 CUSTOM 有证据。不能按过时 platform 注释修改 registry。
该断言现已按两者真实身份改为 true/false；这是已取证的预期修正，不证明 FFI parser 链闭合。
定向验证 handle 31768 / queue 0d2f9878 已 exit 0（26s）：ForeignAndAnnotationParsingTest 10/10、
CjmpFunctionParsingTest 12/12，新鲜 XML UTC 2026-10-05T04:49:03.479Z–04:49:03.480Z，全部无失败。
当前 languageBuiltInAnnotationAtCurrentToken 会拒绝第一段后有 DOT 的名称，而官方按第一段
识别 builtin 后报告剩余 DOT；isPlatformAnnotationSurface 又按最后一段平台名保留注解表面。
这两条实际分派与官方不同，不能将裸名身份断言通过扩展成限定名 macro/builtin 全链正确。
迁移需发布真实语法 builtin provenance 和声明签名事实，不能以同短名自定义注解或 CFIR
ClassId 替代。官方 CUSTOM 在 TypeCheckAnnotation.cpp:132–151 的处理不会改成 JAVA_MIRROR。

## 端到端框架缺口

Kotlin `KotlinParsing.java:1295–1315` 提供 marker → own ModifierDetector → declaration
parser → 完成声明的生命周期；没有 CJMP 父子语法规则。CJMP 的必要语法事实是仓颉扩展。

- 实际父容器须在签名阶段记录 own modifiers/name/builtin provenance，在成员全部完成后
  按官方顺序验证；实现/initializer/constructor 来自实际文法分支，不从派生 status 推断。
- 子 parser 要共享正确容器生命周期；quote 中声明不计入宿主成员或构造器。无括号 macro
  carrier 直接调用函数/属性/变量/类/构造器 parser，不能仅改 parseMemberDeclaration。
- 宏声明重解析现在直接解析 fragment text，缺少 native 的真实 scope、primary declaration
  名字及 carrier 自身 modifiers/annotations。迁移应传递这些实际入口事实；不能扩大为父
  COMMON/SPECIFIC/ABSTRACT/mirror 上下文，也不能在替换后补跑 native 未执行的父检查。
- CjAbstractClassBody 的声明/构造器 getter、PsiRawCfirBuilder.convertClassMembers 和
  LightTree 文件声明遍历只识别直接声明。标准整声明 ERROR 包裹会使当前消费者丢声明。
  LightTree findPrimaryConstructorNode 递归扫描 body 且含 FUNC fallback，不能作语法事实来源。
  physical parent 还影响 topLevel/local/member/fqName；DefaultStubBuilder 会继续遍历 ERROR
  内的声明，但其 stub 挂最近 parentStub 并标为 dangling，不能说 stub 全部丢失。
  后续统一实际容器序列仅可穿透 ERROR，不能把函数体、嵌套类或 macro input 提升为成员；
  必须同时覆盖 typed getters、raw 两入口、身份和 stub 往返，不改全局 PSI parent 或消除原生标志。
- Marker.error/errorBefore 使用真实 token 范围；doneBefore 的额外 message 只增加边界零宽
  错误。未找到 Kotlin 为零宽错误附带额外完整声明 range 的对应机制。
- 每实例 element type 即使 register=false 也不能证明完整 IDE 协议正确；heavy/light 错误
  type 比较存在不复用的反证。TokenWrapper 也无法消除 deepEqual 的前置判断，不准作兼容层。
  冻结声明绝对 range 还可能在 IDE 前方插入文本后陈旧；当前整声明范围方案尚未获准。

## 宏重解析的实际边界

`CompileStrategy.cpp:250–258` 在宏执行前解析原始文件，因此原父容器 CJMP 检查已经执行。
`MacroExpansion.cpp:345–379,406–426` 随后执行宏并逆序替换。ReplaceEachMacro 使用
SetCompileOptions、SetPrimaryDecl(invocation.outerDeclIdent)、EnableCustomAnno 和实际 scope
调用 ParseNodes，同时传入 carrier 自身的 modifiers/annotations。

`ParseMacro.cpp:59–123` 恢复 currentFile/curMacroCall，按真实 ScopeKind/ExprKind 选择声明、
参数或表达式入口，只有第一个生成声明获得 carrier 的修饰符/注解。没有伪造父容器模板，
也没有传入父角色、abstract/mirror 属性或完整成员表。ReplaceDeclsMacroExpansion 在解析
完成后才设置 generated.outerDecl 并拼接，未重跑原父 CheckCJMPDecl、specific interface、
ctor presence 或 abstract body 后置检查。完整生成的类会正常检查自身内部成员；直接生成
的成员不能额外套用宿主父检查。生成 static init 的自身检查仍适用。

ParseNodes 的函数/构造器分支调用 CheckConstructorBody(..., inMacro=true)，因此非 common
无体构造器不能凭真实父 JavaMirror 属性获得解析豁免。原宏输入位于 invocation.decl；
carrier 仅复制 ENUM_CONSTRUCTOR，不复制 CONSTRUCTOR/PRIMARY_CONSTRUCTOR。
GetMemberDeclPtrs 返回 carrier，不解包 invocation，因此仅宏输入中的 init 不计为真实直接构造器。
矩阵中的 macro-common-carrier 另有普通 init，不能据其无 ctor 错误证明 carrier 计数。

## 原文件 point 诊断的实际显示范围

四份独立 cjc JSON 确认三种 file-role 错误和前两行空白用例均显示 1:1–1:2。
当前 AST/DTO 仍使用 0..0 的原文件错误节点，并以 ORIGINAL_FILE 区分归属；reporter 显式
显示 native point，不能把 offset 1 映射到空首行之后。INPUT 仍按实际 offset 转换。
验证 handle 44021 / queue fc106896 已 exit 0（2m36s）：parser 12、CLI 23、宏生产 27，
全部 0 failure/error/skipped。新鲜 XML UTC 2026-10-05T04:40:36.020Z–04:41:06.532Z。
独立 gate 审查未发现该修正的实质缺陷。随后补普通生成错误的直接 CLI 断言，覆盖完整宏
调用首尾及 LF/CRLF、PSI/LightTree、STRICT/DEGRADED、裸名/限定名的 16 种组合。
handle 52955 / queue 05b20046 已 exit 0（1m10s），CLI 新鲜 XML 24/24，0 failure/error/skipped，
UTC 2026-10-05T04:47:41.534Z。原文件错误的另一组 16 种组合仍保留并通过。

## 真实语义规则仍需修正，不能随语法工厂删除

- EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS 当前只有注册/消息/生成 API，无报告点；
  官方对应 sema 定义也无 src 触发点。不能保留为 parser 使用限制的重命名工厂。
- COMMON_OPEN_CLASS_NO_INIT 是独立 sema：`CheckCJMP.cpp:484–499` 要求 CLASS、COMMON、
  OPEN/ABSTRACT、无 CONSTRUCTOR 且有 general subclass。当前 CFIR 缺 general subclass、
  abstract-only 判据，并扩大到全部 class-like，不能用它替代 parser ctor presence。
- EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY 是真实 sema（`DeclAttributeChecker.cpp:285–325`），
  覆盖 CJMP class 的 function/property resolved ABSTRACT+真实 body，property 排除 compiler-added
  getter。当前 CFIR 只覆盖 common owner 的 function，仍未完整对位。
- CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER 也是 sema，不能随 parser factories
  删除；需要按非 CJMP、非 ABSTRACT、非 compiler-added/constructor、无 body、非 FROM_COMMON_PART
  等真实条件修正当前 open/status 近似判据。

只读 authority/framework 两项审查已完成；上述完整迁移没有开始写实现，不能闭合问题或增加
总体完成百分比。版本 7 文件错误位置协议已另行通过定向验证，不证明此处剩余框架已完成。

## 三项独立 CJMP 规则的精确历史与 49 个已注册身份

本节逐一读取官方仓库固定 tag 的 `src/Parse/ParseCJMPDecl.cpp`，使用
`git show <tag>:src/Parse/ParseCJMPDecl.cpp`，不以当前工作树代码或版本大小推断历史行为。
本地官方仓库 `git rev-parse --is-shallow-repository` 为 true；以下“首次”仅指本地可见
提交历史中的首次，不能宣称完整上游历史的首次。

### 原始提交证据与非单调边界

- `fc4b011fd673aaab7c5c25944b4b02fb42d276a9`（2025-10-09，feat: add some new features）
  相对 parent `98dfb6e878d87b668b4df16be3449b747194ab74` 新增
  CheckCJMPModifiersBetween、严格 common outer 判据，以及 CheckCJMPFuncParams 定义和
  function/primary constructor 调用。当时平台侧关键字为 PLATFORM，不能因尚无 SPECIFIC
  字符串而认定规则缺失。
- `c872ca3e31a47333ce1431b34fc0ae3ab331785d`（2025-12-27，fix: unexpected error）
  删除 CheckCJMPFuncParams 定义和两个调用，但保留严格 common outer 判据。
  `v1.1.0-alpha.65..v1.1.0-alpha.66` 的文件历史包含该删除；
  固定 alpha66 的完整文件确实没有参数检查。
- `849d728c7eef0a49c9d1a53b9d4f0c8192269e37`
  （作者日期 2025-12-22，feat(cjmp): deprecated annos, assignment for common fields in ctor,
  ctor in classes and structs）重新加入 CheckCJMPFuncParams 定义和两个调用。
  `v1.1.0-alpha.66..v1.1.0-alpha.68` 的文件历史包含该恢复；
  固定 alpha68 的完整文件确实恢复参数检查。这里按 Git 拓扑与 tag 内容判定，
  不能用非单调的作者日期推断先后。
- `acd64576b0653987272f458111475d1321e0fcc0`（2026-02-19）
  将 CJMP 平台侧关键字及诊断名从 platform 改为 specific。
  早期 CheckCJMPModifiers 已有 `currentFile->isCommon/isPlatform` 双向互斥，
  诊断名是 `parse_common_and_platform_in_the_same_file`。
  **更正此前只检索 specific 诊断字符串造成的错误判断：早期 alpha/beta 同样拒绝混合文件，
  不存在“早期无混合文件限制”的源码反例。**关键字重命名没有删除混合文件限制。
- `7a9258ae8e69f49d234f971ca8e7dd117d1c3470`
  （2026-05-25，feat: sync dev to main）直接 parent 为 alpha19 的
  `8154d39188b1ce285557a8e9af244782ad266838`。
  `git log v1.2.0-alpha.19..v1.2.0-alpha.20 -- src/Parse/ParseCJMPDecl.cpp`
  仅列出这项文件变更；alpha20 tag commit 为
  `4a3970486e6ff08ab35b4bf06e1b2e77e248f571`。
  同一提交把 common inner 的判据从
  `!outer.TestAttr(Attribute::COMMON)` 改为
  `!outer.TestAnyAttr(Attribute::COMMON, Attribute::SPECIFIC)`，
  删除 CheckCJMPFuncParams 定义及 function/primary constructor 调用，
  并删除文件 common/specific 双向互斥与分侧编译限制。
- 逐 tag 核对 alpha20/21/22、1.2 beta01/02/03/rc/rc1/rc2、
  1.3 alpha02/03/04/05：以上三项 alpha20 行为均未回退。
  稳定 1.1.0/1.1.1/1.1.2/1.1.3 及 1.2 alpha06/07/08/16/17/18/19
  均保留严格 common outer、参数匹配检查和混合文件拒绝。

### 三个规则的语义与完整身份真值

三个独立行为是：

1. **common-in-specific**：common 直接成员的 outer 是否可以是 specific；
   早期 PLATFORM 在此只作平台侧历史角色对照，不表示早期原样接受 specific 拼写。
2. **param-match**：CheckCJMPDecl 是否对 function/primary constructor 的单个参数列表
   执行 CheckCJMPModifiersBetween。这个真值不取消参数自身其他合法 modifier 限制。
3. **mixed-file**：CJMP 编译模式下同一源文件是否允许 common 和平台侧声明混合。
   无 CJMP 的版本三个值均为 false，绝不能从“没有混合文件诊断工厂”推出允许混合。

alpha20 同一提交改变三个规则，不证明它们是同一个 LanguageFeature。
alpha66 参数检查被删除而另两项保持旧行为，alpha68 又恢复，已经直接证明不能借
mixed-file 布尔替代 param-match，也不能对全部预发布身份使用单调 since 门概括。

下表按当前 LanguageVersion 注册的全部 49 个完整身份列出官方行为；true/false 是上述规则
本身的真值，不代表当前生产实现已迁移完成。1.0.x 不运行 CJMP 规则：
本地固定 v1.0.0/v1.0.2/v1.0.3-beta/v1.0.5 没有 ParseCJMPDecl.cpp 或对应 parser 关键字，
而注册的 1.0.1/1.0.3/1.0.4 没有同名本地 tag，不能伪称逐 tag 考证过这些快照；
其 false 行仅表示本仓库已登记的 1.0.x 无 CJMP 语言表面契约。
早期 1.1 预发布行来自其真实 PLATFORM 源码，不用当前 CommonSpecificDeclarations
的 sinceVersion 覆盖这些历史事实。

| LanguageVersion 完整身份 | common-in-specific | param-match | mixed-file |
|---|---|---|---|
| 1.0.0 | false | false | false |
| 1.0.1 | false | false | false |
| 1.0.2 | false | false | false |
| 1.0.3-beta | false | false | false |
| 1.0.3 | false | false | false |
| 1.0.4 | false | false | false |
| 1.0.5 | false | false | false |
| 1.1.0-alpha.34 | false | true | false |
| 1.1.0-alpha.35 | false | true | false |
| 1.1.0-alpha.36 | false | true | false |
| 1.1.0-alpha.37 | false | true | false |
| 1.1.0-alpha.44 | false | true | false |
| 1.1.0-alpha.55 | false | true | false |
| 1.1.0-alpha.65 | false | true | false |
| 1.1.0-alpha.66 | false | false | false |
| 1.1.0-alpha.68 | false | true | false |
| 1.1.0-alpha.69 | false | true | false |
| 1.1.0-alpha.70 | false | true | false |
| 1.1.0.beta.0 | false | true | false |
| 1.1.0-beta.1 | false | true | false |
| 1.1.0-beta.20 | false | true | false |
| 1.1.0-beta.21 | false | true | false |
| 1.1.0-beta.23 | false | true | false |
| 1.1.0-beta.24 | false | true | false |
| 1.1.0-beta.25 | false | true | false |
| 1.1.0 | false | true | false |
| 1.1.1 | false | true | false |
| 1.1.2 | false | true | false |
| 1.1.3 | false | true | false |
| 1.2.0-alpha.06 | false | true | false |
| 1.2.0-alpha.07 | false | true | false |
| 1.2.0-alpha.08 | false | true | false |
| 1.2.0-alpha.16 | false | true | false |
| 1.2.0-alpha.17 | false | true | false |
| 1.2.0-alpha.18 | false | true | false |
| 1.2.0-alpha.19 | false | true | false |
| 1.2.0-alpha.20 | true | false | true |
| 1.2.0-alpha.21 | true | false | true |
| 1.2.0-alpha.22 | true | false | true |
| 1.2.0-beta.01 | true | false | true |
| 1.2.0-beta.02 | true | false | true |
| 1.2.0-beta.03 | true | false | true |
| 1.2.0-beta.rc | true | false | true |
| 1.2.0-beta.rc1 | true | false | true |
| 1.2.0-beta.rc2 | true | false | true |
| 1.3.0-alpha.02 | true | false | true |
| 1.3.0-alpha.03 | true | false | true |
| 1.3.0-alpha.04 | true | false | true |
| 1.3.0-alpha.05 | true | false | true |

### 完整签名 nested 声明的独立 native 探针

新增独立源码
`.workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/authority-nested-complete-signature.cj`，
未覆盖之前 15 份 malformed probes。官方 SDK 1.1.3 编译（exit 1）的原始输出保存于
同目录 `authority-nested-complete-signature-out/stdout.json`、`stderr.json`。

```cangjie
package remainingparsing

class Outer {
    class Inner<T> <: I where T <: I {}
    func after(): Unit {}
}

interface I {}
class Following {}
```

完整 JSON 只有 `parse_unexpected_declaration_in_scope`，主范围为第 4 行 5–10 列
（右端排除，精确对应 class 关键字）。没有 generic/inherit/where、after、I 或 Following
诊断。该探针可作为完整签名保留测试的原始输入；JSON 不直接证明本仓库 PSI/LightTree
结构保留，typeparameter/supertype/constraint 与后续声明实体仍须分别验证。

## 非泛型 where 的共享语法根契约与非法名字恢复

固定 v1.1.3 的 `ParseDecl.cpp:1296–1303`：ParseConstraints(const Decl&) 在没有
decl.generic 时只调用 DiagUnexpectedWhere(lastToken)，不解析约束。class/interface
共享签名（1342）、enum（1575）、struct（1659）、extend（1764）均由调用方先
Skip(WHERE)，所以此时只消费 where token，后续 token 交给声明体与错误恢复。
不能先创建完整 TYPE_CONSTRAINT_LIST，再把它包进 ERROR 来模拟这条规则。

function 使用 `ParseFuncBody`（2321–2369）→`ParseFuncGenericConstraints`（2390–2401）；
后者同样先 Skip(WHERE)，没有 fb.generic 就只报 DiagUnexpectedWhere。
secondary init（1019）、primary class/struct constructor（1068/1073）也调用 ParseFuncBody，
所以共用同一契约。constructor 的 fb.generic 由其自身 `<...>` 得到，不从 enclosing
class/struct 的 generic 继承。宏 `ParseMacro.cpp:59–123` 的 ParseNodes 调用真实 scope 下
的 ParseDecl，没有独立约束解析机制；宏输入/生成完整声明继续使用上述共同入口。

`DiagnosticParser.def:147–148` 原始诊断为 `parse_unexpected_where`，消息
`unexpected 'where' in non-generic declaration`，主 hint 为 `unexpected 'where' here`；
`ParserDiag.cpp:1283–1297` 主范围取 where token，附加 hint 指向非泛型声明 identifier
（extend 指向关键字）。此函数本身不设置外层声明 HAS_BROKEN。

以下新增独立源码与原始 JSON 均在
`.workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing`，JSON 位于同名
`-out/stderr.json`；没有覆盖旧探针。`authority-where-nongeneric-{func,ctor,primary,macro_func}.cj`
分别使用 `func before() where {}`、`init() where {}`、`Outer() where {}`、
`@Unknown func before() where {}`。官方 1.1.3 各只有 parse_unexpected_where，
精确范围依次为第 4 行 19–24、12–17、13–18、28–33 列，右端排除。
原有 `authority-broken-{class,interface}-unexpected_where.cj` 的 `where {}` 只报告
scope 与 unexpected_where，没有约束 identifier/type 错误。

`authority-where-nongeneric-class_full.cj` 的 `class Inner where T <: I {}` 则报告
scope 4:5–10、unexpected_where 4:17–22、expected_left_brace 4:23–24（T），
以及 expected_decl 6:1–2（外层右花括号）。这是后续真实 body 恢复的结果，不能把
`T <: I` 重新解释为非泛型声明的合法约束或宣称此输入保持完整父子结构。

非法 enum/struct 名字的独立 probes 是 `authority-name-{enum,struct}-{number,string,backtick}.cj`：

| 第 4 行输入 | 官方诊断与主范围（右端排除） |
|---|---|
| enum 123 { A } | scope 5–9；expected_left_brace 10–13 |
| enum "wrong" { A } | scope 5–9；expected_left_brace 10–17 |
| struct 123 {} | scope 5–11；expected_left_brace 12–15 |
| struct "wrong" {} | scope 5–11；expected_left_brace 12–19 |
| enum 后孤立反引号及空格 | 仅 lex_expected_identifier 11–12 |
| struct 后孤立反引号及空格 | 仅 lex_expected_identifier 13–14 |

数字/字符串没有 expected_name：nested scope 已先将声明置 HAS_BROKEN，ExpectIdentifier
跳过重复缺名诊断，非法名字 token 留给真实 body delimiter 检查。以上数字/字符串 JSON
没有 after/Following 诊断，但源码实体保留必须由双入口结构测试验证。孤立反引号导致
词法失败，parser 被截断；其无 scope 不是语法豁免，也不能拿此 JSON 证明后续 AST 保留。
