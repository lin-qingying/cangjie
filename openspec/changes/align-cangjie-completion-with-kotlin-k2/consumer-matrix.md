# G0 补全消费矩阵与兼容基线

2026-10-03 依据 `source-baseline.json` 的 85 项指纹及本地源码重新核对。该文档记录静态消费关系与实测制品归属，**不是功能或平台兼容通过报告**。路径 `M` 指实施上游工作树，`K`/`P` 分别指原主检出的 `external/kotlin`/`external/kotlin-plugin`，`H` 指本宿主工作树。

## 公共能力分类

| 编号 | 消费能力 | 实施前结论 |
|---|---|---|
| S | 位置、导入、隐式 receiver scope | 文件/包/成员 API 和 LL ContextCollector 已有；位置 context、kind/owner/次序、importing/composite/static 公共契约缺失 |
| T | 实例化类型及签名 | 签名 API 已有；旧 `CaType.scope` 丢具体实参，连续非空替换抛错，prop 被强转为 `CfirVariableSymbol` |
| X | extend 适用性 | 带约束 compiler matcher 已有；公开 checker/result 缺失；三态可达性不能替代 |
| V | 权限、遮蔽、可达性、导入 | 组件已有但受限：receiver 未进实际访问规则，短名仅查文件 scope；alias/shadow/插入前目标复验需补 |
| R | 调用候选和参数映射 | `resolveToCall()`、成功 mapping、`CaErrorCallInfo.candidateCalls` 已有；错误调用恢复只限本次解析保留候选，不等于 Kotlin 全候选枚举 |
| E | expected type | 已有接口仅处理成功调用实参与 return；typed initializer/赋值/条件/默认值/分支/错误调用需补 |
| D | 副本及生命周期 | dangling module、LL mode/cache 已有；作用域覆盖和 analyzeCopy 缺失，unstable AA cache/修改戳须验证 |
| N | 名称发现 | declaration/package/stub provider 已有；受 analysisScope 限定的名称发现→符号转换需补，禁止全项目 PSI 扫描 |
| F | 符号关系、类型、渲染、pointer | 组件已有；补全实际消费、二进制、生命周期与失效待验证 |

## Kotlin 固定列表：25 个实例

来源：`P/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/Completions.kt:55–112`。语义贡献者放上游 `completion/impl-cfir`，语法/共用编辑处理放 `impl-shared`；跨模块服务放 `api`。不会给每个内部贡献者注册公共 EP。

| Kotlin contributor（省略 K2 前缀） | 依赖 | 仓颉处理及验收 |
|---|---|---|
| ClassifierCompletionContributor | S/N/V/E/F | 类型/类型参数/typealias，已导入与未导入分组，排除 Java/companion 分支 |
| ClassReferenceCompletionContributor | — | `::class`/`::class.java` 形式不适用，不误称普通类型引用补全 |
| KDocParameterNameContributor | T/F | CDoc 参数/类型参数标签，排除已填写参数 |
| DeclarationFromOverridableMembersContributor | T/V/F | 复用 override-implement 的窄生成接口；不搬 Kotlin 主构造 `override val` |
| ActualDeclarationContributor | — | expect/actual 不适用，不因存在对位 API 就假定语言语义 |
| WhenWithSubjectConditionContributor | S/T/V/E | 替换为 match/enum 模式，无参/有参/同名不同 arity 保留 |
| SuperEntryContributor | T/V/F | 保留仓颉 super/父类型场景；不照搬 `super<Type>` 插入 |
| OperatorNameCompletionContributor | PSI | 仓颉 `operator func +` 等，不插入 Kotlin `plus` 名称 |
| TypeParameterConstraintNameInWhereClauseCompletionContributor | F | where 参数名及仓颉约束形式 |
| SameAsFileClassifierNameCompletionContributor | PSI | 同文件名仅是建议，不是语言强制规则 |
| PackageCompletionContributor | S/N/V | package/import、组织名前缀、alias 与表达式名字分离 |
| NamedArgumentCompletionContributor | R/T/E/D | 仅命名参数可用，插入 `name:`，排除已用实参 |
| DeclarationFromUnresolvedNameContributor | S/F | 声明名建议，允许局部引用分析而非全工作区 PSI 候选扫描 |
| KeywordCompletionContributor | PSI/S/E/F | 位置过滤，this/return/super/override 结合语义；无 Kotlin 专属词 |
| ImportDirectivePackageMembersCompletionContributor | S/T/V | import 成员候选，插入标识符而非函数括号 |
| SuperMemberCompletionContributor | T/V/F | 父类实例化签名/provenance，排除无关全局候选 |
| TrailingFunctionParameterNameCompletionContributorBase.All | R/T/E/D | 仓颉支持尾随 lambda，保留参数名建议；使用 `=>` |
| TrailingFunctionParameterNameCompletionContributorBase.Missing | R/T/E/D | 补缺失参数，排除已填项；不搬 implicit it/componentN/`->` |
| MultipleArgumentContributor | R/S/T/E | 合法实参组合，按类型和位置/命名规则校验 |
| CallableCompletionContributor | S/T/X/V/N/E/D/F | 局部/参数/成员/顶层/extend/enum；不搬 Java property/SAM/companion |
| CallableReferenceCompletionContributor | S/T/V/E | `::foo` 形式不适用，普通函数值候选仍须实现 |
| InfixCallableCompletionContributor | — | Kotlin infix 不适用；仓颉 flow 操作符按独立函数值上下文处理 |
| KDocCallableCompletionContributor | S/T/V/N | CDoc 链接，按引用插入而非普通调用 |
| VariableOrParameterNameWithTypeCompletionContributor | S/N/T/F | 变量/参数名称和类型建议，使用仓颉声明语法 |
| TypeInstantiationContributor | E/T/N/F | SMART 构造/实例化，检查构造器访问与类型替换，无 JVM/SAM/object 特例 |

### 固定列表之外

- `KotlinChainCompletionContributor`/`K2ChainCompletionContributor`：属于 B8；精确名称恢复未解析/未导入 receiver，再复用普通成员补全，独立预算与取消。不做任意深度表达式搜索。
- Kotlin XML 的独立 **command completion**（rename、导航、重构、格式化等 IDE 命令）不是上述语言候选管线。本设计聚焦仓颉语言候选、声明生成与编辑事务，不移植整个 IDE 命令面板；不把它归为“仓颉语义不适用”。若后续纳入，应另立命令入口/API baseline/消费者规格。
- 尾随 lambda 的官方依据：`external/cangjie_docs/docs/dev-guide/source_zh_cn/function/lambda.md:7,22–31`。函数值及 flow 依据同目录 `function_call_desugar.md`；运算符依据 `operator_overloading.md`。
- 宏名称发现必须使用已有 `getTopLevelMacros` 等实际入口；旧 file scope 只分派 named function/property，不能假设宏已被覆盖。

## 旧 API 调用者与兼容策略（1.5）

| 能力 | 实际生产调用者 | 保留策略与窄回归 |
|---|---|---|
| file/member/type scope | `lsp/.../AnalysisApiCangjieAnalysisFacade.kt`；`code-insight/override-implement/.../GenerateMembersHandler.kt:collectImplementationRelevantCallables`；`analysis/symbol-light-declarations/.../CaSymbolLightDeclarationProvider.kt`；`CaRendererBodyMemberScopeProvider`；`CaCfirMemberSymbolPointers`/`CaCfirPublicSymbolRestore` | file scope 仍是本文件声明，旧 `CaType.scope: CaScope?` 保留；新 use-site 类型签名先给 completion 使用。回归共享 scopeProvider suites、LSP、生成成员、pointer |
| 三态/shortener/import optimizer | `CaCfirImportPlanning.checkCompletionCandidate` → `collectReferenceShorteningPlan` → `collectImportOptimizationPlan`；range/element command 为全文件 plan 投影 | 保留 DIRECT/REQUIRES_IMPORT/HIDDEN API，新增 X 独立接口。回归 `AnalysisApiCfirComponentExecutionTest.completionCandidateDecisions`、`AnalysisApiImportGroupReferenceTest`、共享 references suites |
| signature | `CaCfirResolver` 调用签名与 candidate substitution；`CaSymbolByCfirBuilder`；light declaration provider；间接 LSP signature help | 保留 function/variable/enum constructor 精确族及声明身份；补连续替换/property/receiver 泛型。现有共享 `AbstractSignatureSubstitutionTest` 只测一次顶层函数替换，不够证明 A3 |
| dangling | platform provider、AA/LL session cache、designation、private-visible extension、session invalidator、symbol relation/visibility、host module chooser/state | 保留默认模式；覆盖按文件/线程隔离且 finally 恢复。回归 `ContextCollectorTest` 原件/副本、共享 dangling diagnostics、宿主 `CaIdeDanglingFileProjectStructureTest`、standalone builder |

补充限制：

- `CaReferenceShorteningPlan` 和 `CaReferenceShorteningCommand` 都是 `CaLifetimeOwner`，operations 含 PSI/符号，不能直接缓存到 UI。B7 必须提取稳定范围/版本/身份/pointer，再分析复验。未找到宿主/LSP/refactoring 的完整 shortener 写入消费者，不将现有 plan 当作已经可用的编辑事务。
- `collectReferenceShortenings` 当前使用 selection `intersects`；插入复用不得假定只修改完全包含的表达式。
- scope 现有 `typeScopeQueries.cj` 是非泛型名称断言；signature fixture 是一次替换；dangling 的 `IGNORE_SELF_MODE` 声明未被准备逻辑消费。这些不是目标完整覆盖。
- 测试生成任务已核实为 `generateTestGeneratorTests`（来自 `ProjectTestsConvention.kt`），公共 context bridge 的生成链仍待单独定位，不能用 diagnostics/test generator 替代。

## SDK 制品与 baseline（1.4）

2026-10-03 使用 Python zipfile 检查本机已有 jar，未下载：

| baseline | 类 | 实际制品 |
|---|---|---|
| 上游 `253.29346.379` | CompletionContributor / LookupElement | Maven `com.jetbrains.intellij.platform:analysis:253.29346.379`，缓存 jar SHA 目录 `114ef518900ab79700cc1a1e864b86a796a085` |
| 上游 `253.29346.379` | BasePlatformTestCase | Maven `com.jetbrains.intellij.platform:test-framework:253.29346.379`，缓存 jar SHA 目录 `de6463c679d349e654506acf197c0aa91479343b` |
| 宿主 IU `253.29346.50`（2025.3.1 RC） | CompletionContributor / LookupElement | 解包 SDK `lib/app.jar` |
| 同宿主 | BasePlatformTestCase | 解包 SDK `lib/testFramework.jar` |

宿主解包根为 `C:/Users/lin17/.gradle/caches/9.4.0/transforms/ea58094c144f9911a11b777889e49f2d/transformed/idea-253.29346.50-win`，由该目录 `product-info.json` 交叉确认。六个 classfile 均 major **65（Java 21）**。

- 补全模块平台依赖保持 compileOnly；Completion/Lookup 的 owner 是 `analysis`，不是 `lang`。现有 `intellijCore()` 聚合已间接包含 analysis，但不等于一个 jar 即完整运行闭包。
- 宿主测试继续复用 `testFramework(TestFrameworkType.Platform)`，不将 SDK class 打包进插件。
- 上游与宿主 patch level 不同，实际 ABI/加载/插入仍待执行验证；242/243/251/252 未验证，不宣称兼容。仓颉源码目标与平台运行 JDK 是不同契约，不以项目 JDK17 注释推定平台 classfile 可在 JDK17 运行。
