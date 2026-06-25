package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.psi.CjFile

/**
 * 基于 AST 文件的 lazy bodies raw CFIR 构建测试基类。
 */
abstract class AbstractRawCfirBuilderLazyBodiesByAstTest : AbstractRawCfirBuilderLazyBodiesTestCase() {
    /**
     * 创建强制拥有 AST tree 的测试文件。
     */
    override fun createFileForLazyMode(filePath: String): CjFile {
        val text = loadFile(filePath)
        val file = createPsiFile(java.io.File(filePath).nameWithoutExtension, text) as CjFile
        file.calcTreeElement()
        checkNotNull(file.treeElement) { "Ast tree for file must not be null" }
        return file
    }
}
