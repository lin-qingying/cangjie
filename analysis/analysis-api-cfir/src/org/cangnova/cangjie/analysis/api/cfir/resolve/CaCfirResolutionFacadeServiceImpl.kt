package org.cangjie.analysis.api.cfir.resolve

import org.cangjie.analysis.api.CaModule
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.resolve.transformers.CfirResolveComponentsRegistrar
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangnova.cangjie.name.Name

/**
 * 最小可用的 CFIR 解析外观服务实现。
 *
 * 提供基础 session、模块信息与空诊断收集器，满足最小解析入口需求。
 */
class CaCfirResolutionFacadeServiceImpl : CaCfirResolutionFacadeService {
    override fun getResolutionFacade(module: CaModule): CaCfirResolutionFacade {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = CfirModuleData(Name.identifier(module.name))
        val diagnostics = CfirDiagnosticCollector()

        session.register(CfirModuleData::class, moduleData)
        val phaseResolverRegistry = CfirPhaseResolverRegistry()
        session.register(CfirPhaseResolverRegistry::class, phaseResolverRegistry)
        session.register(CfirDiagnosticReporter::class, diagnostics)
        session.register(CfirDiagnosticCollector::class, diagnostics)
        CfirResolveComponentsRegistrar.register(session, phaseResolverRegistry, diagnostics)

        return CaCfirResolutionFacadeImpl(
            useSiteModule = module,
            useSiteFirSession = session,
            diagnostics = diagnostics,
        )
    }
}
