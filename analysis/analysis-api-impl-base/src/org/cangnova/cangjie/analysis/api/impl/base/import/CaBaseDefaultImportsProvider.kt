package org.cangnova.cangjie.analysis.api.impl.base.import

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaIdeApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.components.CaDefaultImportsProvider
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImport
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImportPriority
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.resolve.DefaultImportsProvider
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.getOrPut

@CaImplementationDetail
abstract class CaBaseDefaultImportsProvider : CaDefaultImportsProvider {
    private val cache =
        ConcurrentHashMap<DefaultImportsProvider, CaDefaultImports>(
            6 ,
            1.0f
        )

    protected abstract fun getCompilerDefaultImportsProvider( ): DefaultImportsProvider
    override val defaultImports: CaDefaultImports
        get() {
            val firDefaultImportsProvider = getCompilerDefaultImportsProvider( )
            return cache.getOrPut(firDefaultImportsProvider) { createDefaultImports(firDefaultImportsProvider) }

        }


    private fun createDefaultImports(firDefaultImportsProvider: DefaultImportsProvider): CaDefaultImportsImpl = CaDefaultImportsImpl(
        defaultImports = getCaDefaultImports(firDefaultImportsProvider),
        excludedFromDefaultImports = firDefaultImportsProvider.excludedImports.map {
            ImportPath(
                it,
                isAllUnder = false
            )
        }
    )

    @OptIn(CaIdeApi::class)
    private fun getCaDefaultImports(firDefaultImportsProvider: DefaultImportsProvider): List<CaDefaultImport> = buildList {
        firDefaultImportsProvider.getDefaultImports(
            includeLowPriorityImports = false
        ).mapTo(this) { import ->
            CaDefaultImportImpl(ImportPath(import.fqName, import.isAllUnder), CaDefaultImportPriority.HIGH)
        }
        firDefaultImportsProvider.defaultLowPriorityImports.mapTo(this) { import ->
            CaDefaultImportImpl(ImportPath(import.fqName, import.isAllUnder), CaDefaultImportPriority.LOW)
        }
    }
}
