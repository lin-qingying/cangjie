package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolRelationProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiSubclassRelationTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedIsDirectSubclass
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedIsSubclass
import org.cangnova.cangjie.analysis.api.impl.base.test.subClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.superClassName
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * subclass relation generated 测试。
 *
 * 该测试族把 `isSubClassOf` / `isDirectSubClassOf` 固定成一条正式能力链，
 * 覆盖直接继承、间接继承、同类比较、无关类，以及后续可扩展到局部类的源码身份判断。
 */
abstract class AbstractIsSubclassOfTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前 subclass relation 测试额外注册的子类、父类和期望关系指令。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiSubclassRelationTestDirectives

    /**
     * 执行 subclass relation 测试。
     *
     * 方法恢复子类与父类 symbol，并分别断言完整继承链和直接继承关系。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val subClassDeclaration = findClassDeclaration(mainModule, directives.subClassName)
        val superClassDeclaration = findClassDeclaration(mainModule, directives.superClassName)

        analyzeForTest(subClassDeclaration) {
            val subClassSymbol = subClassDeclaration.classSymbol
                ?: error("Subclass `${directives.subClassName}` cannot be restored to a class symbol.")
            val superClassSymbol = superClassDeclaration.classSymbol
                ?: error("Superclass `${directives.superClassName}` cannot be restored to a class symbol.")

            assertEquals(
                directives.expectedIsSubclass,
                subClassSymbol.isSubClassOf(superClassSymbol),
                "isSubClassOf 结果不符合预期。",
            )
            assertEquals(
                directives.expectedIsDirectSubclass,
                subClassSymbol.isDirectSubClassOf(superClassSymbol),
                "isDirectSubClassOf 结果不符合预期。",
            )
        }
    }

    /**
     * 在测试模块源码中按名称定位 class-like 声明。
     *
     * 查找要求唯一命中，避免同名声明导致继承关系断言不稳定。
     */
    private fun findClassDeclaration(mainModule: CjTestModule, className: String): CjTypeStatement {
        return mainModule.cjFiles.asSequence()
            .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, CjTypeStatement::class.java).asSequence() }
            .filter { declaration -> declaration.name == className }
            .singleOrNull()
            ?: error("Cannot uniquely locate class declaration `$className` in module `${mainModule.name}`.")
    }
}
