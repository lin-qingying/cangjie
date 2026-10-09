# 源码依据与核查范围

本文件为 design.md 中编号的索引。编号仅用于追踪计划依据，不代表实现任务完成。所有结论来自本轮直接阅读或只读核查子任务返回的函数级证据；没有运行编译、测试或 IDE。

路径根：`M` 为主仓库 `D:/code/intellij/cangjie`；`H` 为本宿主工作树；`K` 为主仓库 `external/kotlin`；`P` 为主仓库 `external/kotlin-plugin`。下表由文件存在性检查转换为相对链接，文件指纹记录在 source-baseline.json。

## Kotlin 参考

| 编号 | 根 | 文件 | 行 | 核对内容 |
|---|---|---|---|---|
| K01 | P | [plugin/resources/META-INF/plugin.xml](../../../external/kotlin-plugin/plugin/resources/META-INF/plugin.xml:34) | 34 | K1/K2 条件加载，不以仍保留 K1 文件推定 K2 使用 K1 |
| K02 | P | [plugin/k2/resources/kotlin.plugin.k2.xml](../../../external/kotlin-plugin/plugin/k2/resources/kotlin.plugin.k2.xml:246) | 246 | K2 产品加载 completion module |
| K02a | P | [completion/impl-k2/resources/intellij.kotlin.completion.impl.xml](../../../external/kotlin-plugin/completion/impl-k2/resources/intellij.kotlin.completion.impl.xml:13) | 13 | 平台 contributor、chain、dummy、charFilter 和排序统计注册 |
| K03 | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/KotlinFirCompletionContributor.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/KotlinFirCompletionContributor.kt:43) | 43 | BASIC/SMART、beforeCompletion、provider、重跑和 chain 调用 |
| K04 | P | [frontend-independent/src/org/jetbrains/kotlin/idea/util/positionContext/KotlinPositionContext.kt](../../../external/kotlin-plugin/frontend-independent/src/org/jetbrains/kotlin/idea/util/positionContext/KotlinPositionContext.kt:279) | 279 | 真实位置分类函数 |
| K04a | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/KotlinFirCompletionParameters.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/KotlinFirCompletionParameters.kt:138) | 138 | 原始/修正参数与 completion file |
| K04b | P | [completion/impl-shared/src/org/jetbrains/kotlin/idea/completion/implCommon/AbstractCompletionDummyIdentifierProviderService.kt](../../../external/kotlin-plugin/completion/impl-shared/src/org/jetbrains/kotlin/idea/completion/implCommon/AbstractCompletionDummyIdentifierProviderService.kt:87) | 87 | 共享 dummy 修正流程 |
| K05 | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/Completions.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/Completions.kt:55) | 55 | 内部列表、位置筛选、section 注册 |
| K06 | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/K2CompletionContributor.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/K2CompletionContributor.kt:264) | 264 | 内部抽象类及 setup/section 契约，不是公共 EP |
| K06a | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/K2CompletionRunner.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/K2CompletionRunner.kt:355) | 355 | 串行 session；并行入口在 455 附近，优先级和取消 |
| K07 | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/lookups/factories/KotlinFirLookupElementFactory.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/lookups/factories/KotlinFirLookupElementFactory.kt:70) | 70 | 按公开符号/签名分派 lookup |
| K07a | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/lookups/factories/FunctionLookupElementFactory.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/lookups/factories/FunctionLookupElementFactory.kt:187) | 187 | 插入策略；452 附近参数/import/缩短处理 |
| K08 | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/weighers/Weighers.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/weighers/Weighers.kt:318) | 318 | 分析期权重及平台 sorter 锚点；219 附近取位置 scope |
| K09 | K | [analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaCompletionCandidateChecker.kt](../../../external/kotlin/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaCompletionCandidateChecker.kt:21) | 21 | 原件/副本/receiver 的扩展适用性契约 |
| K09a | K | [analysis/analysis-api-fir/src/org/jetbrains/kotlin/analysis/api/fir/components/KaFirCompletionCandidateChecker.kt](../../../external/kotlin/analysis/analysis-api-fir/src/org/jetbrains/kotlin/analysis/api/fir/components/KaFirCompletionCandidateChecker.kt:45) | 45 | lazy checker、single candidate、substitutor、隐式 receiver |
| K10 | K | [analysis/analysis-api-fir/src/org/jetbrains/kotlin/analysis/api/fir/components/KaFirExpressionTypeProvider.kt](../../../external/kotlin/analysis/analysis-api-fir/src/org/jetbrains/kotlin/analysis/api/fir/components/KaFirExpressionTypeProvider.kt:266) | 266 | expectedType 多种上下文分派 |
| K11 | K | [analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaScopeProvider.kt](../../../external/kotlin/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaScopeProvider.kt:245) | 245 | scopeContext/importing context/kind/implicit receiver |
| K11a | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/contributors/helpers/utils.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/contributors/helpers/utils.kt:141) | 141 | 类型 scope 的 callable signatures 消费 |
| K12 | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/ImportStrategyDetector.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/ImportStrategyDetector.kt:58) | 58 | callable/classifier 导入策略 |
| K12a | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/lookups/ImportStrategy.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/lookups/ImportStrategy.kt:15) | 15 | 导入策略模型与已有导入判断 |
| K13 | P | [completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/KotlinChainCompletionContributor.kt](../../../external/kotlin-plugin/completion/impl-k2/src/org/jetbrains/kotlin/idea/completion/impl/k2/KotlinChainCompletionContributor.kt:23) | 23 | 未解析 receiver 的精确名称恢复，不等于任意深度链搜索 |
| K13a | K | [analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/projectStructure/danglingFiles.kt](../../../external/kotlin/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/projectStructure/danglingFiles.kt:194) | 194 | 线程局部模式覆盖及 finally 恢复 |

## Analysis 现有契约与缺口

| 编号 | 根 | 文件 | 行 | 核对内容 |
|---|---|---|---|---|
| A01 | M | [analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/components/CaScopeProvider.kt](../../../analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/components/CaScopeProvider.kt:28) | 28 | 现有公开查询，没有位置 scope context |
| A02 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/scopes/CaCfirFileScope.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/scopes/CaCfirFileScope.kt:92) | 92 | 只枚举本文件 declarations；不以 KDoc 断言已含 import |
| A02a | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/scopes/CaCfirPackageScope.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/scopes/CaCfirPackageScope.kt:46) | 46 | 包内名称/符号查询和子包枚举 |
| A03 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirScopeProvider.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirScopeProvider.kt:165) | 165 | 具体类型转 class-like 后使用 unsubstituted scope |
| A04 | M | [analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/projectStructure/danglingFiles.kt](../../../analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/projectStructure/danglingFiles.kt:15) | 15 | 现有模式和 explicitModule；不是无副本基础 |
| A04a | M | [analysis/analysis-api-platform-interface/src/org/cangnova/cangjie/analysis/api/platform/projectStructure/CangJieProjectStructureProviderBase.kt](../../../analysis/analysis-api-platform-interface/src/org/cangnova/cangjie/analysis/api/platform/projectStructure/CangJieProjectStructureProviderBase.kt:34) | 34 | 副本/context/fragment 识别和默认模式 |
| A04b | M | [analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/sessions/LLCfirAbstractSessionFactory.kt](../../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/sessions/LLCfirAbstractSessionFactory.kt:486) | 486 | disregardSelfDeclarations 实际接线 |
| A05 | M | [analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/components/CaCompletionCandidateChecker.kt](../../../analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/components/CaCompletionCandidateChecker.kt:18) | 18 | 现有三态检查入口，不含扩展适用性 checker |
| A06 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirImportPlanning.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirImportPlanning.kt:153) | 153 | 可见性/文件可达性/导入路径的三态分派 |
| A07 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirImportPlanning.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirImportPlanning.kt:315) | 315 | 文件 lookup scopes 与短名比较；189/246 处缩短和优化 |
| A08 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirVisibilityChecker.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirVisibilityChecker.kt:55) | 55 | receiver 只作有效性断言，后续独立访问分支 |
| A09 | M | [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/VisibilityUtils.kt](../../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/VisibilityUtils.kt:30) | 30 | compiler 调用的公共 accessibility 适配 |
| A10 | M | [analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/util/ContextCollector.kt](../../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/util/ContextCollector.kt:119) | 119 | 定位、body 上下文与 tower 快照基础 |
| A11 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirExpressionTypeProvider.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirExpressionTypeProvider.kt:90) | 90 | expectedType 当前只分派实参/return；146–162 实际实现 |
| A12 | M | [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirFileLookupScopes.kt](../../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirFileLookupScopes.kt:47) | 47 | 文件/包/显式/星号/默认导入的构造 |
| A13 | M | [cfir/providers/src/org/cangnova/cangjie/cfir/calls/CfirReceivers.kt](../../../cfir/providers/src/org/cangnova/cangjie/cfir/calls/CfirReceivers.kt:382) | 382 | typeToScope/collectTypeScopes，具体 receiver、上界和 builtin 基础 |
| A14 | M | [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendProvider.kt](../../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendProvider.kt:22) | 22 | 目标、成员 owner、包和文件查询 |
| A14a | M | [analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/providers/LLCfirSessionExtendProvider.kt](../../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/providers/LLCfirSessionExtendProvider.kt:118) | 118 | materialize、types 解析及 extend 索引刷新 |
| A15 | M | [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt](../../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt:179) | 179 | 精确匹配与泛型约束；201 处宽松入口不能用于正常候选放行 |
| A16 | M | [analysis/analysis-api-platform-interface/src/org/cangnova/cangjie/analysis/api/platform/declarations/CangJieDeclarationProvider.kt](../../../analysis/analysis-api-platform-interface/src/org/cangnova/cangjie/analysis/api/platform/declarations/CangJieDeclarationProvider.kt:49) | 49 | 已有包内名称、按 ID 查询、包枚举 |
| A16a | M | [analysis/stubs/src/org/cangnova/cangjie/analysis/stubs/CaStubServices.kt](../../../analysis/stubs/src/org/cangnova/cangjie/analysis/stubs/CaStubServices.kt:68) | 68 | 现有包/声明名称快照，不是空接口 |
| A16b | H | [modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/analysisApiPlatform/CaIdeDeclarationProviderFactory.kt](../../../intellij-ide/modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/analysisApiPlatform/CaIdeDeclarationProviderFactory.kt:26) | 26 | 宿主现有文件收集及 file-based provider 接线 |
| A17 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/CaCfirSession.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/CaCfirSession.kt:78) | 78 | 组件组装，不只定义未使用接口 |
| A18 | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/signatures/CaCfirSignatureModel.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/signatures/CaCfirSignatureModel.kt:186) | 186 | 变量签名底层强转；93 附近首次函数替换 |
| A18a | M | [analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/signatures/CaCfirSubstitutedSignatureModel.kt](../../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/signatures/CaCfirSubstitutedSignatureModel.kt:75) | 75 | 已替换签名再做非空替换的限制；127 附近同类限制 |

## 插件与装配

| 编号 | 根 | 文件 | 行 | 核对内容 |
|---|---|---|---|---|
| P01 | M | [code-insight/README.md](../../../code-insight/README.md:3) | 3 | 主仓库拥有共享 code-insight 实现 |
| P02 | M | [code-insight/refactoring/build.gradle.kts](../../../code-insight/refactoring/build.gradle.kts:5) | 5 | 现有模块依赖与平台/测试模式 |
| P03 | M | [repo/gradle-build-conventions/utilities/src/main/kotlin/sourceSets.kt](../../../repo/gradle-build-conventions/utilities/src/main/kotlin/sourceSets.kt:80) | 80 | projectDefault 目录布局 |
| P04 | M | [analysis/cj-references/src/org/cangnova/cangjie/idea/references/CangJieReferenceMutateService.kt](../../../analysis/cj-references/src/org/cangnova/cangjie/idea/references/CangJieReferenceMutateService.kt:91) | 91 | bind 只换短名，不能当完整导入/缩短器 |
| P05 | M | [psi/src/org/cangnova/cangjie/psi/CjPsiFactory.kt](../../../psi/src/org/cangnova/cangjie/psi/CjPsiFactory.kt:502) | 502 | 普通/分组 import 节点构造 |
| P06 | M | [code-insight/override-implement/src/org/cangnova/cangjie/ide/core/overrideImplement/GenerateMembersHandler.kt](../../../code-insight/override-implement/src/org/cangnova/cangjie/ide/core/overrideImplement/GenerateMembersHandler.kt:63) | 63 | pointer、renderer、成员生成与写入的复用点 |
| P07 | M | [prepare/ide-plugin-dependencies/cangjie-frontend-code-insight-for-ide/build.gradle.kts](../../../prepare/ide-plugin-dependencies/cangjie-frontend-code-insight-for-ide/build.gradle.kts:7) | 7 | fat jar 制品的现有范式 |
| P08 | M | [prepare/ide-plugin-dependencies-module/cangjie-frontend-code-insight-for-ide-module/build.gradle.kts](../../../prepare/ide-plugin-dependencies-module/cangjie-frontend-code-insight-for-ide-module/build.gradle.kts:7) | 7 | 源码桥接制品的现有范式 |
| P09 | M | [repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/cangjieIdePublishing.kt](../../../repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/cangjieIdePublishing.kt:21) | 21 | 显式 merge 项目 jar 的行为 |
| H01 | H | [product/idea-plugin/src/main/resources/META-INF/plugin.xml](../../../intellij-ide/product/idea-plugin/src/main/resources/META-INF/plugin.xml:14) | 14 | 实际生产模块描述符聚合 |
| H02 | H | [modules/ide/base/src/main/resources/org.cangnova.cangjie.ide.base.xml](../../../intellij-ide/modules/ide/base/src/main/resources/org.cangnova.cangjie.ide.base.xml:2) | 2 | include 上游 code-insight XML 的位置 |
| H03 | H | [product/idea-plugin/build.gradle.kts](../../../intellij-ide/product/idea-plugin/build.gradle.kts:28) | 28 | 产品统一 runtimeOnly/test 制品依赖 |
| H04 | H | [gradle/libs.versions.toml](../../../intellij-ide/gradle/libs.versions.toml:93) | 93 | 现有 code-insight 工件坐标 |
| H05 | H | [settings.gradle.kts](../../../settings.gradle.kts:35) | 35 | 字面 includeBuild ../，当前工作树父目录无 settings |

## 测试与 LSP 边界

| 编号 | 根 | 文件 | 行 | 核对内容 |
|---|---|---|---|---|
| T01 | M | [TESTING_CONVENTIONS.md](../../../TESTING_CONVENTIONS.md:5) | 5 | 共享测试基座、窄层验证、生成文件约束 |
| T02 | M | [analysis/analysis-api-cfir/test/org/cangnova/cangjie/analysis/api/cfir/test/AnalysisApiCfirComponentExecutionTest.kt](../../../analysis/analysis-api-cfir/test/org/cangnova/cangjie/analysis/api/cfir/test/AnalysisApiCfirComponentExecutionTest.kt:43) | 43 | 三态/导入/缩短断言，不是完整候选集合测试 |
| T03 | M | [analysis/analysis-api/testData/components/expressionTypeProvider/expectedExpressionType/propertyInitializer.cj](../../../analysis/analysis-api/testData/components/expressionTypeProvider/expectedExpressionType/propertyInitializer.cj:7) | 7 | 已有 initializer caret fixture，不能忽略取到的表达式种类 |
| T03a | M | [analysis/analysis-api/testData/components/expressionTypeProvider/expectedExpressionType/propertyInitializer.txt](../../../analysis/analysis-api/testData/components/expressionTypeProvider/expectedExpressionType/propertyInitializer.txt:1) | 1 | 现有 callee 定位 golden，不直接改成 RHS 类型 |
| T03b | M | [analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/cases/components/expressionTypeProvider/AbstractExpectedExpressionTypeTest.kt](../../../analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/cases/components/expressionTypeProvider/AbstractExpectedExpressionTypeTest.kt:23) | 23 | bottommost CjExpression 定位 |
| T04 | M | [analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/dsl/AnalysisApiTestGroup.kt](../../../analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/dsl/AnalysisApiTestGroup.kt:81) | 81 | 共享 testData/model DSL |
| T05 | H | [modules/test-support/src/testFixtures/org/cangnova/cangjie/test/CangJieLightCodeInsightFixtureTestCase.kt](../../../intellij-ide/modules/test-support/src/testFixtures/org/cangnova/cangjie/test/CangJieLightCodeInsightFixtureTestCase.kt:14) | 14 | 宿主 light fixture 及 descriptor |
| T06 | H | [modules/ide/base/src/test/kotlin/org/cangnova/cangjie/ide/CangJieFileIconRegistrationTest.kt](../../../intellij-ide/modules/ide/base/src/test/kotlin/org/cangnova/cangjie/ide/CangJieFileIconRegistrationTest.kt:46) | 46 | 真运行时服务接线测试范例 |
| T07 | H | [product/idea-plugin/src/test/kotlin/org/cangnova/cangjie/ide/quickfix/CangJieQuickFixRegistrationTest.kt](../../../intellij-ide/product/idea-plugin/src/test/kotlin/org/cangnova/cangjie/ide/quickfix/CangJieQuickFixRegistrationTest.kt:40) | 40 | 产品真实扩展/结果/唯一性断言 |
| T08 | H | [modules/test-support/build.gradle.kts](../../../intellij-ide/modules/test-support/build.gradle.kts:41) | 41 | 测试运行时依赖与测试描述符隔离 |
| T09 | P | [completion/tests-shared/test/org/jetbrains/kotlin/idea/completion/test/ExpectedCompletionUtils.kt](../../../external/kotlin-plugin/completion/tests-shared/test/org/jetbrains/kotlin/idea/completion/test/ExpectedCompletionUtils.kt:85) | 85 | 候选存在/缺席/数量/顺序 DSL |
| T09a | P | [completion/tests-shared/test/org/jetbrains/kotlin/idea/completion/test/handlers/CompletionHandlerTestBase.kt](../../../external/kotlin-plugin/completion/tests-shared/test/org/jetbrains/kotlin/idea/completion/test/handlers/CompletionHandlerTestBase.kt:48) | 48 | 实际选择并校验 after 文件 |
| T10 | M | [analysis/low-level-api-cfir/test/org/cangnova/cangjie/analysis/low/level/api/cfir/util/ContextCollectorTest.kt](../../../analysis/low-level-api-cfir/test/org/cangnova/cangjie/analysis/low/level/api/cfir/util/ContextCollectorTest.kt:40) | 40 | 已有原件/副本 match 分支 binding 隔离测试 |
| T10a | M | [analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/cases/components/diagnosticProvider/AbstractDanglingFileCollectDiagnosticsTest.kt](../../../analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/cases/components/diagnosticProvider/AbstractDanglingFileCollectDiagnosticsTest.kt:35) | 35 | 模式指令声明与准备文件实际消费需要核对 |
| L01 | M | [lsp/src/org/cangnova/cangjie/lsp/analysis/AnalysisApiCangjieAnalysisFacade.kt](../../../lsp/src/org/cangnova/cangjie/lsp/analysis/AnalysisApiCangjieAnalysisFacade.kt:185) | 185 | reference variants/文件/工作区回退；909 附近 item 转换 |
| L02 | M | [lsp/test/org/cangnova/cangjie/lsp/server/CangjieSemanticFeatureIntegrationTest.kt](../../../lsp/test/org/cangnova/cangjie/lsp/server/CangjieSemanticFeatureIntegrationTest.kt:29) | 29 | 真实语义测试，但补全只断言部分 label 存在 |

## 仓颉语义依据

| 编号 | 根 | 文件 | 行 | 核对内容 |
|---|---|---|---|---|
| D01 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/extension/direct_extension.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/extension/direct_extension.md:36) | 36 | 完全实例化匹配、泛型与约束 |
| D02 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/enum_and_pattern_match/enum.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/enum_and_pattern_match/enum.md:23) | 23 | 同名不同参数个数、无参/有参和限定访问 |
| D03 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/basic_data_type/strings.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/basic_data_type/strings.md:20) | 20 | 插值和原始字符串规则 |
| D04 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/function/define_functions.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/function/define_functions.md:25) | 25 | 命名参数声明与默认值 |
| D05 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/function/call_functions.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/function/call_functions.md:5) | 5 | 命名实参冒号形式 |
| D06 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/package/import.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/package/import.md:5) | 5 | 组织名前缀、alias 与表达式访问边界 |
| D07 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/Appendix/cjo_artifacts.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/Appendix/cjo_artifacts.md:7) | 7 | .cjo 语义接口工件 |
| D08 | M | [external/cangjie_docs/docs/dev-guide/source_zh_cn/Macro/implementation_of_macros.md](../../../external/cangjie_docs/docs/dev-guide/source_zh_cn/Macro/implementation_of_macros.md:18) | 18 | Tokens 与宏展开阶段 |

## 技能执行说明

读取并采用主仓库 `.claude/skills/openspec-propose/SKILL.md`，版本 1.0，由 OpenSpec 1.2.0 生成。该技能未出现在本会话 Skill 可调用列表，故直接加载本地 SKILL.md，而没有虚构 Skill 调用成功。

按技能使用 openspec-cn new change、status、instructions，先 proposal，再 design/specs，最后 tasks。当前工具集没有 TodoWrite，采用 CLI artifact 状态和最后的验证记录跟踪产物进度；tasks.md 的复选框仅追踪未来实现，未冒充已执行。

核查子任务曾遇到服务端 503，重试后均返回结果。长设计文档经 shell 写入失败且未留下设计文件，随后已使用专用文件工具写入。工具失败不作为证据；最终文件和引用另行验证。

## 未验证边界

- 未证明所有现有 Analysis API 的语义完整性，未作“只缺某一项”的排他断言。
- 未运行 cjc、仓颉 Gradle 编译测试、Kotlin 测试、插件构建或沙箱。官方文档用于计划语义边界，不等于候选 fixture 已通过官方编译。
- 未证明所有承诺平台均支持新版 Kotlin 使用的 Completion API，G0/G5 必须单独核验。
- 文件指纹用于发现计划基线漂移，不证明实现正确；OpenSpec 校验只证明文档结构和需求格式符合 schema。
