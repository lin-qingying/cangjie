# P0：22 条高严重度问题的源码与复现索引

本索引对应 `docs/cjmp-framework-review-and-fix-plan-20260928.md` §2 的 **22** 个编号。2026-09-29 补充；只改本 scratch 目录，不修改正式 source set、testData 或生成器，未运行 Gradle。

`p0-fixtures/` 中新增的是可交给官方编译器的纯仓颉源码，不含猜测的内联诊断期望。`p1-tests/` 中已有 A1 分模块 fixture 与 A2 LL 测试草稿。已有 `p_*` 探针直接复用。本轮新增形状均**未运行 cjc**；表中的“源码确认”只表示语义由官方实现或文档支持，不表示本仓已经出现红测试，也不表示精确诊断位置/数量已验证。

## 执行形态

- **PAIR**：分别编译表中 common/specific 源码。common 用 `cjc common.cj --experimental --output-type=chir --output-dir <独立 common 输出目录>`；specific 用 `cjc specific.cj <common 输出目录>/<pkg>.chir --experimental --common-part-cjo=<common 输出目录>/<pkg>.cjo --output-type=dylib --output-dir <独立 specific 输出目录>`。入口和 SDK 版本沿用本目录 `PROBE-FINDINGS.md`。不得在原有产物目录覆盖 common CJO。
- **SOURCE**：把 PAIR 的两份源码按既有 `// MODULE: common` / `// MODULE: specific()()(common)`、`CJMP_MODE`、`FILE` 指令装入正式 fixture。负例须先取得官方 JSON 诊断位置后再加入 inline markers；不能把下面的纯源码直接当成“期望零诊断”的正式负例。
- **LIBRARY**：库与客户端使用不同包、不同输出目录，通过显式库路径加载普通 CJO。只比较语义诊断，不把缺少链接对象产生的 link error 算成语义结论。
- **LL**：真实 `collectDiagnosticsForFile` / FileStructureElement 入口；输入源码与语义断言不足以替代请求顺序断言。不得先推进被调声明后声称验证了 caller-first。
- **HOST**：经过生产 standalone/CLI 装配。手动注入 `CfirCjmpSettingsComponent`、手动设置 `cjoOutputDirectory` 或调用 `CfirFrontendPipelinePhase.executePhase` 的测试不能证明对应 HOST 问题修复。

## 全部高编号

| 编号 | 可执行源码 / 草稿 | 已确认的预期与观测点 | 状态和剩余缺口 |
|---|---|---|---|
| A1 | [默认参数推断调用](p1-tests/cjmpDefaultReadThroughBeforeImplicitTypes.cj)、[同名候选推断调用](p1-tests/cjmpSpecificShadowingBeforeImplicitTypes.cj)；SOURCE | use3/use4 省略返回类型，应该推断 Int64；默认参数有效、common 不与 matched specific 形成歧义。默认参数用例还应核对 call 最终绑定 specific。 | 官方源码和文档确认；未运行。纯诊断 fixture 可能漏掉“静默绑定 common”，符号 identity 必须由 LL/前端断言补齐。 |
| A2 | [defaultArgumentCallerFirst.cj](p1-tests/defaultArgumentCallerFirst.cj)、[shadowedCommonCallerFirst.cj](p1-tests/shadowedCommonCallerFirst.cj)、[LL 测试草稿](p1-tests/CfirCjmpCallerFirstDiagnosticsTest.kt.draft)；LL | 第一个语义请求只检查 caller 文件；事前 callee 阶段小于 CJMP_MATCHING、映射为空；事后无诊断，call 的 resolvedSymbol 与 callee 文件 specific symbol 同一。 | 草稿未编译/运行。P1 暂时的 CJMP_MODE 测试注入不能计入 G1/P7 验收；优先复用统一测试配置 owner。 |
| B1 | [helper common](p0-fixtures/b1_helper/common.cj)、[helper specific](p0-fixtures/b1_helper/specific.cj)；[unmarked common](p0-fixtures/b1_unmarked/common.cj)、[unmarked specific](p0-fixtures/b1_unmarked/specific.cj)；另复用 [enum common](p_b1/common.cj) / [enum specific](p_b1/specific.cj)；PAIR+SOURCE | helper 没有 COMMON 位，不得被报告缺少 specific；api 可调用 helper。未标记 g 不能成为 specific g 的对应物，specific g 应有 NOT_MATCHED，未标记 g 保留参与重定义。enum 两种载荷形状的 COMMON 与 FROM_COMMON_PART 位按旧探针分别恢复。 | enum 属性位已实测复用。新增 helper/g 为源码确认、未运行；g 的冲突诊断精确数量/锚点待 cjc。两段式 harness 必须检查 messages、completed 和结构化 diagnostics，不能只看 diagnostics。 |
| C1 | [default-member common](p0-fixtures/c1_default_member/common.cj)、[specific+Repo().extra()](p0-fixtures/c1_default_member/specific.cj)；另复用 [省略构造器 common](p_c1/common.cj) / [specific](p_c1/specific.cj)；PAIR+SOURCE | specific 空 Repo 应使用 common 默认构造器和 public extra；Repo().extra() 应合法且返回 Int64。旧 p_c1 验证 specific 省略构造器允许。 | 省略构造器已有实测；成员调用新增形状为源码确认、未运行。须核对合并后的 scope 中构造器和 extra 的 symbol，而非只免掉 NOT_MATCHED。 |
| C2 | [generic-interface common](p0-fixtures/c2_unpaired_subtype/common.cj)、[普通 D<U> 与 D<Int64>](p0-fixtures/c2_unpaired_subtype/specific.cj)；PAIR+SOURCE | specific I 空体仍继承 common f(U) 默认成员；未参与 CJMP 配对的 D<U> 声明自己的 f(Int64)，D<Int64> 应出现泛型实例化歧义。 | 官方声明级合并与实例化路径确认；精确形状未运行，不能预写“恰 1 条/精确高亮”。必须同时用 SOURCE 与 CJO PAIR 复现，避免 Library-origin 过滤被源码路径掩盖。 |
| C3 | 直接复用 [p_c3/common.cj](p_c3/common.cj)、[p_c3/specific.cj](p_c3/specific.cj)；PAIR+SOURCE | common 与 specific 类中的两个**未标记** f() 经合并产生 overload conflicts。 | 已有 cjc 实测：1 error。转换正式 fixture 前取得本用例 JSON 位置及 note；不能依据其它 ctor fixture 的定位推定。init/extend/prop 的同族变体尚未覆盖。 |
| C4 | [common platform](p0-fixtures/c4_callable_reference/common.cj)、[specific+apply(platform)](p0-fixtures/c4_callable_reference/specific.cj)；PAIR+SOURCE | 函数引用在候选类型收集前排除已由 specific 替代的 common；apply(platform) 应合法。还应检查实参引用的目标是 specific。 | NameReferenceExpr.cpp 官方路径确认；未运行。下游第三模块 use-site session 的变体尚无宿主 fixture。 |
| C5 | [common Base 默认 init](p0-fixtures/c5_implicit_super/common.cj)、[specific Base+Child 隐式 super](p0-fixtures/c5_implicit_super/specific.cj)；PAIR+SOURCE | specific 构造器从 common 得到 x! 默认值，Child 的隐式 super() 不应报 NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS。 | 默认参数复制与 HasNonParamCtorForClass 官方路径确认；未运行。递归构造检查/CJO 写出默认值是其它消费点，仍需阶段内追加检查。 |
| D1 | [public bounded owner](p0-fixtures/d1_public_owner/main.cj)、[bounded owner 自带接口](p0-fixtures/d1_owner_interface/main.cj)；单模块普通编译 | 两份源码均保留声明级 f(U)/f(Int64) 的不同身份；Int64 满足 Bound 后实例化应产生歧义，不应因具体签名预先合并而消失。 | 官方 owner 图/实例化路径确认；精确形状未运行。至少确认具体诊断数/位置与 public-owner 模式的其它合法性诊断，才能转正式 negative fixture。相同约束 bounded-peer 第三形状仍缺。 |
| D2 | 复用 [generic_outer/common.cj](../generic_outer/common.cj)，配 [含具体 Holder<Int64> 的 specific](p0-fixtures/d2_concrete_holder/specific.cj)；PAIR+SOURCE | matched specific extendIdentity 已替代 common 实现，Holder<Int64> 不得把被遮蔽 common 当作第二实现而报歧义；调用返回 Int64。 | 既有 generic_outer 无具体实例化版本已实测；新增具体实例化源码未运行。**旧 FIXTURE-DRAFTS.md 的“报冲突”有误，本条应无该冲突**，见下方裁决。 |
| D3 | 直接复用 [p_d3/main.cj](p_d3/main.cj)；单模块普通编译 | 只报 deriving member visibility 错误，不报 EXTEND_MEMBER_CANNOT_SHADOW。 | **旧审查预测已被官方实测推翻**，此例是防回归证据，不是已知应红的用例。现存 implementsInterface 启发式是否还有错误形状仍须再取证，不能据此把 D3 框架整改宣告完成。 |
| D4 | 直接复用 [bound 未满足库](p_d4/ownermatrixlib.cj)、[bound 满足库](p_d4/ownermatrixlib_sat.cj)、[同一 client](p_d4/client.cj)；LIBRARY | sat：1 条 OwnerBox<Int64> 泛型实例化歧义 + 2 个 found-candidate note；unsat：无该歧义。 | 已实测复用；note 指 CJO 记录的库源 17:17/21:18。正式 host 测试须保存库路径身份、行列，不能把 link error 混进 sema 期望。 |
| E1 | [common abstract A](p0-fixtures/e1_marked_abstract_members/common.cj)、[specific abstract A](p0-fixtures/e1_marked_abstract_members/specific.cj)；PAIR+SOURCE | 已标 common/specific 的 f 不必额外声明 abstract/open；specific f 带实现体合法，显式 init 排除缺构造器干扰。 | DeclAttributeChecker.cpp 条件确认；未运行。属性成员、FROM_COMMON_PART、错误参数文本各分支仍需补独立样本。 |
| E2 | 复用 [双侧 @OverflowWrapping common](p_e2a/common.cj) / [specific](p_e2a/specific.cj)，及 [仅 specific 注解 common](p_e2b/common.cj) / [specific](p_e2b/specific.cj)；PAIR | 两例均报 specific 注解失配，common CJO 不序列化该注解。 | 已实测；这两例**不能证明** CUSTOM common-only、nominal 或注解实参不同的分支。后者缺已确认的注解构造源码/精确期望，必须单独补证；不能用 OverflowWrapping 泛化。 |
| E3 | [common S <: I](p0-fixtures/e3_same_count_interfaces/common.cj)、[specific S <: J](p0-fixtures/e3_same_count_interfaces/specific.cj)；PAIR+SOURCE | I/J 都为空接口，个数相同而类型不同，应报告 SPECIFIC_HAS_DIFFERENT_SUPER_TYPE。 | 官方逐类型比较确认；未运行，精确高亮待 JSON。泛型实参替换及不同 concrete superclass 两变体尚缺。 |
| E4 | 直接复用 [p_e4/common.cj](p_e4/common.cj)，**仅编译 common 侧**；辅助 [p_e9/common.cj](p_e9/common.cj) | common 类中的 common private f 合法，不应产生修饰符冲突；p_e9 两个 private 重载不应误报同名重复。 | common private 类成员已实测；不能擅自断言 p_e4/specific.cj 的整对编译零诊断，它改变了 f 的可见性。specific private、struct/enum/prop/const/foreign 的分表边界尚缺。 |
| F1 | [函数类型 common](p0-fixtures/f1_function_parameter/common.cj) / [specific](p0-fixtures/f1_function_parameter/specific.cj)，[元组类型 common](p0-fixtures/f1_tuple_parameter/common.cj) / [specific](p0-fixtures/f1_tuple_parameter/specific.cj)；PAIR+SOURCE | 把 common T 替换到 specific U 后，整个函数类型/元组类型应等价并成功配对，不报 NOT_MATCHED。 | 官方 GetInstantiatedTy 后整体比较确认；未运行。VArray 与 enum 函数载荷变体尚缺，不对其语法和诊断猜测。 |
| F2 | [全部匹配 common](p0-fixtures/f2_tuple_pattern/common.cj) / [specific](p0-fixtures/f2_tuple_pattern/specific.cj)，[部分匹配 common](p0-fixtures/f2_partial_pattern/common.cj) / [specific](p0-fixtures/f2_partial_pattern/specific.cj)；PAIR+SOURCE+LL | 全部绑定的 specific let (a,b) 合法；部分匹配保留 a 的成功配对，失败 binding 与整体 pattern 报失配。LL 不得因 pattern 自身没结果而抛异常。 | 官方逐 binding 规则确认；未运行。部分匹配的诊断数量/锚点待 JSON；LL pattern 请求与完整结果断言的 Kotlin harness 尚缺。遇非 VAR_PATTERN 会 break，不能编造“始终检查后续所有 binding”。 |
| G1 | [common.cj](p0-fixtures/g1_standalone/common.cj)、[specific.cj](p0-fixtures/g1_standalone/specific.cj)、[caller.cj](p0-fixtures/g1_standalone/caller.cj)；HOST | 一个 common 源模块和一个 specific 源模块，specific dependsOn common；生产 session 应取得模式并完成配对，caller 无 PARSE_*_IN_NON_*_FILE 与歧义。 | 输入源码已备，**生产 standalone 集成 harness 尚缺**。不能使用 A2 的手动模式注入代替。模块角色/dependsOn 配置入口本身属于 P7 实现范围，具体补点见下文。 |
| H1 | [library.cj](p0-fixtures/h1_full_id/library.cj)、[client.cj](p0-fixtures/h1_full_id/client.cj)；LIBRARY | Envelope<T> 没有显式 where：CJO 不应写合成默认 Any 约束；String/Object 的实际 FullId 引用须对称解析，client 的声明/调用类型保持各自 ClassId。 | 源码已备，尚未运行。需真实 CJO header/metadata 检查及加载断言，单独编译源码成功不能验证本仓读写键约定。无 std.core.cjo 的变体必须走 builtins provider，不能硬编码 exportId 测试期望。 |
| H2 | [common.cj](p0-fixtures/h2_exact_path/common.cj)、[specific.cj](p0-fixtures/h2_exact_path/specific.cj)、[stale.cj](p0-fixtures/h2_exact_path/stale.cj)、[wrong_package.cj](p0-fixtures/h2_exact_path/wrong_package.cj)、[neighbor.cj](p0-fixtures/h2_exact_path/neighbor.cj)、[neighbor client](p0-fixtures/h2_exact_path/specific_imports_neighbor.cj)；HOST+PAIR | 精确指定 common CJO 时不能被 classpath 同包 stale CJO 替换；显式 wrong-package 路径须基于实际 header 拒绝；common CJO 父目录不能自动暴露 neighbor 包。 | 三个输入场景已备，**目录布局与实际加载文件 identity 的 host 断言尚缺**；不预填跨场景所有诊断的数量/工厂。不要把 wrong_package.cjo 改名为目标包 cjo，否则又会绕开待修路径。 |
| H3 | [common.cj](p0-fixtures/h3_cli_output/common.cj)、[specific.cj](p0-fixtures/h3_cli_output/specific.cj)；HOST | 真实 CLI common 编译应根据输出参数生成可供第二阶段加载的 CJO；两阶段都经过 exec / AbstractFrontendPipeline.execute；错误必须经真实 collector 外显。 | 源码已备，**真实 CLI 两段式 Kotlin host 测试尚缺**。现有只捕获参数的 stub pipeline 不能当作复现或验收。不得手动设置 cjoOutputDirectory 让用例绿色。 |

## 新增源码的官方依据

以下由 CangjieSemanticsAuthority 在本轮对官方 v1.1.3 复核后确认，仅登记到行为层，不伪造运行日志：

- A1/A2：`TypeChecker.cpp:2027–2047` 匹配先于主体；`CheckCJMP.cpp:948–963` common 默认参数复制到 specific；函数末项调用决定推断返回类型由 docs MCP 的函数定义说明确认。
- B1：`CheckCJMP.cpp:668–693` 只把 COMMON 声明加入对应物集合。FROM_COMMON_PART 是来源信息，不能制造 COMMON 资格。
- C1/C2：`CheckCJMP.cpp:219–293` 的合并向所有后续消费者提供同一成员面。
- C2/D1：`InstantiatedChecker.cpp:60–122,125–144`、`StructInheritanceChecker.cpp:362–412`、`MergeInheritedMemberHelper.cpp:139–177`；声明级 f(U) 与 f(Int64) 保持不同，实例化后才出现碰撞。
- C4：`NameReferenceExpr.cpp:330–344` 的 ChkRefExpr 在 CollectValidFuncTys 前调用 RemoveCommonCandidatesIfHasSpecific。
- C5：`CheckCJMP.cpp:948–963` 复制默认值，`TypeChecker.cpp:1523–1583` 的无参构造可用性查询使用参数 assignment。
- D2：`CheckCJMP.cpp:387–445` 合并 CJMP extends 后，matched common 不独立形成第二实现。
- E1：`DeclAttributeChecker.cpp:317–322` 的 !isCJMP / !constructor / !hasBody / !FROM_COMMON_PART 等完整条件；显式 abstract 只加在类上，不能给带 body 的 f 添加 abstract。
- E3：`CheckCJMP.cpp:818–856` 比较替换后的超类型，个数相同并不表示类型相同。
- F1：`MatchCJMPFunction`（`CheckCJMP.cpp:923–931`）对整个 FuncTy 做 GetInstantiatedTy 后再比较。
- F2：`TryMatchVarWithPatternWithVarDecls`（`CheckCJMP.cpp:1090–1112`）逐 binding 保存成功；非 VAR_PATTERN 将 matchedAll 置 false 并 break；失败报告与 pattern 报告不能用 all-or-none 代替。
- H1/H2/H3：主计划所核实的 `ASTWriter.cpp:1125–1162`、`CjoManager.cpp:460–489`、`ImportManager.cpp:296–298`。这些是序列化/driver 契约，新增源码仍需 host harness 验证契约而非仅验证语法。

## D2 期望纠正

旧 `FIXTURE-DRAFTS.md` 对 Holder<Int64> 写“报冲突”，与主文档 D2 的问题定义矛盾；主文档附录 A.3 只列出该具体实例化场景，没有写诊断结论。D2 的失败机制是“原本已被 specific 替代的 common extendIdentity 被补回而产生**误报**”。本轮 authority 确认正确行为是没有该重复实现/实例化歧义。这里保留既有文件不改，只在唯一新索引明确纠正；后续正式 fixture 必须采用无该冲突的语义，不能把当前误报固化成期望。

## 宿主测试落点与必须补的断言

### B1/C2：源码与 CJO 一致

复用 `compiler/frontend/test/org/cangnova/cangjie/frontend/pipeline/CjmpTwoPhaseCompilationTest.kt` 的真实库产物读取方式，但必须检查阶段 completed、messages 中的错误和结构化 diagnostics。B1-helper 在 CJO 中必须验证 helper 的 COMMON=false、FROM_COMMON_PART=true，加载后也分别保留；B1-unmarked 的配对候选集不得包含 g。C2 同一 specific 源必须分别在 SOURCE 与 PAIR 两路径触发，不能用源码成功代替 Library-origin 行为。

### F2：真正请求 pattern

可以复用 P1 的手写 LL 执行基座，先取得 raw pattern 声明，随后仅从其源声明/文件诊断 API 进入。全匹配时确认 pattern 与 binding 的结果均可读取且不抛异常；部分匹配时确认 a 的 successful counterpart 保留。不得只对两个 binding 单独主动解析，以此绕开 pattern 后置条件。

### G1：生产 standalone 模块图

现有生产入口位于 `analysis/analysis-api-standalone/src/org/cangnova/cangjie/analysis/api/standalone/session/CaStandaloneSessionBuilder.kt`；现有宿主测试位于同模块 `test/.../session/builder/StandaloneSessionBuilderTest.kt`。用上述三个源码构造两个 source module，并由正式 project structure provider 指定 dependsOn 和 CJMP 角色。随后通过正常分析/LL facade 请求 caller 诊断和 platform symbol。测试代码不得直接 register CfirCjmpSettingsComponent；正式 API 尚不能表达时，记录为 G1 红测试的装配缺口，而不是新增仅测试可见的模式旁路。

### H1：实际 wire key 与类型恢复

library 的 Envelope<T> 用于检查“没有显式 where 时不写默认 Any constraint”；retainString/retainObject 使 CJO 必须写外部 nominal 类型引用。client 用于检查加载后的 ClassId 与泛型替换。对实际 SDK std.core.cjo 运行一份，另在没有 std.core.cjo、仅有正式 builtins provider 的会话运行一份。禁止为断言写入 `_CNat3AnyE` 白名单或按 type.toString() 匹配尾缀。

### H2：三个独立布局

1. `wanted/<pkg>.cjo` 由 common.cj 产生；`stale/<pkg>.cjo` 由 stale.cj 普通编译产生。common-part 精确指 wanted 文件，普通 classpath 包含 stale。断言依赖模块 provider 加载的是 wanted 文件及 Int64 api，不能只从报错数量猜实际文件。
2. wrong_package.cj 产出的 `p0_h2_other_package.cjo` 保持原名，通过 common-part 精确路径传给 p0_h2_exact_path 的 specific；断言 header 包名不匹配的加载诊断，而非用 NOT_MATCHED 级联替代。
3. neighbor CJO 与 wanted CJO 放在同一输出目录，该目录不在普通 classpath；specific_imports_neighbor.cj 不得仅因 common-part 输入的父目录而获得该包。具体 import 诊断工厂/位置需官方 JSON 确认。

### H3：真实 CLI 两阶段

CLI 宿主抽象为 `compiler/cli/src/org/cangnova/cangjie/cli/CangJieCLICompiler.kt` 的 `CangJieFrontendCLICompiler`，实际配置入口是 `compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/AbstractFrontendPipeline.kt:execute`。测试可使用产品所需的具体参数类型与真实 pipeline，但不能覆盖为“捕获收到的参数即返回成功”的 stub。第一阶段只从 CLI 输出参数得到 CJO 路径；检查文件存在且 header 包名正确；第二阶段把该精确路径作为 common-part 输入，检查成功完成及 specific 输出。未完成此宿主草稿前，H3 只有可执行语言输入，不满足 P0 的可运行 host 红测试要求。

## 完成度

22 个高编号均已在表中登记具体源码或已存在探针路径。**P0 仍未验收完成**：新增精确形状没有 cjc 运行证据；负例的 JSON 位置/数量尚未验证；G1/H2/H3 生产宿主测试、F2 LL pattern 测试及 H1 wire 断言仍未落为可运行 Kotlin 草稿；D3 原预测已推翻，剩余启发式的真实反例未证实。跨版本差异清单也不由本索引替代。
