package org.cangjie.cfir.builder

import java.io.File

/**
 * 对齐 Kotlin 的 lazy bodies 测试类别。
 */
abstract class AbstractRawCfirBuilderLazyBodiesTestCase : AbstractRawCfirBuilderTestCase() {
    override fun doRawCfirTest(filePath: String) {
        val file = createFileForLazyMode(filePath)
        val cfirFile = file.toCfirFile(bodyBuildingMode = BodyBuildingMode.LAZY_BODIES)
        val dump = dumpCfirFile(cfirFile)
        val expected = File(filePath.replace(".cj", ".lazyBodies.txt"))
        assertEqualsToFile(expected, dump)
    }

    protected open fun createFileForLazyMode(filePath: String): org.cangnova.cangjie.psi.CjFile {
        val sourceText = loadFile(filePath)
        return createPsiFile(File(filePath).nameWithoutExtension, sourceText) as org.cangnova.cangjie.psi.CjFile
    }
}
