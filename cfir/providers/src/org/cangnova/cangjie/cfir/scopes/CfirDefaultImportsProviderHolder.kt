package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.SessionConfiguration

import org.cangnova.cangjie.cfir.session.CfirComposableSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.resolve.DefaultImportsProvider


sealed class CfirDefaultImportsProviderHolder : CfirComposableSessionComponent<CfirDefaultImportsProviderHolder> {

    companion object {
        fun of(provider: DefaultImportsProvider): CfirDefaultImportsProviderHolder {
            return Single(provider)
        }
    }

    class Single(override val provider: DefaultImportsProvider) : CfirDefaultImportsProviderHolder()

    class Composed(
        override val components: List<CfirDefaultImportsProviderHolder>
    ) : CfirDefaultImportsProviderHolder(), CfirComposableSessionComponent.Composed<CfirDefaultImportsProviderHolder> {
        override val provider: DefaultImportsProvider = DefaultImportsProvider.Composed(components.map { it.provider })
    }

    abstract val provider: DefaultImportsProvider

    @SessionConfiguration
    override fun createComposed(components: List<CfirDefaultImportsProviderHolder>): Composed {
        return Composed(components)
    }


}
private val CfirSession.defaultImportsProviderHolder: CfirDefaultImportsProviderHolder by CfirSession.sessionComponentAccessor()
val CfirSession.defaultImportsProvider: DefaultImportsProvider get() = defaultImportsProviderHolder.provider
