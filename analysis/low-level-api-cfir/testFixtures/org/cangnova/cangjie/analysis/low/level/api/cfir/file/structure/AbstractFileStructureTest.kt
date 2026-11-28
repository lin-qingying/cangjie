package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.TargetSourceTestSupport
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
    /**
     * 生成包含 FileStructureElement 注释的文件文本并与 golden 比较。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fileStructure = mainFile.getFileStructure()
        val allStructureElements = fileStructure.getAllStructureElements()
        val cfirFile = checkNotNull(mainFile.getResolvableSessionForTest().moduleComponents.cache.getCachedCfirFile(mainFile)) {
            "The file structure must belong to a cached CFIR file."
        }
        val sources = TargetSourceTestSupport(cfirFile, mainFile)
        // CFIR 声明始终持有实际 Parsed 目标树；渲染 key 必须通过有效产品双向验明唯一原对象。
        // 文件根也走同一对象对应，缺失 source 或对应关系不能改用 mainFile。
        val elementsToStructureElementMap = buildMap<PsiElement, FileStructureElement> {
            allStructureElements.forEach { structureElement ->
                val target = checkNotNull(structureElement.declaration.psi) {
                    "Every structure element, including the file root, must have a target PSI anchor."
                }
                val original = sources.originalElement(target)
                check(put(original, structureElement) == null) {
                    "Each original PSI anchor must identify exactly one structure element."
                }
            }
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

    /**
     * 为结构元素创建插入到源码中的块注释。
     */
    private fun FileStructureElement.createComment(): String = """/* ${this::class.simpleName} */"""

    /**
     * 从当前文件所属可解析 session 中读取 [FileStructure]。
     */
    private fun CjFile.getFileStructure(): FileStructure {
        val session = getResolvableSessionForTest()
        return session.moduleComponents.fileStructureCache.getFileStructure(this)
    }
}

/**
 * source 配置下的文件结构测试基类。
 */
abstract class AbstractSourceFileStructureTest : AbstractFileStructureTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
