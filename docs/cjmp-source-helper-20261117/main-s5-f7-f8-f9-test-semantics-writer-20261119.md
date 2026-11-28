# mainS5 F7/F8/F9：CangjieTestSemanticsWriter 实际测试交付

唯一编辑树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
本角色仅新增独立 [CjoSourcePublicPayloadTest.kt](../../cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoSourcePublicPayloadTest.kt) 和本报告。未修改生产代码、原 SOURCE18、原 fixture 或其正确期望；未运行 Gradle/cjc。新增 **6 个测试方法，执行 0 个**，不能记为 6/6 通过或关闭 F7/F8/F9。

续轮：main 补齐 API 后，现有 6 项测试已直接用 Kotlin 编译器编译成功、退出码 0，源码 SHA 未变；未运行 JUnit。详见 [API 编译证据](main-s5-f7-f8-f9-api-compile-20261119.md)。本报告的首轮 body0 扫描缺口已由 main 修复，JString 官方资源缺口仍保留。

## 固定依据

- native `14276740ffec1262b81b6b5236b1d840af270b37`，通过共享 external 的 Git 对象只读 `git show SHA:path`：
  - `src/Modules/ASTSerialization/ASTLoader.cpp:763–834`：参数分组、逐参数 desugars 的 0/实际 FuncDecl、参数 default assignment 与 helper 独立。
  - `src/Modules/ASTSerialization/ReferenceLoader.cpp:732–742`：每个 MatchCase 根 pattern 的 ctxExpr 指向所属 selector，不新建 selector。
  - `src/Modules/ASTSerialization/ExprWriter.cpp:233–243`、`ExprLoader.cpp:141–151`、`schema/ModuleFormat.fbs:398–403`：constKind 与四值 StringKind 分别保存/恢复。
  - `src/Sema/Collector.cpp:527–540`、`src/Sema/TypeCheckExpr/LitConstExpr.cpp:139–194`：STRING/JSTRING 分别引用真实 core String/JString；不能把 JString 的类型假造为 Int64 或普通 String。
- Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`，本轮按固定 SHA 读取 `compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrDeclarationDeserializer.kt:522–575`：签名/参数材料化与正文读取政策分开。CJO 参数分组和 StringKind/Pattern 载荷属于 native 语义适配，不声称是 Kotlin 同名 wire 字段。
- docs MCP 本轮读取 `manual_source_zh_cn_strings`、`manual_source_zh_cn_match`：普通、多行、多行原始字符串形态；Match guard 为 Bool，正例须穷尽。两个 Match 输入各有常量 case 和末尾 wildcard case，返回实际 `(Int64, Int64)`。

## 测试装配和断言

直接复用 `CjoTypeGraphDeserializationTest` 的正式装配形式：Writer 写临时真实 CJO；Manager 按真实包头/路径加载；实际 Library session 注册 `CfirDeserializedSymbolProvider`；消费 `CfirImportedSourceGraphProvider` 公共 API。没有测试解析器、空声明 factory、internal decoder 快捷入口或按名字制造 symbol。

| 测试方法 | 实际断言 |
| --- | --- |
| `publicHeaderPreservesBodylessFunctionAndRealHelper` | public FuncBody.body=0；两组参数，保存的 `[[0],[d4]]` helper 槽；d4 为正式 loader 建立的 CfirNamedFunction；normal 默认参数 metadata 与 SOURCE helper 借用同一对象；native retType=0/公开 header.returnType=null，签名返回 Int64；captureKind=1、always=true、bodyAvailable=false。 |
| `publicGraphKeepsHeadersAcrossHeaderFirstAndBodyFirstQueries` | 两种查询顺序下公共 graph.functionHeaders 包含 body0 函数、实际 helper 和 bodyful entry；body0 不在 functionBodies；两份实际 Block/Return/literal 正文与各自函数绑定；重复 provider 查询复用函数、graph 和 e identity；normal CFIR.body 仍为 null。 |
| `headerOnlyQueriesNeverDecodeAnAvailableButInvalidBody` | 只把目标 body 改为越界 e7，现有 e1–e6 statement 保持完整；header 查询及重复查询成功，bodyAvailable=true；显式 SOURCE body 请求传播越界错误；失败后原声明/参数/helper/header 内容继续可读，重复 body 请求仍失败。 |
| `fourStringKindsSurviveThePublicGraph` | NORMAL/JSTRING/MULTILINE/MULTILINE_RAW 完整保留、isJavaString 由 JSTRING 派生；四份相同文本各有独立 e identity；wire 的四值 strKind 再核对；实际 String/JString 类型必须来自官方 core CJO。**当前资源缺 JString，见下方具体输入缺口。** |
| `eachMatchPatternBorrowsItsOwnSelectorAndKeepsItsPayload` | 两个 selector 内容相等、identity 不同；每个根 pattern.ctx 是自己的 selector，ctx 不进入 owning expressionChildren；各 selector 只有一个 owning edge；const slot 不复制/替换 selector；guard/type/常量缓存/位置/runtime flags/零 guard 槽和实际 case Block 保留。 |
| `aRepeatedCaseIdentityWithDifferentSelectorsIsRejected` | 第二个 Match 借用第一个 Match 的同一 case e8，正式 graph 发布必须拒绝双 owning Match/不同 selector 冲突；失败不替换已有函数/header。 |

Header 是数据产品，比较完整字段值；身份断言作用于真实 CfirFunction、参数、helper、SOURCE graph 和 e 节点，不额外要求 header data class 实例不能作防御复制。

每个 Root/FuncBody 的 statement 都通过真实 `CjoExpressionMetadata` 和官方 info union 写出：Block 的 `isExpr` 长度为 operands+1，末尾为 UNSAFE=false；Return 必有一个真实 value；所有表达式具有非零、表内类型。MatchCase 包含 `[guard 或 0, body]` 和实际 Pattern.types/exprs/values。坏 body 负例破坏引用，重复 case 负例破坏拥有关系，不拿 InvalidExpr、缺 BlockInfo 或空 factory 伪造 statement。

## 已核验的输入缺口，交 main

四字符串用例严格读取现有 `cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo`。该资源的来源 README 明确为 SDK 1.0.5；本轮未替换资源。

用已编译的正式 `PackageFormat.Package` reader、Kotlin stdlib 和 FlatBuffers Java 通过 JDK 21 JShell 只读检查该真实文件，成功输出：

```text
String export=_CNat6StringE
SDK allDecls=3120; String=1; JString=0
```

资源 SHA-256：`E02D58B8AF3BA450860C6D14EDD37A927E54DA3C88C037E0C86DFD7BF10351AD`。

因此当前 `fourStringKindsSurviveThePublicGraph` 的 `stringMetadata` 会在获取真实 JString 声明时明确失败，还没进入 F8 graph 验收。main 需提供含真实 JString 的官方 core 输入/资源；不能将 constKind=JString 的类型改成 String/Int64、补一个伪造 core 声明、skip 用例或删掉第四形态。这里报告的是实际输入缺口，不是已经运行的 JUnit 失败。

## 生产 API 落盘观察，交 main

生产代码由 main 同时修改，本报告只描述读取时点，不作为冻结终态：

- 末次读取已见公共 `functionHeaders`、header.bodyAvailable、Literal.stringKind/派生 isJavaString、Pattern.contextExpression。
- 已见接口及具体 provider 的 `sourceHeaderOf`，具体 provider 经 provenance 选择同一 context 的 header 入口，先前“接口尚未出现”的观察已撤销。
- 读取时 `CfirSourceGraphLoader.load()` 仍在函数扫描中以 `body.body == 0u` 跳过函数；公共 graph 验收要求其中 body0 的完整 header 也被发布。独立测试保留 `graph.functionHeaders.getValue(publicHeader)`，没有配合旧过滤减弱期望。
- `sourceHeaderOf` 和 graph header 的参数/helper 必须来自同一 canonical 声明域；独立测试同时检查 flattened normal 参数与完整分组内对象身份。实际正文解码仍仅经正常 provider 的显式 SOURCE 请求。

## 验证范围

已完成新增源码的静态名称/类型对照和 `git diff --no-index --check -- NUL <新增测试文件>` 空白检查；已完成上述正式生成 reader 的 SDK 内容检查。未编译/执行新增测试，未运行 Gradle，未借用原 SOURCE18 通过成绩为这 6 项记通过。F7/F8/F9 运行和最终 production parity 门仍由 main 后续验证。
