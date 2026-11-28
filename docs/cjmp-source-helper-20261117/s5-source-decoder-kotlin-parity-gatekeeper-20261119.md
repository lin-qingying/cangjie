# S5 SOURCE decoder：KotlinParityGatekeeper 新审查（2026-11-19）

审查结论：**REJECT 当前候选的“完整 canonical SOURCE 图恢复”门；确认它实际推进了 native 载荷恢复。** 这不是因 Library.body 仍为 null 而拒绝。SOURCE graph 是独立能力，保留 null body、不给 DFA/CFG 就绪资格是正确边界；完整 S5/S6、R5 normal 分析仍 OPEN。

审查对象为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie` 的未提交工作文件，HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`。已按本轮用户更新重读 Try.resources / Spawn.future 的 CURRENT ownedTarget 修订；旧“只借用 target 丢 initializer”的 finding 已撤销。以下判断来自新读取的源码，不依赖旧会话。仅新增本报告，未运行 Gradle/cjc，未修改生产、测试或原正确期望。

## 依据、版本和审查时点

先读 [decoder continuation](../cjmp-source-decoder-continuation-20261119.md)、[native SOURCE 权威合同](s5-astloader-source-native-authority-20261119.md)、[Kotlin mapper](s5-kotlin-framework-mapper-20261119.md)，应用 kotlin-cangjie-frontend-parity 技能的框架、身份和发布核对规则；本任务显式只读，不执行技能中的实现/测试步骤。

固定对象通过主树 external 的 `git show SHA:path` 读取，无 checkout：
- native `14276740ffec1262b81b6b5236b1d840af270b37`：ExprLoader.cpp、ReferenceLoader.cpp、ASTLoader.cpp、ExprWriter.cpp、ASTWriter.cpp、AST/Utils.cpp、ModuleFormat.fbs。
- Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`：FirMemberDeserializer.kt、IrSymbolDeserializer.kt、IrBodyDeserializer.kt、ValueWithPostCompute.kt。

固定 Kotlin 的 FIR metadata function（FirMemberDeserializer:649–747）恢复库签名；完整语句/循环正文对位在 IR reader（IrBodyDeserializer:122–168、793–823）。CJO 的 30+3 SOURCE AST 不是 Kotlin FIR metadata 的同格式产品。新增 decoder/loader/constant reader 分解可以视为仓颉 d/e/t/辅助记录所需的格式实现；本报告不把它们称为 Kotlin 同名文件原样复制，也不因这一格式差异单独拒绝。

代码坐标以下以本工作树文件为准。简称：
- D = [CfirCanonicalSourceDecoder.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirCanonicalSourceDecoder.kt)
- L = [CfirSourceGraphLoader.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirSourceGraphLoader.kt)
- C = [CfirSourceConstantDecoder.kt](../../build/cjmp-owned-closure-20261120/documentation-link-repair-map.md#missing-source-constant-decoder)（原链接路径：`../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirSourceConstantDecoder.kt`；历史源码原字节缺失）
- DC = [CfirDeserializationContext.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeserializationContext.kt)
- DD = [CfirDeclDeserializer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt)
- P = [AbstractCfirDeserializedSymbolProvider.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/AbstractCfirDeserializedSymbolProvider.kt)
- G = [CfirSourceGraph.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt)
- T = [CjoSourceGraphConsumptionAssertions.kt](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CjoSourceGraphConsumptionAssertions.kt)

## Findings：生产候选

### F1 [P1] 全表函数预加载绕过实际 SOURCE owner，局部函数先变成非局部声明

坐标：**L:29–35、43–50；DD:191–203、209–218、1757、1828–1831；CjoFullIdResolver:279–298。**

L 对所有 FuncDecl/body!=0 调 deserializeCanonicalDeclaration，未先恢复该 d 的 Block/Pattern/FuncBody 拥有域。parent 索引只覆盖 nominal members、own generic、FuncDecl params，没有 Block 中的 local FuncDecl、lambda FuncBody 或 helper 边。没有 parent 的 local function 因而走 DD:199 的普通 deserializeDecl；sourceDeclarationOwner 未设置，DD:1757 给出 isLocal=false，DD:1831 给出 package CallableId。稍后 Block 真正遇到该 local d 时，L:46 直接返回缓存对象，不能再通过 source owner 建立正确身份。

影响：一个保存在外层函数 Block 中的 local FuncDecl，即使 e 正文与 Return 最终都存在，也已拥有错误的 locality/callable 域；同名局部函数不会因 graph 中 d identity 正确就恢复实际 owner。lambda 中 local function 同样受此预扫描影响。

执行修复：以真实 top-level 声明为 root，先遍历选中拥有域、登记 local/function/lambda/helper 身份和 own TP，再补 references。全表扫描只能作索引/校验，不能把所有非零 FuncDecl 当创建入口。已登记 d 的获取须验证原 owner，不以缓存命中免除 owner 合同。定向验收从含两个不同 FuncBody 的同名 local functions 开始，检查 isLocal、CallableId、TP/param owner、Return target及重复请求身份。

固定依据：native ASTLoader:343–386、ExprLoader:361–380 沿 top-level→拥有 Block/d 建声明；Kotlin IrBodyDeserializer:125–130、157–168 从 statement 恢复 declaration；IrSymbolDeserializer:30–79 分离 file-local identity 与 public 路由。

### F2 [P1] 发布范围遗漏 top-level / nominal field initializer 等拥有 roots

坐标：**L:24–37；D:52–53、96–120、124–127；G:336–408。**

L 第一遍只恢复 top-level header，随后仅将非零 FuncDecl body 交给 D。D 的 initializer/default/declarationPattern 注册仅在其私有 declaration() 被调用时执行；该入口来自 Block、Pattern、参数、helper 和 ownedTarget，没有将 top-level VarDecl、nominal body 的 VarDecl/VarWithPatternDecl 作为 SOURCE 内容 roots 登记。

影响：同一包存在选中方法正文及独立非零字段 initializer 时，graph 可以 finish 并进入 completedSourceGraph，但字段 initializer 的 e、其 lambda 和 pattern 不在 graph 中。forSourceGraph 的 DD:887–888 关闭旧 field initializer reader，进一步不能把 normal header 中的 initializer 当完整恢复证据。G.finish 只证明“已经 reserve 的节点全部填好”，没有证明“选中 SOURCE 所有拥有 roots 均已 reserve”。

执行修复：把实际选中的声明内容 roots（函数、变量 initializer、参数 default、pattern、annotation owner 内容等）纳入一个可核对的 root plan；以 owning closure 而非 allExprs 无条件填槽恢复。完成门对 plan 的每个 root 做覆盖校验。未选中内容保留明确能力事实。验收用方法正文之外的全局/成员 lambda initializer，并核对 graph.initializers、lambda body、所有 d/e/t 的同代身份；不能补造 body0 或把非零内容改为空。

### F3 [P1] VarWithPattern normal binding 与 SOURCE graph / FullId 建立两份 symbol

坐标：**DD:2205–2212、2269–2284、2374–2419；D:109–113、345–347；L:43–50。**

DD.deserializePatternBindingVariable 从实际 binding d 创建 CfirPatternBindingSymbol / CfirPatternBindingVariableImpl，并记录 provenance，但 DD:2417–2419 只 publishDeserializedDeclaration，没有将此 binding 登记到 context.declarations。D 再恢复同一个 VarPattern.exprs[0] 时，L 找不到该 d，于是普通 VarDecl converter 创建另一个 CfirFieldVariableSymbol / CfirFieldVariable（DD:2013–2063）。

影响：CfirPatternVariable.pattern 中的 binding、graph.declarationPatterns 中的声明槽、后续 CURRENT FullId 可以具有相同 provenance/index 却是不同对象/symbol。仅检查 graph 里的 d 重复引用相同，不能证明 header 与 graph 共享 binder。这违反“一份实际 binding”，也把原 binding 角色降成 field-variable 壳。

执行修复：binding d 的创建、登记与引用统一到 canonical declaration reservation；normal pattern 和 SOURCE pattern 都引用同一个真实 binding，不在 pattern reader 直接另建 symbol。验收比较 normal pattern.bindingVariable.symbol、graph VarPattern slot.declaration.symbol、initializer/ref target，必须 assertSame，并保留其真实 binding kind。

固定依据：native ExprLoader:577–584 拥有单一 VarDecl，设置同一 parentPattern；Kotlin IrSymbolDeserializer:37–54 的声明入口与 61–79 的引用入口共享 identity map。

### F4 [P1] context 原子提交未覆盖 provider 缓存，失败时 staging 声明可提前外泄

坐标：**C:57–69；L:22–37；DC:99–143；ToSymbolUtils.kt:60–66；P:92–111、276–284。**

SOURCE transaction 内 C:64 使用 type.toClassLikeSymbol(session)。ToSymbolUtils:64 转入正式 provider 查询。P.loadDirectTopLevelClassSymbols:282 在可重入的同一 context materialize 中可返回当前 staging 声明；P:94–97 / 104–111 随即写入 classCache/classSymbolsCache。这些 provider 缓存和 lookupTag.boundSymbol 不在 DC.CompletedGraph 中；DC.catch/finally 只恢复 generic 状态、清理本 context staging，没有撤回这些发布。

具体失败路径：已先加载入口接口 header → SOURCE 请求第一遍在 staging 创建另一个 class C → 某 Pattern.values composite 首次查询 C 的 classifier，provider 缓存了 staging C → 后续 e kind/必需槽材料化失败 → context 没发布 C，但公开 provider 仍持有 C 的 symbol。重试又可以在 context 中创建另一个 C，两个正式入口不再 canonical。这里不需要假定非法 package import cycle；单包、第一次 classifier 查询和可控后置失败就能覆盖。

执行修复：identity 获取与 completed 对象查询分别定义；正式 provider cache / tag 绑定只接已完成声明，或纳入同一个发布事务。内部 reader 使用 transaction-owned canonical binder，不能调用会发布结果的公用 getter取得 staging owner。失败注入测试必须同时查询 context、provider、tag与重复请求身份；不能只断言 completedSourceGraph==null。

固定依据：Kotlin ValueWithPostCompute:55–81、101–125 只让计算线程内部见 postCompute 对象，公开 getValueIfComputed 不返回它；本地 CompletedGraph 的 volatile 提交满足单 context 容器门，但不能替代整个 provider 的观察门。

另一个未验收原子边界：DC:99 持本包锁时，L:58/DC:235–237 可进入另一包的 materialize；没有 registry 层跨 context 锁序/闭包调度。本轮不声称已运行并证明死锁，也不把 Kotlin per-key cache 当作任意跨包互等安全的证明。

### F5 [P1] composite 常量的 classifier 又按 ClassId 选首个 parent，丢弃实际 FullId 选择

坐标：**C:60–69；CfirTypeDeserializer.kt:154–166；ToSymbolUtils.kt:60–66；P:92–111；CfirDeserializedSymbolProvider.kt:181–203。**

普通 Reference target 经 FullId/selected path 路由是正确进展。但 C.composite 先用 wire.type 得 Cone，再以 session 的 ClassId 查询 declaration。class type reader 只保留 ClassId 的 ConeClassLikeLookupTagImpl，没有把 FullId 选择的实际 snapshot classifier 绑定到该 tag。P:94 对多个同名 classifier 仍取 firstOrNull。

影响：两个 parent 快照有同包同 ClassId，requestingModule 真实 dependency/refinement 计划选择第二个输入时，composite 字段可以从第一个 parent 取 symbol。C:67 按名字 single 匹配只能选择“错误 owner 里的唯一字段”，无法恢复正确 parent；字段同名时可静默错绑，字段不同则错误失败。对象 shellmemo 保留 allValues 身份并不能修复 classifier 身份。

执行修复：constant object owner 从实际 SemaTy.CompositeTyInfo.FullId 和 selected snapshot 的 canonical declaration binder取得；保留该精确 classifier 与字段 symbol。不能在 constant reader按首个 parent 找 ClassId，也不能从字段拼名字生成替身。验收两个同包同 classifier/field-name parent，断言对象字段 symbol、moduleData 与 provenance 全部是计划选中的输入，交换候选顺序结果不变。

这也是原“Alias/nominal 精确 classifier 使用视图仍 OPEN”在新 constant consumer 中的实际调用，不因类型数量完整就视为已关闭。

### F6 [P1] importedPosition 未使用请求 context 的 snapshot plan

坐标：**L:64–68；DC:177–179；CjoSourcePositionResolver.kt:25–29；CjoManager.kt:64–65。**

位置 decode 把 manager 直接传给 CjoSourcePositionResolver。Position.pkgId>0 时 resolver 使用 loadPackageSnapshot(packageName)，manager 实际实现为 loadPackageSnapshots(...).firstOrNull()。这没有经过 DC.importedSnapshot / CfirDeserializedSymbolProvider.selectImportedSnapshot 的 dependency/refinement 选择。

影响：相同 import package 的两个 parent/file tables 不同时，FullId target 可以来自已选第二快照，expression/pattern 的 begin/end 却来自第一个快照文件。新增 ImportedPosition 保留了行列，却不能证明位置的来源身份正确。

执行修复：保留 Position 当前/外包不同 file 基数合同，让位置解析使用本 context 的精确输入选择；不要借用 FullId 的 -1/-2/-3 转换。新增跨包位置、多 parent 顺序交换与同 raw file index 的断言；合法缺失位置仍保持缺失，不造 offset 或锚点。

### F7 [P2] named function 的 native header 完成后被公开 API 丢弃

坐标：**D:31–36、130–156；G:62–68、103–110、233–247；P:466–474；CfirImportedSourceGraphProvider.kt:8–10。**

D 为每个函数记录 parameterLists、对齐 desugars、retType 的存在/0、captureKind、always；这些 named headers 只保存在 CfirDeserializedSourceGraph.functionHeaders。正式 sourceGraphOf 最终只返回 loader.load().graph。G 没有 named functionHeaders map，只有 Lambda.function.importedHeader 承载这些字段；named function 的 decodeFunction 返回产品也未保存。

影响：公开调用只能拿 named function 的 flattened normal 参数列表和 body identity，无法读取原参数分组/完整 desugars 序列、native retType 0 与 signature 返回类型的区别、captureKind/always。内部 headers “曾经读过”不等于正式 SOURCE 产品保存了这些语义字段。D:102–103 还跳过 body0 function 的 header/default内容，不可拿“body不可用”概括其它已保存字段。

执行修复：在 SOURCE 公共产品中以同一 CfirFunction identity 暴露完整 imported header，并明确 header-only 与 body 能力；不要新增另一套 reader。normal helper 元数据与 graph 的对齐 helper 列表应共享同一声明，helper 的 ownerFunc/outer owner不能混成一个 sourceDeclarationOwner。验收两个参数列表、零 helper 槽、retType=0但签名有真实返回类型的输入，按公开 API 读取，不直接访问 internal wrapper。

固定依据：native ASTLoader:763–791、794–834；特别是 826 的 helper.ownerFunc 与外层 nominal owner 为不同关系。

### F8 [P2] LitConst 的四值 StringKind 被压为 JavaString 布尔

坐标：**D:197–201；G:137；固定 native ModuleFormat:398–403、ExprWriter:233–243、ExprLoader:141–151。**

wire 保存 Normal/JString/MultiLine/MultiLineRaw，D 只记录 strKind==JString。后三种非 JString 输入无法在正式 graph 中区分；字符串 kind 是固定 native writer/loader实际保存恢复的字段。本轮无其它字段承载它。

执行修复：SOURCE literal 产品保留正式四值 string-kind合同并完整映射；未知值明确失败。新增对应 typed graph 字段断言。原 SOURCE canonical 去 Paren/desugar的合同不是删除已保存 stringKind 的依据。

### F9 [P2] MatchCase pattern 没有恢复 selector ctxExpr 非拥有关系

坐标：**D:272–277、297–302、329–353；G:89–100、188–191、203–207；固定 native ReferenceLoader:732–742。**

decoder 独立填 Match 与 MatchCase，CfirSourcePattern 没有 contextExpression 字段/关系索引。native refs 阶段会令 MatchCase 每个根 pattern.ctxExpr 指向同一个 Match.selector。G 当前只保存 Match拥有 selector/cases的结构，没有恢复这一非拥有关系或执行其一致性校验。

影响：SOURCE consumer 取得独立 MatchCase/pattern 后没有完成的 selector context；存在 case identity 重用时也没有“一个拥有者/一个 selector”校验。可以从外部搜索整张 Match 图推导关系，不等于提供已完成 native pattern 产品。

执行修复：在 pattern 或 graph 的 canonical关系表保存实际 selector identity，post-reference阶段填回并校验；selector 回边不得作为 owning edge重新构造。验收两个 match 的同型 patterns，比较每个 ctx identity 与自己的 selector对象，保留 guard/body/type/runtime flags/const values及位置。

## Findings：新增验收断言

### F10 [P1] 捕获 pattern 的正确前缀/尾槽合同与新增测试断言冲突

坐标：**T:536–570，特别是 550–551；D:341–351。**

Except/Command 的当前生产修订正确地让 types 只保留首个 semantic type，typeReferences 保存 types[1..]。T:550 却无条件断言 wire.typesLength==actual.types.size，再到 567–569 单独检查尾槽。因此任何至少一个捕获 Type 尾槽的真实输入，都先在 550 因 n+1 != 1 失败。这是新加入断言没有按角色分槽，不能据其失败回退生产 decoder 的正确分离。

执行修复：新增测试按固定 native 槽合同独立断言一个 own semantic type +有序 typedRef tails；原正确 .cj、handler数量、非零正文、pattern/FullId/loop期望继续保持。T 的 pattern断言还没检查 importedPosition/constantValues，不能用当前遍历声称验证了 Pattern.values 和原行列。新增值/位置的独立断言，并覆盖对象共享回边，不从本仓 decoder 输出反推期望。

## 全 33 roles 逐字段核对

此表仅记源码覆盖，不计行为通过。公共 e type / overflow / begin/end 已进入 D:59–74；map 被保留，Jump 独立转入 loop。G.finish 有 kind/Block/Argument/loop存在校验；它没有验证所有拥有 roots、canonical d owner、跨 provider发布，故不能由“33分支齐”授予完整图门。

| # | native role | 当前 D 坐标 | 已读载荷 / 尚欠合同 |
| --- | --- | --- | --- |
| 1 | Wildcard | 167 | 无 operands；保留 type |
| 2 | PrimitiveType | 168 | 无 operands；保留 type；只有标记，非新造默认类型 |
| 3 | Return | 169–172 | value + function；普通/lambda function传入；owner与实际FuncBody合同仍受 F1/F2影响 |
| 4 | Jump | 174–177 | isBreak + map槽loop；G:353–357校验loop角色 |
| 5 | MemberAccess | 180–182、316–326 | base/target/instTys/matchedParent；精确 FullId target，类型 classifier使用仍有 F5 |
| 6 | Ref | 179、316–326 | 名称与target独立；同上 |
| 7 | Call | 184–187 | base/FuncArg序列/kind/sideEffect；resolvedFunction未单列，已恢复base target可供后续专用产品消费；不宣称post-reference normal Call完成 |
| 8 | Unary | 189–191 | operand/op |
| 9 | IncOrDec | 193–195 | operand/op；未删除该正式 writer role |
| 10 | LitConst | 197–201 | literal kind/text；四值 stringKind丢失（F8）；尚未产 native初始化后的literal常量值产品 |
| 11 | Binary | 203–206 | left/right/op |
| 12 | Subscript | 207–209 | base/非空 indices/isTupleAccess |
| 13 | Assign | 211–214 | left/right/op/isCompound |
| 14 | ArrayExpr | 216–221 | FuncArg/initFunc/isValueArray；native非VArray init可空；G没有独立校验Array args为FuncArg |
| 15 | Pointer | 223–225 | optional FuncArg；没有native post-reference从arg.expr.ty回填的专门完成步骤 |
| 16 | TypeConv | 227 | operand/结果type |
| 17 | Throw | 228 | value/type |
| 18 | Perform | 229 | value/type；不以此证明R5 effect/DFA |
| 19 | Spawn | 230–234、124–127 | optional arg/current owned future d+内容；本轮修订接受；不臆造task |
| 20 | ArrayLit | 216–220 | elements/initFunc；native非VArray必须FuncDecl的校验未接，VArray跳过init的语义未单列 |
| 21 | TupleLit | 236 | 有序elements/type |
| 22 | If | 237–239 | cond/then/固定else0；G检验then Block |
| 23 | Try | 241–249、124–127 | try/catches/finally0/patterns/current owned resources+内容；旧owned拒绝撤销 |
| 24 | While | 251–254 | cond/body/普通map |
| 25 | DoWhile | 251–254 | 保留 wire body/cond顺序并投影到Loop |
| 26 | Lambda | 256–261 | FuncBody/参数/body.type/supportMock；匿名Function为Library、body null；native外层owner未作为公开关系保存 |
| 27 | Block | 263–270 | ordered mixed d/e、末尾UNSAFE；local owner预扫描问题F1 |
| 28 | Match | 272–277 | matchMode经selector存在表示；cases角色由G核验；ctx回边缺失F9 |
| 29 | LetPatternDestructor | 279–283 | initializer +内联patterns |
| 30 | ForIn | 285–295 | iterable/guard0/body/pattern/kind；G非DIRECT校验iterable Block |
| 31 | FuncArg | 305–310 | InvalidExpr+FuncArgInfo角色/isDefaultVal/withInout/0或1 operand/自身type；没有defaultUnit补造 |
| 32 | MatchCase | 297–302 | patterns/guard0/body/record.type；pattern selector ctx缺失F9 |
| 33 | MatchCaseOther | 304 | condition/body/record.type |

Pattern附加核对：D:329–353 保存kind、有序子pattern、Var d/Const Enum e角色、runtime flags、own semantic type、Except/Command typedRef tails、ImportedPosition及constantValues。类型尾槽使用 L:70–73 的 resolved SEMANTIC ref，符合 native从已保存 SemaTy做WrapType的方向，不声称还能重建wire未保存的原Type词法位置。Pattern自己的原行列已读，跨包来源仍 F6。Const raw union和本loader内 allValues object shellmemo确已接入（C:18–71）；固定 native SaveConstPattern:720–737 没有 add_values，不能凭本轮新增values reader声称固定native writer已填非空pattern常量向量。

Decl/helper附加核对：参数列表顺序和零desugars保留；defaultVal没有用helper.body代替；lambda建独立anonymous Function和真实参数，返回指向它；不造CFIR body。缺口为F1/F3/F7，以及 sourceDeclarationOwner单值不能等同native lexical owner/outerDecl/ownerFunc/FuncBody四种关系。DD.deserializeSourceLambda:225–249 的 owner输入本身未存入任何公共owner关系；其参数defaults元数据先以null helper创建（DD:216–217），后续graph读desugars没有回填该normal元数据。本轮不以未见真实lambda default输入断言已触发失败，保留专门验收项。

Late/borrowed附加核对：e work queue允许先reserve后fill；当前 CURRENT target在L:57调用声明创建入口，不是只读取已登记identity。没有真实owner索引的晚到local target仍可能以引用方owner先创建，因此完整owning barrier仍未闭合。G.finish不拒绝同一e被同owner的多个owning父节点拥有，也不验证Return.function确实属于该root；不能把“所有边目标在records内”称为native拥有域唯一性的证明。现有正确的重复wire槽必须保留，不能以去重operands作修复。

## 实际 consumer / R5 / 验证状态

新测试接线是实质进展：[CfirSourceExpressionExportTest.kt:401–417](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirSourceExpressionExportTest.kt)（原审查坐标 L401）用公开 frontend实际SAVE_CJO；T:61–79为fresh consumer再跑公开frontend；T:86从consumer.symbolProvider查库接口，T:102–117从真实Library session组件请求SOURCE graph；T:118–127检查header参数/类型和snapshot身份不被替换。它不是test-only decoder工厂。

但测试 consumer源码仅package/import（T:61、85），SOURCE请求发生在公开frontend executePhase完成之后（T:76→103），由断言代码发起。当前生产 `sourceGraphOf/importedSourceGraphProvider` 调用搜索只见组件定义、provider实现与该测试，没有BODY/DFA/CFG/LL正常分析消费的调用。普通入口注册见 [CfirDefaultSessionFactory.kt:150–158](../../cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirDefaultSessionFactory.kt)（原审查坐标 L150）；DD:1766 / 1889 / 1907 / 1997以及anonymous lambda:239仍body=null。

因此：
- 可以承认“fresh公开frontend装配出的库session有按需SOURCE graph能力”；行为通过仍待真实test终态。
- 不能据此接受R5 B10 normal consumer或整项S6。新配置未设置实际common-part输入/refinement family或LL source消费，只复制cjmpMode和classpath（T:64–74）；多parent/common/depends-on/LL闭包仍须各自证据。
- 不能为R5门直接把graph注入Library.body、提升phase或制造空CFG。应完成有明确能力的正式normal/source consumer链，实际owner context、typed声明和DFA/CFG完成产品按原R5批准合同提交；其实现任务与本轮格式decoder评审分别记录。

2026-11-19T07:16:51Z读取 `.gradle/queue/tasks/2bc4253c.json` 时为 RUNNING。报告落盘后的末次只读复查已为 **DONE / exitCode=0**，startedAt=2026-11-19T07:09:28.401Z，finishedAt=2026-11-19T07:19:37.244Z；任务为 :cfir:analysis-tests:compileTestKotlin，非行为测试。所审生产/测试文件的指纹复查全部与下表一致。本报告没有启动/排队/中止Gradle。测试编译成功不计行为通过：没有本轮33roles/freshCJO行为终态，不复用旧类型图73项或旧SOURCE成绩替代。

## 本次候选文件指纹

以下SHA256取于2026-11-19T07:16:51Z（UTC）。本报告只覆盖这些读取内容；后续生产/测试改动须重审相应finding。

| 文件 | SHA256 |
| --- | --- |
| D | 64298294DBDBD386149347CDA2DD043215B44197666727C08732754F2F0546A4 |
| L | D72DBFD6FA74712B7C8E0C6ACBDB9ADF96098AB722C3B69431E8C57C121B40DB |
| C | 7CAEAC7CEC50EB17BF3D969AA2821345581501BF1B92154DE593AFA1CAD48DA6 |
| DC | 77EEDAAD384D2BE88B410642CCE2A9CF2DC337385108EC53B8A6A8C8399BC69E |
| DD | 09A1246CE02C830BE1885132341F8BFA19B017A8E2FC4067C2A44C72D3356662 |
| P | 710674E8040E07A08808C127CBC41E3BA85B652068C161BA4D2DBA5BAEA5DD86 |
| CfirDeserializedSymbolProvider | 76216CFD7C65FBC456CE33DF67A5329506AC94A04809CA144B419800249A8AEA |
| G | 61100FFAAF12F07EB82F79C3C4CF8F22156832918ACE1854873D18BB1D4A339D |
| T | 662608CDE1766B11EBA36989F26AED68BD86EEAC5DAC1C72F4C6CDC01E09A303 |

