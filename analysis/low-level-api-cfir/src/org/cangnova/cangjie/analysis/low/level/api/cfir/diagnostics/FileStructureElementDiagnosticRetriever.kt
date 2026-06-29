

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
    /**
     * 当前 structure element 对应的 CFIR 声明。
     */
    val declaration: CfirDeclaration,
    /**
     * 声明所在的 CFIR 文件。
     */
    private val file: CfirFile,
    /**
     * 当前模块的 low-level 解析组件。
     */
    private val moduleComponents: LLCfirModuleResolveComponents,
) {
    /**
     * 强制必要的 body resolve，恢复 checker context，并收集当前 structure element 的 diagnostics。
     */
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

    /**
     * 基于持久 checker context 和 diagnostics 组件创建具体 visitor。
     */
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
    /**
     * 当前容器 diagnostics 收集时不应递归访问的声明集合。
     */
    private val declarationsToIgnore: Set<CfirDeclaration>,
    context: CheckerContextForProvider,
    components: DiagnosticCollectorComponents,
) : LLCfirDiagnosticVisitor(context, components) {
    /**
     * 跳过属于其它结构元素的嵌套声明。
     */
    override fun shouldVisitDeclaration(declaration: CfirDeclaration): Boolean {
        return declaration !in declarationsToIgnore
    }
}

/**
 * class-like declaration 对应的 structure element diagnostics retriever。
 */
internal class ClassDiagnosticRetriever(
    declaration: CfirClassLikeDeclaration,
    file: CfirFile,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(declaration, file, moduleComponents) {
    /**
     * 创建忽略子结构元素声明的 class diagnostics visitor。
     */
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLCfirDiagnosticVisitor {
        return Visitor(declaration as CfirClassLikeDeclaration, context, components)
    }

    /**
     * class-like 容器 diagnostics visitor。
     */
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
        /**
         * 判断 fake-source 元素是否必须在 class diagnostics 中检查。
         */
        fun shouldDiagnosticsAlwaysBeCheckedOn(cfirElement: CfirElement) = when (cfirElement.source?.kind) {
            CjFakeSourceElementKind.PropertyFromParameter -> true
            CjFakeSourceElementKind.ImplicitConstructor -> true
            else -> false
        }
    }
}

/**
 * 单个非局部声明对应的 structure element diagnostics retriever。
 */
internal class SingleNonLocalDeclarationDiagnosticRetriever(
    declaration: CfirDeclaration,
    file: CfirFile,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(declaration, file, moduleComponents) {
    /**
     * 创建只检查当前非局部声明的 visitor。
     */
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLCfirDiagnosticVisitor {
        return Visitor(context, components)
    }

    /**
     * 单声明 diagnostics visitor，额外处理主构造参数对应属性。
     */
    private class Visitor(
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLCfirDiagnosticVisitor(context, components) {
        /**
         * 主构造器 diagnostics 收集后，补充访问由构造参数生成的属性。
         */
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

/**
 * 整个 CFIR 文件对应的 structure element diagnostics retriever。
 */
internal class FileDiagnosticRetriever(
    file: CfirFile,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(file, file, moduleComponents) {
    /**
     * 创建忽略子结构元素声明的 file diagnostics visitor。
     */
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLCfirDiagnosticVisitor {
        return Visitor(declaration as CfirFile, context, components)
    }

    /**
     * 文件级容器 diagnostics visitor。
     */
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
