

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.PersistenceContextCollector
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.PersistentCheckerContextFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.declarationsToIgnore
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.forEachDeclaration
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirPrimaryConstructor
import org.cangnova.cangjie.cfir.resolve.SessionHolderImpl
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.util.withSourceCodeAnalysisExceptionUnwrapping

/**
 * Collects [FileStructureElementDiagnosticList] for specific [declaration].
 *
 * @see FileStructureElementDiagnostics
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructureElement
 */
internal sealed class FileStructureElementDiagnosticRetriever(
    val declaration: CfirDeclaration,
    private val file: CfirFile,
    private val moduleComponents: LLCfirModuleResolveComponents,
) {
    fun retrieve(filter: DiagnosticCheckerFilter): FileStructureElementDiagnosticList {
        forceBodyResolve()

        val sessionHolder = SessionHolderImpl(moduleComponents.session, moduleComponents.scopeSessionProvider.getScopeSession())
        val context = if (declaration is CfirFile) {
            PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder)
        } else {
            PersistenceContextCollector.collectContext(sessionHolder, file, declaration)
        }

        return withSourceCodeAnalysisExceptionUnwrapping {
            collectForStructureElement(declaration, filter) { components ->
                createVisitor(context, components)
            }
        }
    }

    abstract fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLCfirDiagnosticVisitor

    /**
     * Declarations-containers may analyze its members, so we have to resole them explicitly as
     * not all of them are pre-resolved during [declaration] resolution.
     * For instance, functions and classes are not a part of the container body resolution.
     */
    private fun forceBodyResolve() {
        ProgressManager.checkCanceled()

        declaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        val declarationContainer = when (declaration) {
            is CfirFile, is CfirClassLikeDeclaration, is CfirExtend -> declaration
            else -> return
        }

        declarationContainer.forEachDeclaration {
            it.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        }
    }
}

/**
 * The visitor is supposed to check the container itself and all declarations that belong to its structure element.
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CfirElementContainerRecorder
 */
private abstract class LLCfirContainerDiagnosticVisitor(
    private val declarationsToIgnore: Set<CfirDeclaration>,
    context: CheckerContextForProvider,
    components: DiagnosticCollectorComponents,
) : LLCfirDiagnosticVisitor(context, components) {
    override fun shouldVisitDeclaration(declaration: CfirDeclaration): Boolean {
        return declaration !in declarationsToIgnore
    }
}

internal class ClassDiagnosticRetriever(
    declaration: CfirClassLikeDeclaration,
    file: CfirFile,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(declaration, file, moduleComponents) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLCfirDiagnosticVisitor {
        return Visitor(declaration as CfirClassLikeDeclaration, context, components)
    }

    private class Visitor(
        regularClass: CfirClassLikeDeclaration,
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLCfirContainerDiagnosticVisitor(
        declarationsToIgnore = regularClass.declarationsToIgnore,
        context = context,
        components = components,
    )

    companion object {
        fun shouldDiagnosticsAlwaysBeCheckedOn(cfirElement: CfirElement) = when (cfirElement.source?.kind) {
            CjFakeSourceElementKind.PropertyFromParameter -> true
            CjFakeSourceElementKind.ImplicitConstructor -> true
            else -> false
        }
    }
}

internal class SingleNonLocalDeclarationDiagnosticRetriever(
    declaration: CfirDeclaration,
    file: CfirFile,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(declaration, file, moduleComponents) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLCfirDiagnosticVisitor {
        return Visitor(context, components)
    }

    private class Visitor(
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLCfirDiagnosticVisitor(context, components) {
        override fun visitConstructor(constructor: CfirConstructor, data: Nothing?) {
            super.visitConstructor(constructor, data)

            if (constructor is CfirPrimaryConstructor) {
                for (valueParameter in constructor.valueParameters) {
                    valueParameter.correspondingProperty?.let {
                        visitProperty(it, data)
                    }
                }
            }
        }
    }
}

internal class FileDiagnosticRetriever(
    file: CfirFile,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(file, file, moduleComponents) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLCfirDiagnosticVisitor {
        return Visitor(declaration as CfirFile, context, components)
    }

    private class Visitor(
        file: CfirFile,
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLCfirContainerDiagnosticVisitor(
        declarationsToIgnore = file.declarationsToIgnore,
        context = context,
        components = components,
    )
}
