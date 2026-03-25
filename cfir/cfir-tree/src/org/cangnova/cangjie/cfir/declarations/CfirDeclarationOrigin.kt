package org.cangnova.cangjie.cfir.declarations

sealed class CfirDeclarationOrigin(
    private val displayName: String? = null,
    val fromSupertypes: Boolean = false,
    val generated: Boolean = false,
    val fromSource: Boolean = false,
) {
    object Source : CfirDeclarationOrigin(fromSource = true)
    object Library : CfirDeclarationOrigin()

    sealed class Synthetic : CfirDeclarationOrigin(generated = true) {
        data object Default : Synthetic()
        data object FakeFunction : Synthetic()
    }

    object ImplicitDefault : CfirDeclarationOrigin(generated = true)
    object GenericInstantiation : CfirDeclarationOrigin(generated = true)
    object Extension : CfirDeclarationOrigin(generated = true)
    object SamConstructor : CfirDeclarationOrigin(generated = true)

    override fun toString(): String = displayName ?: this::class.simpleName!!
}
