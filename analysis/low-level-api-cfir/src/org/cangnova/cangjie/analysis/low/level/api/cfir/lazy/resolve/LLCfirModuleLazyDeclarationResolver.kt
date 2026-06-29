

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLCfirLazyResolverRunner
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.PartialBodyAnalysisSuspendedException
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLFlightRecorder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.getContainingFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnalysisReadiness
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.session.diagnosticReporter
import org.cangnova.cangjie.cfir.resolve.transformers.CfirImportResolveTransformer
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * This is the entry point for lazy resolution.
 *
 * The class is responsible to [collect][LLCfirResolveDesignationCollector] required [LLCfirResolveTarget]
 * and resolve it for the requested phase.
 *
 * @see org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
 * @see LLCfirLazyResolverRunner
 */
internal class LLCfirModuleLazyDeclarationResolver(val moduleComponents: LLCfirModuleResolveComponents) {
    /**
     * Lazily resolves the [target] to a given [toPhase].
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration that is going to be resolved.
     */
    fun lazyResolve(target: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        if (checkAnalysisReadiness(target, containingDeclarations = null, toPhase)) {
            return
        }

        lazyResolve(target, toPhase, LLCfirResolveDesignationCollector::getDesignationToResolve)
    }

    /**
     * Lazily resolves the [target] with all callable members to a given [toPhase].
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration that is going to be resolved.
     */
    fun lazyResolveWithCallableMembers(target: CfirClass, toPhase: CfirResolvePhase) {
        if (target.resolvePhase >= toPhase && target.declarations.all { it !is CfirCallableDeclaration || it.resolvePhase >= toPhase }) {
            LLFlightRecorder.readyPhase(target, toPhase)
            return
        }

        lazyResolve(target, toPhase, LLCfirResolveDesignationCollector::getDesignationToResolveWithCallableMembers)
    }

    /**
     * Lazily resolves the [target] with nested declarations to a given [toPhase] recursively.
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration that is going to be resolved.
     */
    fun lazyResolveRecursively(target: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        lazyResolve(target, toPhase, LLCfirResolveDesignationCollector::getDesignationToResolveRecursively)
    }

    /**
     * lazy resolve 的公共执行骨架。
     *
     * 先保证目标所在文件至少完成 import resolve，再通过 [resolveTarget] 收集真正需要推进的 designation；
     * 所有异常都会附带目标元素、起止阶段和模块会话信息后重新抛出。
     */
    private inline fun <T : CfirElementWithResolveState> lazyResolve(
        targetElement: T,
        toPhase: CfirResolvePhase,
        resolveTarget: (T) -> LLCfirResolveTarget?,
    ) {
        val fromPhase = targetElement.resolvePhase
        try {
            resolveContainingFileToImports(targetElement)
            if (toPhase == CfirResolvePhase.IMPORTS) return

            val target = resolveTarget(targetElement) ?: return
            lazyResolveTargets(target, toPhase)
        } catch (e: Exception) {
            handleExceptionFromResolve(e, targetElement, fromPhase, toPhase)
        }
    }


    /**
     * Lazily resolves all the declarations which are specified for resolve by [target]
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration which is going to be resolved.
     */
    fun lazyResolveTarget(
        target: LLCfirResolveTarget,
        toPhase: CfirResolvePhase,
    ) {
        try {
            target.cfirFile?.let(::resolveFileToImportsWithLock)
            if (toPhase == CfirResolvePhase.IMPORTS) return

            lazyResolveTargets(target, toPhase)
        } catch (_: PartialBodyAnalysisSuspendedException) {
            // Do nothing, partial body resolve is complete
        } catch (e: Exception) {
            handleExceptionFromResolve(e, target, toPhase)
        }
    }

    /**
     * 确保 [target] 所在文件已经推进到 [CfirResolvePhase.IMPORTS]。
     *
     * 对没有包含文件的 synthetic 或特殊 CFIR 元素直接返回。
     */
    private fun resolveContainingFileToImports(target: CfirElementWithResolveState) {
        if (checkAnalysisReadiness(target, containingDeclarations = null, CfirResolvePhase.IMPORTS)) return

        val containingCfirFile = target.getContainingFile() ?: return
        resolveFileToImportsWithLock(containingCfirFile)
    }

    /**
     * 在全局锁和文件写锁下解析 [cfirFile] 的 imports。
     *
     * import resolve 是同文件后续声明 resolve 的前置条件，必须通过 lock provider 串行化。
     */
    private fun resolveFileToImportsWithLock(cfirFile: CfirFile) {
        val lockProvider = moduleComponents.globalResolveComponents.lockProvider
        lockProvider.withGlobalLock {
            lockProvider.withWriteLock(cfirFile, CfirResolvePhase.IMPORTS) {
                cfirFile.transformSingle(
                    CfirImportResolveTransformer(
                        cfirFile.moduleData.session,
                        cfirFile.moduleData.session.diagnosticReporter,
                    ),
                    null,
                )
            }
        }
    }

    /**
     * 将 [target] 中的所有目标声明逐阶段推进到 [toPhase]。
     *
     * 起始阶段取所有目标声明当前阶段的最小值，保证同一个 designation 中阶段较低的声明不会被跳过。
     */
    private fun lazyResolveTargets(target: LLCfirResolveTarget, toPhase: CfirResolvePhase) {
        var currentPhase = getMinResolvePhase(target).coerceAtLeast(CfirResolvePhase.IMPORTS)
        if (checkAnalysisReadiness(target.target, target.path, toPhase, currentPhase)) return

        val helper = LLCfirResolutionActivityTracker.getInstance()
        try {
            helper.beforeLazyResolve()

            while (currentPhase < toPhase) {
                currentPhase = currentPhase.next
                checkCanceled()

                LLCfirLazyResolverRunner.runLazyResolverByPhase(
                    phase = currentPhase,
                    target = target,
                )
            }
        } finally {
            helper.afterLazyResolve()
        }
    }

    /**
     * 返回 [designation] 中所有待解析声明的最低解析阶段。
     */
    private fun getMinResolvePhase(designation: LLCfirResolveTarget): CfirResolvePhase {
        var min = CfirResolvePhase.BODY_RESOLVE
        designation.forEachTarget { target ->
            min = minOf(min, target.resolvePhase)
        }

        return min
    }
}

/**
 * 为单个 CFIR 元素 lazy resolve 失败补充模块、会话、阶段和声明附件后重新抛出。
 */
private fun handleExceptionFromResolve(
    exception: Exception,
    cfirDeclarationToResolve: CfirElementWithResolveState,
    fromPhase: CfirResolvePhase,
    toPhase: CfirResolvePhase,
): Nothing {
    val session = cfirDeclarationToResolve.llCfirSession
    val moduleData = cfirDeclarationToResolve.llCfirModuleData
    val module = moduleData.caModule

    rethrowExceptionWithDetails(
        buildString {
            appendLine("Error while resolving ${cfirDeclarationToResolve::class.java.name} ")
            appendLine("from $fromPhase to $toPhase")
            appendLine("current declaration phase ${cfirDeclarationToResolve.resolvePhase}")
            appendLine("origin: ${(cfirDeclarationToResolve as? CfirDeclaration)?.origin}")
            appendLine("session: ${session::class}")
            appendLine("module data: ${moduleData::class}")
            appendLine("CaModule: ${module::class}")
        },
        exception = exception,
    ) {
        withEntry("CaModule", module) { it.moduleDescription }
        withEntry("session", session) { it.toString() }
        withEntry("moduleData", cfirDeclarationToResolve.moduleData) { it.toString() }
        withCfirEntry("cfirDeclarationToResolve", cfirDeclarationToResolve)
    }
}

/**
 * 为 designation 级 lazy resolve 失败补充模块、会话和 designation 附件后重新抛出。
 */
private fun handleExceptionFromResolve(
    exception: Exception,
    designation: LLCfirResolveTarget,
    toPhase: CfirResolvePhase,
): Nothing {
    val session = designation.target.llCfirSession
    val moduleData = session.llCfirModuleData
    val module = moduleData.caModule

    rethrowExceptionWithDetails(
        buildString {
            appendLine("Error while resolving ${designation::class.java.name} ")
            appendLine("to $toPhase")
            appendLine("module data: ${moduleData::class}")
            appendLine("CaModule: ${module::class}")
        },
        exception = exception,
    ) {
        withEntry("CaModule", module) { it.moduleDescription }
        withEntry("session", session) { it.toString() }
        withEntry("moduleData", moduleData) { it.toString() }
        withEntry("cfirDesignationToResolve", designation) { it.toString() }
    }
}
