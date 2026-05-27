package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.createDeclarationProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration

/**
 * 为没有源码 PSI 的 synthetic CFIR 声明恢复可导航 PSI。
 *
 * Kotlin 在 analysis-api-fir 层通过 `FirSyntheticFunctionInterfaceSourceProvider`
 * 把 function interface / invoke 的 synthetic FIR 映射回 builtins PSI。
 * 仓颉当前对齐到同一层 owner：primitive type declaration 没有 `source`，
 * 但 IDE 引用、文档与导航仍然需要回到 builtins 的 decompiled PSI。
 */
internal object CfirSyntheticDeclarationSourceProvider {
    fun findPsi(
        declaration: CfirDeclaration,
        scope: GlobalSearchScope,
        preferredProject: Project? = null,
    ): PsiElement? {
        return when (declaration) {
            is CfirPrimitiveTypeDeclaration -> provideSourceForPrimitiveType(declaration, scope, preferredProject)
            else -> null
        }
    }

    private fun provideSourceForPrimitiveType(
        declaration: CfirPrimitiveTypeDeclaration,
        scope: GlobalSearchScope,
        preferredProject: Project?,
    ): PsiElement? {
        val project = preferredProject ?: return null
        val ownerModule = (declaration.moduleData as LLCfirModuleData).caModule

        return project.createDeclarationProvider(scope, ownerModule)
            .getAllClassesByClassId(declaration.symbol.classId)
            .firstOrNull { typeStatement ->
                typeStatement.containingCjFile.isCompiled
            }
            ?.restoreCurrentCompiledPsi(project)
    }
}
