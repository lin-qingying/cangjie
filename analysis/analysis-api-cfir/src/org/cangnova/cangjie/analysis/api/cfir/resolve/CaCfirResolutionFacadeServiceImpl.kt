package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.pipeline.buildCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.runResolution
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.cfir.session.diagnosticCollector
import java.util.concurrent.ConcurrentHashMap

/**
 * CFIR 解析外观服务实现。
 *
 * 对齐 Kotlin `LLResolutionFacadeService` 的核心职责：
 * 1. 以 `CaModule` 为键缓存 facade；
 * 2. 根据模块依赖图构建底层 session 图；
 * 3. 预先把 use-site 模块推进到可读取诊断与解析结果的状态。
 *
 * 当前仓颉实现先覆盖源码模块路径，后续平台模块、脚本模块与库模块补齐时，应继续沿用同一构造协议。
 */
class CaCfirResolutionFacadeServiceImpl(
    private val project: Project,
) : CaCfirResolutionFacadeService {
    private val cache = ConcurrentHashMap<CaModule, CaCfirResolutionFacadeImpl>()

    override fun getResolutionFacade(module: CaModule): CaCfirResolutionFacade {
        return cache.computeIfAbsent(module, ::createResolutionFacade)
    }

    private fun createResolutionFacade(module: CaModule): CaCfirResolutionFacadeImpl {
        val sourceModule = module as? CaSourceModule ?: error(
            "CaCfirResolutionFacadeService 目前只支持源码模块，实际得到 `${module::class.java.name}`。",
        )

        val dependencyFacades = sourceModule.directRegularDependencies.map(::requireSourceDependencyFacade)
        val friendDependencyFacades = sourceModule.directFriendDependencies.map(::requireSourceDependencyFacade)
        val dependsOnFacades = sourceModule.directDependsOnDependencies.map(::requireSourceDependencyFacade)

        val moduleName = Name.special("<${sourceModule.stableModuleName ?: sourceModule.name}>")
        val dependencyList = DependencyListForCliModule.build(moduleName) {}
        val configuration = org.cangnova.cangjie.config.CompilerConfiguration.createForCfirFrontend().apply {
            this.languageVersionSettings = sourceModule.languageVersionSettings
            this.moduleName = sourceModule.name
        }

        val moduleData = CfirSourceModuleData(
            name = moduleName,
            dependencies = dependencyList.regularDependencies +
                dependencyFacades.map(CaCfirResolutionFacadeImpl::moduleData) +
                friendDependencyFacades.map(CaCfirResolutionFacadeImpl::moduleData),
            refinementDependencies = dependencyList.dependsOnDependencies +
                dependsOnFacades.map(CaCfirResolutionFacadeImpl::moduleData),
            platform = CfirPlatform.DEFAULT,
            isCommon = false,
        )

        val sessionFactory = CfirDefaultSessionFactory()
        val sharedLibrarySession = sessionFactory.createSharedLibrarySession(
            mainModuleName = moduleName,
            extensionRegistrars = emptyList(),
            languageVersionSettings = sourceModule.languageVersionSettings,
        )
        sessionFactory.createLibrarySession(
            sharedLibrarySession = sharedLibrarySession,
            moduleDataProvider = dependencyList.moduleDataProvider,
            extensionRegistrars = emptyList(),
            languageVersionSettings = sourceModule.languageVersionSettings,
        )

        val sourceSession = sessionFactory.createSourceSession(
            moduleData = moduleData,
            extensionRegistrars = emptyList(),
            configuration = configuration,
        )

        val sourceFiles = sourceModule.psiRoots.filterIsInstance<CjFile>()
        require(sourceFiles.isNotEmpty()) {
            "Source module `${sourceModule.name}` does not contain any CjFile roots."
        }

        val cfirFiles = sourceSession.buildCfirFromCjFiles(sourceFiles)
        sourceSession.runResolution(cfirFiles)

        return CaCfirResolutionFacadeImpl(
            useSiteModule = module,
            useSiteFirSession = sourceSession,
            moduleData = moduleData,
            diagnosticCollector = sourceSession.diagnosticCollector,
        )
    }

    private fun requireSourceDependencyFacade(module: CaModule): CaCfirResolutionFacadeImpl {
        return getResolutionFacade(module) as? CaCfirResolutionFacadeImpl ?: error(
            "Expected cached CFIR facade for dependency `${module.moduleDescription}` to be `CaCfirResolutionFacadeImpl`.",
        )
    }
}
