package org.cangnova.cangjie.analysis.api.platform.modification

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * [CaElementModificationType] describes which kind of modification was applied to a changed [PsiElement]. [CaSourceModificationService]
 * uses this information to perform change locality detection.
 */
@CaPlatformInterface
sealed interface CaElementModificationType {
    /**
     * The element has been added as a new element.
     */
    @CaPlatformInterface
    data object ElementAdded : CaElementModificationType

    /**
     * The element passed is the parent of a removed element, which is additionally provided as [removedElement]. The removed element
     * itself cannot be the modification "anchor" because it has already been removed and is not part of the file anymore, but it might
     * still be used to determine the modification's change type.
     */
    @CaPlatformInterface
    class ElementRemoved(val removedElement: PsiElement) : CaElementModificationType

    /**
     * Which kind of modification was applied to the element is unknown.
     */
    @CaPlatformInterface
    data object Unknown : CaElementModificationType
}
