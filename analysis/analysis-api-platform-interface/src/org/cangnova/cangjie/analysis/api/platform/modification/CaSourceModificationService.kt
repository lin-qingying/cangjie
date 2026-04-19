package org.cangnova.cangjie.analysis.api.platform.modification

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaEngineService

/**
 * [CaSourceModificationService] is an **engine service** which handles cache invalidation after source code changes.
 */
@CaPlatformInterface
interface CaSourceModificationService : CaEngineService {
    /**
     * Classifies the modification of [element] and its [modificationType] in terms of [CaSourceModificationLocality].
     */
    fun detectLocality(element: PsiElement, modificationType: CaElementModificationType): CaSourceModificationLocality

    /**
     * Handles the cache invalidation for [element]'s modification based on the detected [modificationLocality].
     */
    fun handleInvalidation(element: PsiElement, modificationLocality: CaSourceModificationLocality)

    /**
     * Returns the farthest ancestor [PsiElement] of [element] which would be affected by an in-block modification to [element], or `null`
     * if it's uncertain.
     */
    fun ancestorAffectedByInBlockModification(element: PsiElement): PsiElement?

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CaSourceModificationService = project.service()
    }
}

/**
 * Detects the modification locality of [element] and handles the corresponding cache invalidation.
 */
@CaPlatformInterface
fun CaSourceModificationService.handleElementModification(element: PsiElement, modificationType: CaElementModificationType) {
    val modificationLocality = detectLocality(element, modificationType)
    handleInvalidation(element, modificationLocality)
}
