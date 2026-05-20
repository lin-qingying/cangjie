package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic.AbstractSourceCfirContextCollectionTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic.AbstractSourceDiagnosticTraversalCounterTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.AbstractSourceFileStructureTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.resolve.AbstractSourceWholeFileResolvePhaseTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("analysis/low-level-api-cfir/tests-gen", "analysis/low-level-api-cfir/testData") {
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
        }
    }
}
