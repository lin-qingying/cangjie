package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractSessionInvalidationTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.AbstractResolveToCfirSymbolTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.resolve.AbstractSourceLazyDeclarationResolveScopeBasedTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic.AbstractSourceCfirContextCollectionTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic.AbstractSourceDiagnosticTraversalCounterTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.AbstractSourceFileStructureTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.resolve.AbstractSourceWholeFileResolvePhaseTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.AbstractCodeFragmentContextModificationLLCfirSessionInvalidationTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.AbstractGlobalModuleStateModificationLLCfirSessionInvalidationTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.AbstractGlobalSourceModuleStateModificationLLCfirSessionInvalidationTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.AbstractGlobalSourceOutOfBlockModificationLLCfirSessionInvalidationTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.AbstractModuleOutOfBlockModificationLLCfirSessionInvalidationTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.AbstractModuleStateModificationLLCfirSessionInvalidationTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined.AbstractCombinedPackageDelegationSymbolProviderTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

/**
 * 生成 low-level CFIR 相关测试类。
 */
fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("analysis/low-level-api-cfir/tests-gen", "analysis/low-level-api-cfir/testData") {
            testClass<AbstractCfirSourceLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractCfirSourceLazyDeclarationResolveByReferenceTest> {
                model("lazyResolveByReference", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceLazyDeclarationResolveScopeBasedTest> {
                model("lazyResolveScopes", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceFileStructureTest> {
                model("fileStructure", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceDiagnosticTraversalCounterTest> {
                model("fileStructure", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceCfirContextCollectionTest> {
                model("fileStructure", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceWholeFileResolvePhaseTest> {
                model("fileStructure", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceClassIdTest> {
                model("classId", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceGetOrBuildCfirTest> {
                model("getOrBuildCfir", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractSourceFileBasedCangJieDeclarationProviderTest> {
                model("fileBasedDeclarationProvider", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractResolveToCfirSymbolTest> {
                model("resolveToCfirSymbol", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractCompilationPeerAnalysisTest> {
                model("compilationPeers", pattern = """^(.+)\.cj$""")
            }

            testClass<AbstractContextCollectorSourceTest> {
                model(
                    "contextCollector",
                    pattern = """^(.+)\.cj$""",
                    excludedPattern = """^matchPatternBindingBranchScopes(InCopiedFile)?\.cj$""",
                )
            }

            testClass<AbstractCombinedPackageDelegationSymbolProviderTest> {
                model("symbolProviders/combinedPackageDelegationSymbolProvider", pattern = """^(.+)\.cj$""")
            }
        }

        testGroup("analysis/low-level-api-cfir/tests-gen", "analysis/analysis-api/testData") {
            testClass<AbstractModuleStateModificationLLCfirSessionInvalidationTest> {
                model(
                    "sessions/sessionInvalidation",
                    pattern = """^(.+)\.cj$""",
                    excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES,
                )
            }

            testClass<AbstractModuleOutOfBlockModificationLLCfirSessionInvalidationTest> {
                model(
                    "sessions/sessionInvalidation",
                    pattern = """^(.+)\.cj$""",
                    excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES,
                )
            }

            testClass<AbstractGlobalModuleStateModificationLLCfirSessionInvalidationTest> {
                model(
                    "sessions/sessionInvalidation",
                    pattern = """^(.+)\.cj$""",
                    excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES,
                )
            }

            testClass<AbstractGlobalSourceModuleStateModificationLLCfirSessionInvalidationTest> {
                model(
                    "sessions/sessionInvalidation",
                    pattern = """^(.+)\.cj$""",
                    excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES,
                )
            }

            testClass<AbstractGlobalSourceOutOfBlockModificationLLCfirSessionInvalidationTest> {
                model(
                    "sessions/sessionInvalidation",
                    pattern = """^(.+)\.cj$""",
                    excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES,
                )
            }

            testClass<AbstractCodeFragmentContextModificationLLCfirSessionInvalidationTest> {
                model(
                    "sessions/sessionInvalidation",
                    pattern = """^(.+)\.cj$""",
                    excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES,
                )
            }
        }
    }
}
