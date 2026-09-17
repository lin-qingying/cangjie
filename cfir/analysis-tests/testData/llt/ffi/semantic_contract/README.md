# FFI 语义契约测试与证据

## 语义基线

- 官方编译器：`external/cangjie_compiler`，revision `896235c9fd18f22d570a9818ac672838c36c3932`。
- 官方文档：Cangjie docs MCP 的 `manual_source_zh_cn_cangjie_c_CType_13`、`manual_source_zh_cn_cangjie_c_CFunc_2`。
- 可执行探针：`C:/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc.exe`，只用于补充验证，不替代仓库 `cangjieVersion=1.1.1`。
- 所有探针源码位于仓库根目录 `tmp/ffi-annotation-contract-probes/`，使用 `--output-type=staticlib --diagnostic-format=json --error-count-limit all -Woff all`，不执行生成的机器码。
- `official-probe-evidence.json` 保存最终 13 个 fixture 去除 markers 后的精确源码探针结果：5 个正例编译成功，8 个负例共 43 个错误，诊断名称与 marker 集合逐文件一致。
- 本轮测试维护未运行 Gradle；仓库验证由主线程统一通过 Gradle queue 调度。

## 本目录用例

每个 `.cj` 同时进入 `CfirAnalysisLLTTestGenerated`（LightTree）和 `CfirAnalysisLLTPsiTestGenerated`（PSI）；测试类路径为 `Ffi.SemanticContract`。

| 文件 | 覆盖的语义 | 官方实现依据 | 可执行探针 |
| --- | --- | --- | --- |
| `ctypeParametersPositive.cj` | Bool、整数、浮点、指针、CString、CFunc、C struct 以及 typealias 的合法参数/返回 | `AST/Types.cpp: Ty::IsMetCType`；`Sema/FFI/CFFICheck.cpp: UnsafeCheck/CheckCFuncParam` | 0 个诊断，编译成功 |
| `ctypeParametersNegative.cj` | String/Rune/Float16/Option/CType 接口/普通函数不是合法 C 参数；Unit/named 参数专诊断；VArray 返回禁止 | `CFFICheck.cpp: UnsafeCheck/CheckCFuncParam` | 11 个错误，名称与位置已核验 |
| `foreignBlockCallsPositive.cj` | foreign block 声明收集、文件归属、foreign 状态和 unsafe 调用 | 官方 foreign declaration 与 `CFFICheck.cpp: SetForeignABIAttr/CheckUnsafeInvoke` | 0 个诊断，编译成功 |
| `cfuncPointerConversionsPositive.cj` | CPointer 到 CFunc、CFunc 到 CPointer、指针元素转换、CString 构造、CFunc typealias 构造 | `Sema/TypeCheckBuiltinExpr.cpp: SynCFuncCall/ChkPointerCall`；官方 CFunc 文档 | 0 个诊断，编译成功 |
| `cfuncSignaturesNegative.cj` | CFunc wrapper 内的 Unit/String 参数、String/VArray 返回与声明级规则一致 | `CFFICheck.cpp: CheckCFuncParamType` 与 `Sema/TypeCheckType.cpp` | 4 个对应错误；另行隔离了非函数类型参数探针 |
| `cstructFieldsPositive.cj` | 原始类型、CPointer、CString、CFunc、嵌套 C struct、VArray 字段 | `CFFICheck.cpp: CheckCTypeMember`；`Ty::IsMetCType` | 0 个诊断，编译成功 |
| `cstructFieldsNegative.cj` | Unit 字段专诊断；String/Rune/Option/普通 struct/普通函数字段拒绝 | `CFFICheck.cpp: CheckCTypeMember` | 6 个错误，范围为字段名到显式类型末尾 |
| `annotationAbiPositive.cj` | CDECL foreign、FastNative foreign、STDCALL @C、顶层/成员 Frozen 函数及 Frozen property | `CFFICheck.cpp: CheckAnnoC/CheckAnnoCallingConv/CheckAnnoFastNative/CheckAnnoFrozen` | 0 个诊断，编译成功 |
| `callingConventionTargetsNegative.cj` | CallingConv 的 CFunc 目标限制以及 C/CallingConv 的顶层限制 | `CFFICheck.cpp:18–67` | 5 个错误 |
| `annotationArgumentsNegative.cj` | C/FastNative/Frozen 拒绝参数；CallingConv 必须恰好一个参数 | `CFFICheck.cpp:18–95` | 5 个错误 |
| `callingConventionArgumentsNegative.cj` | 非 CDECL/STDCALL 引用和字符串实参分别诊断 | `CFFICheck.cpp:49–64` | 2 个错误 |
| `annotationTargetsNegative.cj` | C 非 struct 目标、C 泛型函数、FastNative 非 foreign、Frozen 非函数/property 和局部函数 | `CFFICheck.cpp:18–95` | 6 个错误，含泛型参数自身不满足 CType |
| `cstringConstructorsNegative.cj` | CString 缺参/多参、错误指针元素、非指针字面量 | 官方 CString 构造器和普通调用类型检查 | 4 个错误 |

`CType` 接口本身不满足 `CType` 约束；它作为泛型约束使用，不是可直接传入 C ABI 的参数或返回类型。

## 定位策略

官方诊断的语义对象与本仓库已有诊断位置策略共同决定内联范围：

- `UNABLE_TO_INFER_GENERIC_FUNC` 覆盖完整 callee token，含显式类型实参，不含调用参数；官方依据为 `TypeArgumentInference.cpp:148–154` 的 `*ce.baseFunc`。
- annotation 根诊断使用完整 annotation source；多出的无参注解实参使用该实参表达式。cjc 1.0.5 的通用 `Diagnose(anno)` 输出仅高亮 `@`，本仓库仍保留既有完整 annotation 节点策略。
- `INVALID_CFUNC_PARAMETER_TYPE` 使用现有 type reference 范围；named/Unit 参数的独立诊断使用完整 parameter source。
- C struct 字段诊断覆盖 `name: Type`，与官方 `identifier.Begin()` 到 `type.end` 一致。
- `WRONG_NUMBER_OF_ARGUMENTS` 覆盖参数列表；CString 的类型/字面量错误覆盖出错实参。
- C 泛型声明错误覆盖类型参数列表，保留完整泛型语法节点。

## 既有 fixture 修正

以下 10 个文件只修改内联诊断标记。逐文件比较 `HEAD` 与工作树、去除 markers 并统一 CRLF/LF 后，源码完全相同：

| 文件（相对 `cfir/analysis-tests/testData`） | 修正依据 |
| --- | --- |
| `llt/ffi/cpointer_generic_reverse.cj` | 6 处无法推断诊断从整个调用缩到 callee |
| `llt/ffi/cpointer_generic_reverse2.cj` | 2 处同类定位修正 |
| `llt/ffi/c_type_subtype.cj` | CPointerHandle 无法推断诊断缩到 callee；保留其 CType 实参不满足约束的期望 |
| `diagnostics/interop/foreignFunctionReturnTypeRich.cj` | CType/CTypeAlias 返回值不能用于 C ABI |
| `diagnostics2/interop/foreignFunctionReturnType.cj` | 同上 |
| `diagnostics2/interop/foreignFunctionMixedSignatureLegality.cj` | 补全所有 CType 参数和返回诊断 |
| `diagnostics2/interop/foreignFunctionTypeAliasExpansion.cj` | CTypeAlias 参数和返回也必须拒绝 |
| `diagnostics2/interop/foreignFunctionParameterTypePlaceholder.cj` | CType 参数不能作为合法 C 参数 |
| `diagnostics2/interop/foreignFunctionCFuncLegalityPlaceholder.cj` | CFunc 的 CType 返回无效，包含嵌套 typealias 情形 |
| `macro/diagnostics2/interop/callingConventionBoundaryPlaceholder.cj` | CallingConv 合法不代表 CType 参数/返回合法 |

每个 CType 相关文件均使用去除内联标记后的原始源码执行过独立 cjc 探针，未通过修改函数名称、声明结构或源码场景来回避错误。

## 本轮开始时保存的 testcase 基线

基线来自 `cfir/analysis-tests/build/test-results/test` 中时间戳为 `2026-09-13T13:31:27.341Z` 至 `13:31:38.728Z` 的 6 份 XML。它们只覆盖 FFI，不代表全量测试结果。`test-stale-20260901` 不属于此次基线。

两个 suite 前缀分别为：

- LightTree：`org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated`
- PSI：`org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTPsiTestGenerated`

完整 testcase key 由前缀加下表 suffix 构成；两条路径都必须单独记录，不可合并计数。

| testcase key suffix | LightTree | PSI |
| --- | --- | --- |
| `$Ffi#testAllFilesPresent()` | passed | passed |
| `$Ffi#testCpointerGeneric()` | passed | passed |
| `$Ffi#testCpointerExpectedInference()` | passed | passed |
| `$Ffi#testCfuncConstructor()` | passed | passed |
| `$Ffi#testCTypeInGenerictype()` | passed | passed |
| `$Ffi#testCpointerGenericReverse()` | failed | failed |
| `$Ffi#testCpointerGenericReverse2()` | failed | failed |
| `$Ffi#testCTypeSubtype()` | failed | failed |
| `$Ffi#testInferTypeWithOption()` | failed | failed |
| `$Ffi$Bugfix1#testAllFilesPresent()` | passed | passed |
| `$Ffi$Bugfix1#testCFfi()` | failed | failed |
| `$Ffi$ImportCpointerGeneric#testAllFilesPresent()` | passed | passed |
| `$Ffi$ImportCpointerGeneric#testB()` | passed | passed |

合计 26 tests / 10 failures；每条路径 13 tests / 5 failures，包含 3 个目录覆盖检查。当前不能沿用早期的 34/12 统计。

## 其他已补测试入口

- `common/test/org/cangnova/cangjie/annotations/BuiltInAnnotationRegistryTest.kt`：26 个语言源码名称、24 个官方 kind、3 个系统/表达式身份、共享 Overflow kind 的独立策略、Java 特殊身份、ConstSafe 的 std 模块边界、十类 target、Deprecated/外部名称参数 schema、FFI 互斥身份及特殊语义 owner。
- `psi/test/org/cangnova/cangjie/psi/ForeignAndAnnotationParsingTest.kt`：foreign token 和容器归属、23 种声明 annotation 参数形态、@/@! 来源、保留名称 @! 拒绝、annotation lambda、NonProduct features directive、IfAvailable 独立宏表达式。
- PSI/LT Raw golden 共用 `cfir/raw-cfir/psi2cfir/testData/rawBuilder`，入口为 `RawCfirBuilderTestCaseGenerated`、`LightTree2CfirConverterTestGenerated`、`TreesCfirCompareTest`；生成器分别为 `TestGeneratorForPsi2Cfir` 与 `TestGeneratorForLightTree2Cfir`。
- 语义生成器为 `TestGeneratorForCfirAnalysisTests`；旧 annotation/interop 测试大量位于 `testData/macro`，需要单独选择 `CfirAnalysisMacroTestGenerated` 和 `CfirAnalysisMacroPsiTestGenerated`。

## 尚不能计入完成的验收项

- CPointer 错误构造参数、命名参数、超量参数和未知泛型的独立官方诊断；CFunc 的零/多参数、非 CPointer 参数以及非函数类型参数。这些官方探针已放在 `tmp/ffi-annotation-contract-probes`，应在诊断身份进入正式定义后接入对应 marker，不能用其他诊断桶代替。
- annotation 每个 Registry entry 的完整参数/目标/组合负例；自定义 const constructor 的位置/命名/默认值映射、非 const initializer、const evaluator 和重复参数；When/Overflow/Intrinsic/ConstSafe/mock 的实际消费者结果。
- Raw CFIR 的全部 annotation、compile-time 来源及 foreign 声明快照。
- CJO annotation/ABI/version 能力 round-trip、旧 CJO 未知状态、synthetic declaration/typealias/callable reference 的 interop 状态复制契约。
- Analysis API 的 resolved annotation arguments、ABI、calling convention、外部名称、deprecation/availability 与 CFIR 同源结果；当前 `AnalysisApiCfirComponentExecutionTest.cInteropInfo` 仍保留 foreign `backends.isEmpty()` 的旧断言，需要随新契约修订。
- 全量测试及按 testcase key 的 `fixed`、`regressed`、`new`、`removed`、`unchanged failures` 分类。目录里增加了 fixture 不等于其对应实现已经通过验证。
- 原 OpenSpec 台账写 354 个诊断，但此次工作开始的 `HEAD:CfirDiagnosticsList.kt` 按实际 `val NAME by error|warning|deprecationError` 定义已有 487 个唯一名称。新增 FFI 诊断后应重新取完整集合回填，而不是继续使用 354 作验收分母。
