package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.psi.CjFile

abstract class AbstractRawCfirBuilderLazyBodiesByAstTest : AbstractRawCfirBuilderLazyBodiesTestCase() {
    override fun createFileForLazyMode(filePath: String): CjFile {
        val text = loadFile(filePath)
        val file = createPsiFile(java.io.File(filePath).nameWithoutExtension, text) as CjFile
        file.calcTreeElement()
        checkNotNull(file.treeElement) { "Ast tree for file must not be null" }
        return file
    }
}
