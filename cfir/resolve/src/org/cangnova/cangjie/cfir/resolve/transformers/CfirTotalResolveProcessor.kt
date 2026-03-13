package org.cangjie.cfir.resolve.transformers

import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirPackageDirective
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName

/**
 * K2 FirTotalResolveProcessor-like orchestrator for CFIR pipelines.
 */
class CfirTotalResolveProcessor(
    @Suppress("unused") private val session: CfirSession,
    private val registry: CfirPhaseResolverRegistry,
) {
    fun processFile(file: CfirFile) {
        process(listOf(file))
    }

    fun processToPhase(declaration: CfirDeclaration, targetPhase: CfirResolvePhase) {
        val syntheticFile = CfirFile(
            moduleData = declaration.moduleData,
            name = "__cfir-synthetic.resolve",
            packageDirective = CfirPackageDirective(FqName.ROOT),
            declarations = mutableListOf(declaration),
        )

        for (phase in CfirResolvePhase.entries) {
            if (phase > targetPhase) break
            if (phase <= declaration.resolvePhase) continue
            val processor = registry.getProcessor(phase) ?: continue
            processor.beforePhase()
            try {
                processWithProcessor(processor, listOf(syntheticFile))
            } finally {
                processor.afterPhase()
            }
        }
    }

    fun process(files: Collection<CfirFile>) {
        for (phase in CfirResolvePhase.entries) {
            val processor = registry.getProcessor(phase) ?: continue
            processor.beforePhase()
            try {
                processWithProcessor(processor, files)
            } finally {
                processor.afterPhase()
            }
        }
    }

    private fun processWithProcessor(
        processor: CfirResolveProcessor,
        files: Collection<CfirFile>,
    ) {
        when (processor) {
            is CfirGlobalResolveProcessor -> processor.process(files)
            is CfirTransformerBasedResolveProcessor -> files.forEach(processor::processFile)
        }
    }
}
