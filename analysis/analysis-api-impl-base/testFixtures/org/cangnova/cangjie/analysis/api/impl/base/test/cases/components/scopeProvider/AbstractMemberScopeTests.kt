package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.combinedDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.components.declaredMemberScope
import org.cangnova.cangjie.analysis.api.components.memberScope
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * `scopeProvider` 成员作用域抽象测试基座。
 *
 * 对齐 Kotlin `AbstractMemberScopeTests.kt` 的 owner：各 bucket 共享一套目标 class-like
 * 解析流程，子类只负责选择公开 scope 入口与读取对应期望。
 */
abstract class AbstractMemberScopeTestBase : AbstractScopeTestBase() {
    context(session: CaSession)
    protected abstract fun getScope(
        classLikeSymbol: CaClassLikeSymbol,
        declarationContainerSymbol: CaDeclarationContainerSymbol,
    ): CaScope

    context(session: CaSession)
    final override fun getScope(mainFile: CjFile, testServices: TestServices): CaScope {
        val module = testServices.cjTestModuleStructure.requireModuleByFile(mainFile)
        val directives = directivesForMainFile(mainFile, module)
        val targetClass = PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java)
            .single { it.name == directives.targetClassName }

        val classSymbol = with(session) { getClassLikeSymbol(targetClass.getClassId()!!) }
        assertNotNull(classSymbol, "目标 class-like 符号应可从 Analysis API 获取。")
        assertTrue(classSymbol is CaDeclarationContainerSymbol, "目标 class-like 符号必须实现 declaration container。")

        val nonNullClassSymbol = classSymbol!!
        return getScope(
            classLikeSymbol = nonNullClassSymbol,
            declarationContainerSymbol = nonNullClassSymbol as CaDeclarationContainerSymbol,
        )
    }
}

abstract class AbstractMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: CaSession)
    override fun getScope(
        classLikeSymbol: CaClassLikeSymbol,
        declarationContainerSymbol: CaDeclarationContainerSymbol,
    ): CaScope = declarationContainerSymbol.memberScope
}

abstract class AbstractDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: CaSession)
    override fun getScope(
        classLikeSymbol: CaClassLikeSymbol,
        declarationContainerSymbol: CaDeclarationContainerSymbol,
    ): CaScope = classLikeSymbol.declaredMemberScope
}

abstract class AbstractCombinedDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: CaSession)
    override fun getScope(
        classLikeSymbol: CaClassLikeSymbol,
        declarationContainerSymbol: CaDeclarationContainerSymbol,
    ): CaScope = declarationContainerSymbol.combinedDeclaredMemberScope
}
