package org.cangnova.cangjie.cfir.builder

import java.io.File

/**
 * 对齐 Kotlin 的 lazy bodies 测试类别。
 */
abstract class AbstractRawCfirBuilderLazyBodiesTestCase : AbstractRawCfirBuilderTestCase() {
    /**
     * 执行 lazy bodies raw CFIR golden 测试。
     */
    override fun doRawCfirTest(filePath: String) {
        val resolvedFilePath = resolveTestDataPath(filePath).path
        val file = createFileForLazyMode(resolvedFilePath)
        val cfirFile = file.toCfirFile(bodyBuildingMode = BodyBuildingMode.LAZY_BODIES)
        val dump = dumpCfirFile(cfirFile)
        val expected = File(resolvedFilePath.replace(".cj", ".lazyBodies.txt"))
        assertEqualsToFile(expected, dump)
    }

    /**
     * 创建 lazy bodies 测试使用的仓颉 PSI 文件。
     */
    protected open fun createFileForLazyMode(filePath: String): org.cangnova.cangjie.psi.CjFile {
        val sourceText = loadFile(filePath)
        return createPsiFile(File(filePath).nameWithoutExtension, sourceText) as org.cangnova.cangjie.psi.CjFile
    }
}
