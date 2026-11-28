# SOURCE call：This 应用签名与调用结果的角色 gate（2026-11-19）

范围按用户最新纠正：当前 orderedAliasRoles 失败为 `FrozenTypesGraph.nominal` L408 / `assertAliasRoles` L331 的 `isThisTy`，期望 true、实际 false；撤销先前把当前失败归因于 instTys 的猜测。文件名沿用已指定的 source-call-instantiation-role-gate，内容聚焦真实 This 应用签名根因。

唯一写入本报告。未改生产、原 fixture、测试、期望或构建文件，未运行 Gradle/cjc，未联系旧代理或停止会话。

## 1. 结论与已核实的失败身份

**根因是完成的 applied-reference 把 call 结果类型当成 callee 的返回签名。** 当前 writer L505 将 `resultType` 传给 `resolvedReferenceFor`；后者 L1603 传给 `buildAppliedCallableReference`；factory 原样存入 `substitutedReturnType`。SOURCE `appliedSignature` L434–436 又把这个字段当作 callee Func.return。

对 `value.asThis()`，call 结果按 native GetCallTy 正确归一为普通 `Carrier<Int64,Bool>`。这个值的 This 标记 false；将它写进 applied-reference 的 callee 返回字段后，MemberAccess 的 Func.return 也变成 false。**归一化调用结果本身是正确职责，应修复输入角色混用，保留 call 的 nominal 类型。**

本轮读取现有两个 XML：

- `cfir/analysis-tests/build/test-results/test/TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirSourceFrozenTypesTest.xml`。
- 同目录 `TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirSourceFrozenTypesLightTreeTest.xml`。

orderedAliasRoles 的完整 failure stack 均到 `FrozenTypesGraph.nominal(...:408) → assertAliasRoles(...:331)`，expected true / actual false。读取现有结果不等于重新运行测试；联合39579531的111/83/28计数、Tree25/S040/C-extra正例通过是用户提供的最新运行事实，本轮未重算其运行状态。

一方读取根 W：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，存在未提交生产修改；行号对应读取的工作文件。主仓库及 external 只读。

Kotlin 固定为 `3c7a5041f238102285764d370665f56f587f9dc4`，native 固定为 `14276740ffec1262b81b6b5236b1d840af270b37`；引用源码均用对应仓库的 `git show 提交:路径` 读取。

## 2. 正确 fixture 的四个独立产品

原 `cfir/analysis-tests/testData/diagnostics2/common-specific/source-frozen-types/orderedAliasRoles.cj` 中：

```text
Carrier<T,U>.asThis(): This
carrierUse(value: Carrier<Int64,Bool>) {
    return value.asThis()
}
```

| 产品 | 应有内容 |
| --- | --- |
| 声明 asThis 的函数签名 | `() → ClassThis(Carrier,[Carrier::T,Carrier::U])`。 |
| MemberAccess 的完成 callee 签名 | `() → ClassThis(Carrier,[Int64,Bool])`；该返回的 `isThisTy=true`。 |
| Call 自身结果类型 | `Class(Carrier,[Int64,Bool])`；`isThisTy=false`。 |
| MemberAccess ReferenceInfo | target=asThis，函数自身 instTys=[]；matchedParentTy 保留 native 的非零 Invalid 角色。 |

class 的 `[Int64,Bool]` 是 receiver/owner 替换及完成返回类型内部的槽，不是 asThis 自身函数泛型实参。This flag、nominal arguments、reference instTys、matchedParentTy 也不是可以互相推导的同一个字段。

当前测试 L329 验证 MemberAccess 引用与空实参，L330 验证 receiver reference，L331 验证 callee Func.return 的 This flag，L332 验证 Call 为 concrete nominal。XML 停在 L331，不能把已经执行到的 earlier reference assertions 改称当前失败。

## 3. 官方归档的独立解码证据

本轮在内存中解压已有 `docs/cjmp-source-helper-20261117/frozen-type-roles-native-evidence-20261118.json` 的 orderedAliasRoles CJO 归档，按固定 ModuleFormat FlatBuffers 槽读取 MemberAccess、FuncTyInfo、CompositeTyInfo 和关联 Call。没有写临时产物、没有使用本库 writer 产生期望。

归档 SHA-256 解压后验证匹配：`5BA49A5DE228E98195C52878F029DB0B5261978D04EFD090676415E9354A54A9`。

| native 归档直接读取 | 结果 |
| --- | --- |
| asThis MemberAccess Expr index | 23 |
| MemberAccess.type（callee Func type index） | 13 |
| 该 FuncTyInfo.retType | 14 |
| type 14 的 CompositeTyInfo.isThisTy | true |
| MemberAccess.ReferenceInfo.instTys | [] |
| 以 Expr23 为 callee 的 Call.type | 12 |
| type 12 的 CompositeTyInfo.isThisTy | false |

这些编号仅属于官方归档；CFIR 输出可以重新分配编号，但必须保持上述不同角色及真实声明绑定。

## 4. 固定 native：先形成 callee 签名，再计算 call 结果

固定 `src/Sema/TypeCheckCall.cpp`：

| 方法 / 行号 | 已核实职责 |
| --- | --- |
| 完成候选后的写回 L1351–1354 | `ce.baseFunc->ty = ApplySubstPack(funcTy, mapping)`；随后 `rawTy = GetCallTy(...)`，`ce.ty = ApplySubstPack(rawTy, mapping)`，另调用 FillTypeArgumentsTy。callee 函数类型与 call 结果由两条赋值分别发布。 |
| `GetCallTy` L1472–1493 | 普通返回保持原 retTy；ClassThisTy 实例 MemberAccess 返回 `ma.baseExpr->ty`，static 返回 owner ty，无限定类体调用按当前类构造 This 视图。这是 **Call 结果投影**。 |
| `DynamicBindingThisType` L2702–2724 | 为返回 This 的 callable，根据真实 receiver/调用域把 `baseExpr.ty` 重建为保留参数列表的函数类型。class receiver L2714–2715 的返回是实例化 ClassThisTy；泛型 receiver L2717–2721 有不同的 generic 返回合同。不是把所有结果都强加 This flag。 |
| 候选 expected-return 检查 L1402–1408 | GetCallTy 用于结果兼容；DynamicBindingThisType 更新 callee 的返回签名，两个动作不能合并。 |
| `ReorderCallArgument` 末段 L1465–1466 | 再次分别写 baseFunc.ty 与 `ce.ty = GetCallTy(...)`。 |

固定 `src/Modules/ASTSerialization/ExprWriter.cpp::SaveExpression(MemberAccess)` L136–155 原样保存 ma.instTys 和 matchedParentTy；函数类型来自该表达式自己的 NodeInfo/type。`SaveExpression(CallExpr)` 使用 Call 自身类型和 callee operand；不需要从 Call.type 倒推出 callee Func.return。

固定 `src/Modules/ASTSerialization/ASTWriter.cpp::SaveNominalTy` L995–1012 将 `Is<ClassThisTy>` 独立写为 isThisTy，FuncTy 的参数/返回又各自保存。因此 serializer 中补 This flag 会掩盖 resolve 产品的角色错误，不是正确修复位置。

## 5. CFIR 确定的混用链

以下完整仓库内路径均相对 W。

### 5.1 call 结果的归一化 owner 是正确的

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt`：

- `substitutedReturnType` L846–863 先应用 candidate substitutor，再调用 `bindThisTypeToCallSite`。尽管方法名为 substitutedReturnType，其当前语义包含**调用结果投影**。
- `bindThisTypeToCallSite` L878–889 对显式非 static receiver 返回 `explicitCallSiteReceiverType()`；L910–912 从 dispatch/extension receiver 读取已完成类型。
- 对本 fixture，receiver 是普通 Carrier<Int64,Bool>，所以这个 owner 得出标记 false 的 call 结果符合 native GetCallTy。

这条方法不能直接作为“完成的 callee signature return”查询使用；也不能为了 L331 把它改成始终保留 This，从而破坏 L332 的 call 结果及正常类型兼容。

### 5.2 writer 将结果写入引用的签名字段

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt`：

1. `prepareQualifiedTransform` L161–166 使用 candidate.bindThisTypeToCallSite 计算表达式结果；L196、197 分别写回自身 typeArguments 与表达式 type。
2. `transformFunctionCall` L438 取得 completedFunctionCallResultType，L486–494 进行正常错误/结果选择，L502 写 Call.type。
3. L503–505 在成功调用上把同一个 `resultType` 传给 `resolvedReferenceFor`。
4. `resolvedReferenceFor` L1597–1607 将 resultType 作为 `buildAppliedCallableReference` 的第三个参数。

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAppliedCallReferenceFactory.kt`：

- L25–31 第三个参数名叫 `substitutedReturnType`，但它接收上面的 call result。
- L39–44 从真实 formal 参数类型分别应用 candidate/final substitutor。
- L45–52 直接将传入的值存到 applied-reference.substitutedReturnType，没有从 callee 返回声明/完整应用签名建立它。

这里把“参数是 callee 的完成 formal types”与“返回是已归一化的 call result”拼成了一个不一致签名。

`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/references/impl/CfirResolvedAppliedCallableReference.kt` 的字段合同原本声明为 use-site 完成的 callable 参数/返回类型；该字段应该表达 callee signature return。当前 class 本身只是保存输入，真正错误是 factory 输入合同与调用者传值。

### 5.3 SOURCE 读到了已经错误的完成签名

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt`：

- `call` L398 计算 appliedSignature；L411 用这个 signature 生成 callee 的 Member/Reference expression 记录。
- `appliedSignature` L434–436 将 applied.substitutedParameterTypes 与 applied.substitutedReturnType 组装成 ConeFunctionType。
- Call 的 record type 则来自完成 Call 自身类型，保持 nominal。
- `resolvedReference` L446–452 只发布目标、传入的 instantiatedTypes 和独立 matchedParentType。它没有从 resultType 生成 This 返回签名。

因此本轮应修 resolve/completion 对 applied signature 的发布，SOURCE 的 appliedSignature 继续消费正确的已完成产品。让 SOURCE 忽略 applied reference、重新读原声明并重新替换，会丢失 use-site 泛型/receiver/PCLA 完成事实，并重新制造另一套解析路径。

## 6. 精确生产修复 owner 与所有相关 readsites

### 6.1 应形成的完成合同

建议主线程在 existing Candidate/应用引用 factory 的 resolve owner 建立明确的两份查询/产品（名称为实施建议，不声称已有 API）：

```text
completed callee signature:
  真实参数 types + 完成的 signature return
  声明自身/owner type substitutions + final substitutor
  native DynamicBindingThisType 对 receiver/调用域的签名绑定

completed call result:
  对 signature/declaration return 执行 native GetCallTy 对应的调用结果投影
```

signature return 的来源是 callee 完成的声明返回和真实候选替换，不是 `call.resolvedType`。返回 This 时，应按 native DynamicBindingThisType 处理 receiver 所属类型和其真实 TP；本 fixture 为 ClassThis(Carrier,[Int64,Bool])。派生 class receiver、泛型 receiver和无显式 receiver 的不同 native 规则不能被一个“保留 This flag”的布尔补丁替代。

工厂可以接收已完成的高层 callee signature，或在它的唯一 resolve owner 从 Candidate 完成该签名；两者都必须保证参数/返回属于同一份 substitution/receiver 合同。不能继续以 `substitutedReturnType` 参数名包装实际 call result。

### 6.2 必须接通的 caller / consumer

| 层 / 完整仓库内路径 | 方法、精确位置 | 修复/核验职责 |
| --- | --- | --- |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt` | substitutedReturnType L846–863、bindThisTypeToCallSite L878–889 | 保留现有 call-result 语义；提供或消费独立的 callee 签名完成查询，不能复用已执行 GetCallTy 投影的返回作为签名。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAppliedCallReferenceFactory.kt` | buildAppliedCallableReference L25–53 | 唯一应用引用构造 owner，发布一致的完成 signature parameters/return，保持 target、matchedParentType、isFunctionValue。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt` | transformFunctionCall L438、502–505；resolvedReferenceFor L1597–1607 | Call.type 保持结果类型；应用引用使用完成的 callee signature return。不得把成功 call 的 resultType 继续传为签名。 |
| 同 writer | transformQualifiedAccessExpression 的函数值路径 L1163–1167；computeNamedValueFunctionType L242 起 | 一并核对 named function value / callable return 属于哪份签名。不能因为表达式类型是 ConeFunctionType 就对普通“返回函数的调用”任意取其 returnType；已有 isFunctionValue 角色应保留。 |
| `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/references/impl/CfirResolvedAppliedCallableReference.kt` | substitutedReturnType、substitutedParameterTypes 字段 | 用明确中文合同说明 callee signature，与表达式 call result 分开；不增加 serializer-only 修补字段。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirTypeVariablesAfterPCLATransformer.kt` | transformResolvedNamedReference L83–95 | 已独立替换 applied return/parameter/parent；新的正确签名输入应继续经过这条 final-substitution 链，保持 This 及 owner 绑定。 |
| `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyVisitor.kt` | copyAppliedReference L146–157 | 已通过 context.type 复制签名参数/返回并重定位 target/parent；保持签名产品身份，不从被复制的 Call.type重建返回。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt` | call L398–411、appliedSignature L424–442、resolvedReference L446–452 | 消费完成 callee signature，分别发布 callee record type、Call record type、instTys/parent。当前读取的签名字段正确 owner 不变。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt` | captureElement L694–707；validation L299–301 | 分别冻结表达式 type 与 appliedReturnType/parameters/parent，并对原身份校验；新的应用签名不是从 call type导出的同一个快照槽。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt` | captureExportSourcePayload AppliedReference L334–337 | 对正确的引用参数/返回独立捕获；不后补 This 标记。 |
| `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt` | expression L41–48、reference L159–165 | 按各 record.type 编码 Member/Call；instTys、matchedParentTy 独立编码。不是生产修复处。 |

当前仅有的普通 applied-reference factory 生产调用在 writer.resolvedReferenceFor；copy/PCLA 是重建已存在引用的另两种 lifecycle owner。`analysis/.../CaCfirResolver.kt` 当前分支只从这个引用读取 resolvedSymbol，本轮未发现它直接消费 substitutedReturnType；不据 imported class 名字就扩展为 analysis API重写。

`ResolveUtils.kt::functionTypeForFunctionValueCandidate` L245–259 目前调用 candidate.substitutedReturnType 后 approximateThisTypeForDeclaration，是**函数值表达式类型**入口，应与上述 function-value 路径一并核验真实 native 值语义。此处不是本次 MemberAccess调用失败的直接 factory caller，不能拿它替代本次修复 owner。

## 7. 固定 Kotlin 的准确框架对应与语义边界

已 `git show` 的固定文件为：

`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt`。

- `prepareQualifiedTransform` L149 起从 candidate/声明计算类型、完成 receiver/引用、L223 写 typeArguments。
- `transformFunctionCall` L450–489 对表达式结果继续进行 candidate/final substitution和类型近似，并写回 Call.type。
- `computeTypeArguments` L1026–1077 / `computeTypeArgumentTypes` L1079–1093 属于完成 callable 自身泛型实参写回；不是从返回 nominal 类型解包 class arguments。
- `transformCallableReferenceAccess` L837–877 独立完成函数引用的类型与 inferredTypeArguments，L873 调同一 computeTypeArgumentTypes；不从另一个调用结果推导引用自身的实例化实参。

**Kotlin 没有仓颉 ClassThisTy / native MemberAccess 源图这一直接语义。** 本库 CfirResolvedAppliedCallableReference 与 SOURCE appliedSignature 因而没有一个可以逐字复制的 Kotlin This flag实现。应保留 Kotlin 的 declaration/candidate → completion → reference/表达式产品 → 后续编码职责，按 native 在这个 resolve/completion owner 中落实 This 的签名与结果差异。

不能给 Kotlin 普通函数返回近似 helper附会一个不存在的 Cangjie This counterpart，也不能把 SOURCE encoder 当成实现这些语义的 owner。

## 8. instTys 的另记证据：没有证明它是当前失败

当前 W writer `computeTypeArgumentTypes` L1523–1537 已使用 `declaration.typeParameters.ownTypeParameters`，并对各自 symbol应用 candidate/final替换；不会直接把 Candidate.freshVariables 或 receiver名义 typeArguments 写进方法 typeArguments。

asThis 原声明没有函数自有 TP；其 class T/U参与候选 owner 替换，但本次完成方法自身 instantiation应为空。`computeTypeArguments` L1566–1580 保留显式错误/额外源 type refs 的合同，不是给无源码类型实参的 asThis 增加 class槽的依据。

SOURCE `access` L370、`call` L399 显式传入完成的 access/call.typeArguments；`resolvedReference` L446–452 不把 matchedParentType或返回类型的 class arguments追加为 instTys。当前 applied-reference class也没有另一个自身类型实参数组字段；不应凭名称认为它已有第二套 instTys。

固定 native `TypeCheckUtil.cpp::GetCurrentGeneric` L684–691 对普通函数取 fd.funcBody.generic；仅 type-object creation在函数无自有generic时取 outerDecl generic。`TypeCheckCall.cpp::FillTypeArgumentsTy` L1234–1315 把函数自身 instantiation与 qualifier/enum owner instantiation分开处理；owner泛型必要时写在 base reference，而不是把所有 class TP塞到实例方法ref。

Kotlin constructor的 typeParameters可包含 constructed-class refs（固定 AbstractRawFirBuilder.kt L200 起）；W ownTypeParameters helper排除 CfirConstructedClassTypeParameterRef。constructor/enum/qualifier因此有自己的 owner实例化语义，不能把“asThis的方法自身列表为空”扩展成“所有 constructor和owner reference都应为空”。这不是当前408/331失败的证据，也不在本轮修改那些路径。

已解码的官方 asThis MemberAccess.instTys=[]，现有测试stack明确停在 This flag。没有新增 instTys失败证据，本报告不给 instTys路径另报确定bug。

## 9. 主线程修复后的真实验收点

应在完成 CFIR 引用本身先观察到：

```text
callee reference.substitutedParameterTypes = []
callee reference.substitutedReturnType = ClassThis(Carrier,[Int64,Bool])
call.resolvedType = Class(Carrier,[Int64,Bool])
call.typeArguments = []
reference matchedParentType = Invalid（保持原生角色）
```

随后 SOURCE callee Member record 的 type应为返回 This 的 Func，Call record type仍为 nominal；copy/PCLA/export冻结保持这些独立值。原 fixture和 L331/L332正确断言保持不变。

继承 receiver、generic receiver、函数值引用、函数返回函数、正常 asNominal调用应按各自真实签名/结果合同验证，避免只保住当前 Carrier同类场景。此为主线程后续验证范围；本轮没有运行这些验证。

读取快照 SHA-256：

| W 文件 | SHA-256 |
| --- | --- |
| CfirAppliedCallReferenceFactory.kt | `BFB483712FA5C0F622FA5DD9AEA278B080F9D067D01E81077F440B3EB945C6C1` |
| Candidate.kt | `BE0C0B4A5CDBB244FFA5DD9CBECC68007C0D67D80A8D9342F769C78CAF5D2D5D` |
| CfirCallCompletionResultsWriterTransformer.kt | `E215EC86FF110616ADBBE7006CDEFB91A330BA33F3FB76AAD4E5694A50E403A2` |
| CfirSourceGraphProducer.kt | `0D41DE85DF456DB9AEF0447BD149B886B9D7B1ED016E06D73FE26A32DAA15F0E` |
