# CJMP alias 与赋值文法接续（2026-11-27）

唯一实现树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10保持完整；H6排除余额保留分母，v10待办不阻断已授权工作。全部历史未提交修改、正确Source/expect及失败证据保留，局部通过不关闭S1–S7/P8/E6/G1或原条目。

## 当前实际完整窗口

`build/cjmp-invisible-peer-20261125/r33-full-source-formal-20261127-b-results.json`：queue d0aa1d09 DONE/exit1，exec27076已返回，实际646项、624通过、22失败、0错误/跳过。三份frontend编译class有92/28/26个JUnit方法，本轮XML完整报告同样数量，共146；nativeWorkerFailure=false、selectedSuitesReported=true、missingSelectedXml为空。54份原XML及两原流均已逐份gzip解压核长度/SHA，共56份验证成功。9847输入、SDK84和显式2GiB frontend资源脚本前后同SHA。

serialization436为434通过/2失败，owned-copy/Published46、SOURCE18（含原14）、finally26均全通过；Receiver28为24通过/4正确赋值语法负例失败；TwoPhase92为76通过/16失败。五个初始化正例、缺Any/lazy负例及Primary两个原testkey已通过。serialization余项是std.core当前定义输入不足导致self-import，以及新raw matrix的三String断言被JUnit3解释成message/expected/actual（原wire已通过此前metadata比较，尚未跑完全部循环）。该断言现已限定kotlin.test.assertEquals，期望空字符串保持。

r30-d原stderr也有worker堆耗尽，其505/466/39是已报告计数；r32-a为544/522/22并缺finally26。两窗口原JSON不改写，不再用native执行结束的complete=true证明完整矩阵。r33-b才证明本轮三个frontend suite完整执行，不能据此宣布其16失败已修。

## 实际 alias/引用修订与新验证

decoder保留原boundTarget和CallKind，特殊Func引用仅允许真实Constructor/NamedFunction、具有实际参数列表事实的EnumConstructor、或真实变量的已完成函数值类型；Package/Absent/ClassLike/非函数值目标明确拒绝，普通name/非Func this/super合同不改变。变量Alias只查询FixedSymbol、绑定Alias与已完成RHS，并复用原参数替换器；不通过provider、lazy/infer补输入。

`build/r34-special-reference-actual-gate.md` 曾拒仅按symbol判断循环。当前已分离原RHS定义依赖DFS与每次use-site参数替换：定义依赖只查完成RHS中FixedAlias，直接/间接及参数增长的定义循环仍硬拒；Identity<Identity<Func>>的有限实参嵌套不因重复symbol拒绝。

四个原引用资格测试和两项新增泛型alias测试已应用；新增测试在consumer前核allTypes结构DAG、TP/Generic/Alias保存槽，真正定义环先成功发表header/RHS再检查专用错误。Nested Identity覆盖深度1/2/3、两种特殊spelling及两种header顺序，保留变量、symbol/typeRef、Return/body及graph alias身份。原正确期望不变。编译22e7740a已exit0；当前behavior `r35-alias-source-20261127-c` / exec53250正在冻结窗口执行完整serialization、复制和SOURCE，行为终态待核。新静态gate为build/r35-function-alias-definition-gate.md，状态须读报告，不用旧gate授通过。

## 赋值文法的下一个实际补丁

R35-c已终态：queue f677948b DONE/exit1、exec53250返回，506项504通过/2失败、0错误/跳过，Source/SDK前后同SHA，53份原XML/流解压核验成功。完整serialization442为440/2，SOURCE18/copy46全通过；R35泛型嵌套与真实定义环、R34联合资格/普通Absent及alias值回归均通过。余二为当前Core定义输入和新增builtin this测试的错误模型断言（将手写CfirThisReceiverExpression当成通用CfirQualifiedAccessExpression）。按实际tree模型把该新断言改为专用this receiver，保持原this/symbol/type/body/alias期望；queue3ab49518原XML确认定向13/13及文档检查exit0。该定向窗口无独立全输入事前manifest，原XML/逐case/gzip/SHA见build/cjmp-invisible-peer-20261125/r35-source-public-20261127-d-results.json，不能替代完整模块或原70验收。build/r35-function-alias-definition-gate.md已限定静态ACCEPT并关闭R34-alias-cycle；旧REJECT不覆写。

公共parser候选已实际落盘：build/r35-assignment-parser-implementation.patch（61258字节）及同名.md；含七个生产/资源目标，尚未合入/编译/运行。生成worker已因rate limit停止，不把报告当编译证明；独立审查目标build/r35-assignment-parser-candidate-gate.md待核。必须先对照当前七文件SHA、原N完整leftValue/leftAux/broken/optional/tuple协议及operation token范围核对候选，再main合入并修真实编译/审查问题。不能盲目应用、声称四负例已通过或丢弃正确expect。当前无活动Gradle；后续从该候选审查/实施继续，当前Core定义和TwoPhase16余额不关闭。

固定N与K报告为build/r34-assignment-lhs-parser-authority.md、build/r34-assignment-lhs-parser-mapper.md。四个原负例的operator主范围和消息正确；错误来自共同assignment parser的leftValue/leftAux分类，不能改成CFIR checker错误或删Source。Paren/If不能因member后缀被升级为合法base，Call只递归baseFunc、不把argument/RHS中的if误判；Tuple、optional、首operator/非结合及已有broken按Native分别表达。`++/--`有独立规则，不能套本案诊断。

当前仅准备build/r35-assignment-parser-implementation.patch候选（实际存在/完成度须核磁盘）；actual parser源码尚未合入该候选。候选必须保留ParsedExpression的Macro/Lambda/Let/Assignment用途和范围/broken，真正syntax错误事件随marker rollback撤销，warning/预留role诊断不能污染broken；两parser使用同一文法和正常publication，不增加文件名/测试特判。完整准备后由main核对、合入、编译及原四负例/合法call-base回归验证。

## 当前Core输入及其余原余额

Parser独立gate已REJECT旧候选，两项P1分别为将子树错误升级成当前根IS_BROKEN、selector call参数错误污染之前的member/callee。main已立即修订新候选build/r35-assignment-parser-v2.patch：只传播HAS_BROKEN并保留实际根IS_BROKEN；在selector名称完成后、参数开始前用原返回产品冻结member，Call使用该原base，参数错误不回写base。旧候选/gate保持，新v2仍未应用/编译/执行，复审目标build/r35-assignment-parser-v2-gate.md。下一读取v2与复审，完成实际合入及两parser原负例/合法base/错误归属矩阵；不能因13项alias通过结束parser实现或原70目标。

build/r34-stdcore-self-reference-authority.md证明Native按IMPORTED事实选择import，而非包名相同即CURRENT；不能删Object父边或合成空Object/Any。保持Box/CType/五builtin与正确empty imports，须补真实当前Core定义产品/显式SDK snapshot定义投影，完整处理原d/F/type/owner、唯一Builtin/CType及引用重映射。对应落点见build/r34-same-package-definition-mapper.md；此项未实现，不把正常BODY通过当作当前Core归属证明。

TwoPhase16原失败涉及导入快照捕获期间变化、constructor-header tower上下文、正式CJO initializer/tuple/local消费、generic extend成员/配对及enum constructor身份。应按真实逐case ledger在原生产owner修复，全部正确期待保留；SOURCE14或alias局部通过不替这些正式消费与原70/P0–P10验收。
