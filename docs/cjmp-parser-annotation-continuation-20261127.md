# CJMP parser / annotation 实施接续（2026-11-27）

唯一实现树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。全部历史修改、原70项/P0–P10及正确Source/expect保留，H6排除余额保留分母，v10待办不阻断授权工作。以下只记录限定交付及真实窗口，不关闭整个SOURCE/P8/E6/G1或原条目。

## 实际已合入的共用 parser

七文件 v2 已在核对原SHA后由main通过apply_patch合入，`32501734` PSI/frontend测试源码编译成功；`build/r36-assignment-parser-v2-gate.md`限定静态ACCEPT，旧R35两P1拒绝报告保留。ParsedExpression保留真实根/base/children/range，原Macro/Lambda/Let/Assignment调用用途不变；原赋值入口判断leftValue/leftAux，Paren不透传，call参数不影响callee，OptionalChain在赋值/RHS完成后包装。syntaxErrorCount仅记实际错误载体并随marker rollback撤销，HAS_BROKEN与IS_BROKEN不同；完整operator raw组合读取/消费共用同一产品。

`r36-parser-source-20261127-a`只留下before清单与空原流，未创建native队列，存活进程核验为无；`-a-interrupted.json`记录nativeExitCode=null、reportedTestCases=0，旧证据不覆盖，不算行为执行。

真正 r36-b：`build/cjmp-invisible-peer-20261125/r36-parser-source-20261127-b-results.json` / queue a4be440c DONE/exit1，1156项1127通过/29失败、0错误/跳过，Source/SDK前后同SHA，无worker异常/缺suite；124份XML/原流gzip解压核长度/SHA成功。serialization442为441/1（当前Core输入），SOURCE18含原14全通过，Receiver28全通过（含四个正确PSI/LT赋值负例与exact消息/operator范围），完整PSI668为640/28。

28个PSI失败主要是CJMP声明规则及诊断/范围/版本期望，尚未以完整固定源逐项裁定；不能把所有失败直接归因于赋值parser，也不能根据实际结果改正确expect。其中ParserSeed三个缺函数体期望已单独考证并修正，见下节；其他25项继续保留，尚非完整PSI绿色。

## annotation context 与缺函数体期望

固定K `3c7a5041f238102285764d370665f56f587f9dc4` 的withAnnotationContext只维护标志；当前C原helper却无条件切CONSTRUCTOR_HEADER。main恢复flag-only try/finally，constructor/header/default/delegation/body的tower继续由原owner建立/恢复；未填getter、假造RegularContext、删实参或改正确TwoPhase expect。N `14276740ffec1262b81b6b5236b1d840af270b37` scope/const/literal保存与匹配规则独立支持原用例。详细考证/限定ACCEPT：build/r37-constructor-annotation-tower-mapper.md、r37-constructor-annotation-scope-authority.md、r37-annotation-context-gate.md。

ParserSeedCodeFragmentTest仅将旧泛化body消息匹配改为精确`body of function '$name' is missing`。这是固定N DiagnosticParser.def parse_missing_body/ParserDiag.DiagMissingBody的原定义，并经docs及官方SDK1.1.3 probe验证，不靠本仓实际结果反推语言。probe编译器SHA为DFEBED3139AFE00EE642F75322AB82D1D97651A5E9F10EBF4F6D86ABAC061257，不声称来自固定N构建；Source在当前root/tmp，原JSON在build/r36-missing-body-official-probe.txt。SDK分别报告declaration/item/parentValue三原函数名，ParseDeclFile不报缺体的原入口规则仍保留。

编译091ebb09 exit0。r37-a `build/cjmp-invisible-peer-20261125/r37-annotation-source-20261127-a-results.json` / db4cf9e0 DONE/exit1：475项474/1，Source/SDK同SHA，无worker异常/缺suite，53原XML/流逐份解压核SHA。原两个TwoPhase注解用例2/2、ParserSeed13/13、SOURCE18全通过；唯一失败是当前Core定义输入。此窗口只验证两个注解用例，不把之前完整TwoPhase92的剩余14项计通过。

## 新增文法与错误事务矩阵

`build/r36-assignment-parser-tests-v2.patch`（SHA2287E86076FDC4088EA379F81556946BF8C5F887D966F446AA7AE8A606C9AEE1）两个Add File已合入；build/r37-assignment-parser-tests-v2-gate.md限定静态ACCEPT，旧A/B/C缺项拒绝保留。22个@Test覆盖assignment/nonassoc多个operator、optional前/后形状、Tuple producer与最终publication、actual root/scope绑定、参数错误与callee边界、marker rollback和warning/role时钟。

首轮r37-b / cc7a21eb真实464项451/13：Core1及新增诊断测试11项读取未解析PSI的lexicalInput为空、一个SDK marker测试创建了不该存在的后续未完成marker。语言oracle未改：先完成PSI production再读取已发布seed；普通done/error/collapse不创建before boundary，仅before重载拥有该marker。修改编译dda22cf3 exit0。

修正后 `build/cjmp-invisible-peer-20261125/r37-assignment-diagnostics-20261127-c-results.json` / d07bafa5 DONE/exit1、exec75141已返回：464项463/1、0错误/跳过，Source/SDK同SHA，无worker异常/缺suite，49原XML/流解压核长度/SHA成功。两新增class各11项，全22/22通过；完整serialization442为441/1，唯一Core输入失败保留。

当前无行为冻结窗口。后续不要重复旧RUNNING或因句柄缺失重启：先核queue真实终态/native PID。原r36-a启动中断、r37-b装配失败及所有旧报告保留，不能用新绿覆盖历史或拼接重复计数。

## 后续原目标余额

1. 当前Core定义：保持Box/CType/五builtin与correct empty imports，须显式真实当前定义输入/SDK definition投影，不能按同包名把IMPORTED改CURRENT、删除Object父边或合成空Object/Any；body availability及Builtin/CType唯一性按r34报告闭合。
2. 完整PSI其余25项：一文件一文件按固定版本语义/诊断位置裁定；正确expect不改，错误或过时期望只在官方直接证据后修。不得以旧泛化消息/版本矩阵或当前actual冒充语言证据。
3. 完整TwoPhase92剩余14项：导入捕获时变化、initializer/tuple/local正式消费、generic extend和enum constructor身份/配对仍继续原owner实施。原70/版本49/其他阶段与全量CFIR/API/IDE余额按原计划保留，不因本parser或两个annotation通过关闭。
