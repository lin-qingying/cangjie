package org.cangjie.cfir.resolve

import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.session.CfirSession

/**
 * 全阶段解析编排器。
 *
 * 参考 K2 FirTotalResolveProcessor。
 */
class CfirTotalResolveProcessor(
    private val session: CfirSession,
    private val registry: CfirPhaseResolverRegistry,
) {

    fun processFile(file: CfirFile) {
        for (phase in CfirResolvePhase.entries) {
            val processor = registry.getProcessor(phase) ?: continue
            processor.process(file, session)
            processDeclarationsRecursively(file.declarations, processor)
        }
    }

    fun processToPhase(declaration: CfirDeclaration, targetPhase: CfirResolvePhase) {
        for (phase in CfirResolvePhase.entries) {
            if (phase > targetPhase) break
            if (phase <= declaration.resolvePhase) continue
            val processor = registry.getProcessor(phase) ?: continue
            processor.process(declaration, session)
        }
    }

    private fun processDeclarationsRecursively(
        declarations: List<CfirDeclaration>,
        processor: CfirResolveProcessor,
    ) {
        for (declaration in declarations) {
            processor.process(declaration, session)
            val nestedDeclarations = (declaration as? CfirClassLikeDeclaration)?.declarations ?: emptyList()
            if (nestedDeclarations.isNotEmpty()) {
                processDeclarationsRecursively(nestedDeclarations, processor)
            }
        }
    }
}
