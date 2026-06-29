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

/**
 * 将 compiler 侧默认导入 provider 适配为 Analysis API 默认导入模型的基础实现。
 */
@CaImplementationDetail
abstract class CaBaseDefaultImportsProvider : CaDefaultImportsProvider {
    /**
     * 按 compiler default-import provider 缓存转换后的 Analysis API 默认导入集合。
     */
    private val cache =
        ConcurrentHashMap<DefaultImportsProvider, CaDefaultImports>(
            6 ,
            1.0f
        )

    /**
     * 返回当前宿主使用的 compiler 默认导入 provider。
     */
    protected abstract fun getCompilerDefaultImportsProvider( ): DefaultImportsProvider

    /**
     * 当前宿主可见的默认导入集合。
     */
    override val defaultImports: CaDefaultImports
        get() {
            val firDefaultImportsProvider = getCompilerDefaultImportsProvider( )
            return cache.getOrPut(firDefaultImportsProvider) { createDefaultImports(firDefaultImportsProvider) }

        }


    /**
     * 从 compiler provider 创建 Analysis API 默认导入值对象。
     */
    private fun createDefaultImports(firDefaultImportsProvider: DefaultImportsProvider): CaDefaultImportsImpl = CaDefaultImportsImpl(
        defaultImports = getCaDefaultImports(firDefaultImportsProvider),
        excludedFromDefaultImports = firDefaultImportsProvider.excludedImports.map {
            ImportPath(
                it,
                isAllUnder = false
            )
        }
    )

    /**
     * 将 compiler 默认导入拆分为 Analysis API 高/低优先级导入项。
     */
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
