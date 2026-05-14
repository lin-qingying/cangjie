package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolvableSessionForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 可视化 source file 对应的 [FileStructure] 插入点。
 */
abstract class AbstractFileStructureTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fileStructure = mainFile.getFileStructure()
        val allStructureElements = fileStructure.getAllStructureElements()
        val elementsToStructureElementMap = allStructureElements.associateBy { structureElement ->
            structureElement.declaration.psi ?: mainFile
        }
        val elementToComments = elementsToStructureElementMap.entries.fold(
            initial = linkedMapOf<PsiElement, MutableList<String>>(),
        ) { map, (anchorElement, structureElement) ->
            val specialKey: PsiElement? = when (anchorElement) {
                is CjTypeStatement -> anchorElement.body?.lBrace
                is CjNamedFunction -> anchorElement.bodyBlockExpression?.lBrace ?: anchorElement.typeReference
                is CjProperty -> anchorElement.typeReference
                is CjTypeAlias -> anchorElement.getTypeReference()
                is CjFile -> anchorElement.packageDirective ?: anchorElement.importList
                is CjDeclaration -> null
                else -> error("Unsupported declaration ${anchorElement::class.simpleName}")
            }

            map.apply {
                getOrPut(specialKey ?: anchorElement) { mutableListOf() } += structureElement.createComment()
            }
        }

        val anchorElements = elementsToStructureElementMap.keys.toMutableSet()
        val text = buildString {
            mainFile.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    anchorElements -= element

                    if (element is LeafPsiElement) {
                        append(element.text)
                    }

                    element.acceptChildren(this)
                    elementToComments[element].orEmpty().forEach(this@buildString::append)
                }

                override fun visitComment(comment: PsiComment) {}
            })
        }

        testServices.assertions.assertEqualsToFile(testDataPath.toFile(), text)

        if (anchorElements.isNotEmpty()) {
            error(
                "An anchor element is not found in the file:\n" +
                    anchorElements.joinToString(separator = "\n") { element -> element::class.simpleName.toString() },
            )
        }
    }

    private fun FileStructureElement.createComment(): String = """/* ${this::class.simpleName} */"""

    private fun CjFile.getFileStructure(): FileStructure {
        val session = getResolvableSessionForTest()
        return session.moduleComponents.fileStructureCache.getFileStructure(this)
    }
}

abstract class AbstractSourceFileStructureTest : AbstractFileStructureTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
