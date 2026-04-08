package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSourceModule

/**
 * 指定某个模块在当前 use-site 分析中的解析策略。
 *
 * 该模型直接对齐 Kotlin `LLModuleResolutionStrategy`：
 * `LAZY` 表示模块以可解析源码形态参与当前 low-level session；
 * `STATIC` 表示模块被视作稳定依赖边界，只暴露已固化的依赖语义。
 *
 * 这个区分必须在 low-level 层显式建模，后续 scope、resolver、file-structure cache
 * 才能围绕同一套模块语义继续演进。
 */
internal enum class CaCfirModuleResolutionStrategy {
    LAZY,
    STATIC,
}

/**
 * use-site 视角下的模块解析策略提供器。
 *
 * Analysis API 模块种类相同，并不代表在当前 use-site 下就一定采用相同的解析方式。
 * 例如：
 * source use-site 下，source 依赖仍然是 `LAZY`；
 * script use-site 下，script/source/library-source 都可能保持 `LAZY`；
 * dangling file use-site 需要在上下文模块策略之上额外把自己视作 `LAZY`。
 */
internal fun interface CaCfirModuleResolutionStrategyProvider {
    fun getKind(module: CaModule): CaCfirModuleResolutionStrategy
}

internal class CaCfirSourceModuleResolutionStrategyProvider(
    private val useSiteModule: CaModule,
) : CaCfirModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): CaCfirModuleResolutionStrategy {
        return when (module) {
            is CaSourceModule -> CaCfirModuleResolutionStrategy.LAZY
            is CaBuiltinsModule,
            is CaLibraryModule -> CaCfirModuleResolutionStrategy.STATIC
            else -> cannotProvideResolutionStrategy(module, useSiteModule)
        }
    }
}

internal class CaCfirBinaryModuleResolutionStrategyProvider(
    private val useSiteModule: CaModule,
) : CaCfirModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): CaCfirModuleResolutionStrategy {
        return when {
            module == useSiteModule || module is CaLibrarySourceModule -> CaCfirModuleResolutionStrategy.LAZY
            module is CaBuiltinsModule || module is CaLibraryModule -> CaCfirModuleResolutionStrategy.STATIC
            else -> cannotProvideResolutionStrategy(module, useSiteModule)
        }
    }
}

internal class CaCfirDanglingFileResolutionStrategyProvider(
    private val delegate: CaCfirModuleResolutionStrategyProvider,
) : CaCfirModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): CaCfirModuleResolutionStrategy {
        return when (module) {
            is CaDanglingFileModule -> CaCfirModuleResolutionStrategy.LAZY
            else -> delegate.getKind(module)
        }
    }
}

internal class CaCfirSimpleResolutionStrategyProvider(
    private val useSiteModule: CaModule,
) : CaCfirModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): CaCfirModuleResolutionStrategy {
        return when {
            module == useSiteModule -> CaCfirModuleResolutionStrategy.LAZY
            module is CaSourceModule || module is CaLibrarySourceModule -> CaCfirModuleResolutionStrategy.LAZY
            else -> CaCfirModuleResolutionStrategy.STATIC
        }
    }
}

private fun cannotProvideResolutionStrategy(module: CaModule, useSiteModule: CaModule): Nothing {
    error(
        "无法为模块 `${module.moduleDescription}` 提供 low-level 解析策略，" +
            "当前 use-site 模块为 `${useSiteModule.moduleDescription}`。",
    )
}
