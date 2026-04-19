package org.cangnova.cangjie.analysis.api.platform.modification

/**
 * [CaSourceModificationLocality] describes the scope of effect of a source modification detected by [CaSourceModificationService].
 */
sealed interface CaSourceModificationLocality {
    /**
     * A change that has no effect on cached information.
     */
    interface Invisible : CaSourceModificationLocality

    /**
     * Whitespace modification covers changes in whitespace and comments.
     */
    interface Whitespace : CaSourceModificationLocality

    /**
     * In-block modification is a source code modification that doesn't affect the state of other non-local declarations.
     */
    interface InBlock : CaSourceModificationLocality

    /**
     * Out-of-block modification is a source code modification that may affect the state of other declarations in the same module and the
     * declarations of dependent modules.
     */
    interface OutOfBlock : CaSourceModificationLocality
}
