package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName

/** Concrete CJO-backed implementation of [AbstractCfirDeserializedSymbolProvider]. */
class CfirDeserializedSymbolProvider(
    session: CfirSession,
    private val cjoManager: CjoManager,
    cangjieScopeProvider: CfirCangJieScopeProvider,
    libraryModuleData: CfirModuleData,
) : AbstractCfirDeserializedSymbolProvider(
    session = session,
    cangjieScopeProvider = cangjieScopeProvider,
    libraryModuleData = libraryModuleData,
) {

    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirDeserializedSymbolNamesProvider(cjoManager)

    override fun hasPackage(fqName: FqName): Boolean = cjoManager.hasPackage(fqName)

    override fun loadPackageDeserializers(packageFqName: String): PackageDeserializers? {
        val header = cjoManager.loadPackageHeader(packageFqName)
        val pkg = cjoManager.loadPackage(packageFqName)
        if (header == null || pkg == null) return null

        val context = CfirDeserializationContext(
            pkg = pkg,
            header = header,
            moduleData = libraryModuleData,
            cjoManager = cjoManager,
        )
        return PackageDeserializers(header, context)
    }
}
