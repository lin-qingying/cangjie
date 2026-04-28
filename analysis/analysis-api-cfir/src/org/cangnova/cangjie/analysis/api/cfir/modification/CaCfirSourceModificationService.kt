package org.cangnova.cangjie.analysis.api.cfir.modification

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.modification.CaElementModificationType
import org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationLocality
import org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirDeclarationModificationService

/**
 * 对齐 Kotlin `KaFirSourceModificationService`。
 *
 * Analysis API 平台层只暴露统一的源码修改服务接口，
 * 真正的局部/非局部失效判定与 file-structure 失效逻辑仍然完全留在 low-level CFIR 中。
 */
@OptIn(LLCfirInternals::class, CaPlatformInterface::class)
internal class CaCfirSourceModificationService(
    private val project: Project,
) : CaSourceModificationService {
    override fun detectLocality(
        element: PsiElement,
        modificationType: CaElementModificationType,
    ): CaSourceModificationLocality {
        return LLCfirDeclarationModificationService.getInstance(project).detectLocality(element, modificationType)
    }

    override fun handleInvalidation(
        element: PsiElement,
        modificationLocality: CaSourceModificationLocality,
    ) {
        LLCfirDeclarationModificationService.getInstance(project).handleInvalidation(element, modificationLocality)
    }

    override fun ancestorAffectedByInBlockModification(element: PsiElement): PsiElement? {
        return LLCfirDeclarationModificationService.getInstance(project).ancestorAffectedByInBlockModification(element)
    }
}
