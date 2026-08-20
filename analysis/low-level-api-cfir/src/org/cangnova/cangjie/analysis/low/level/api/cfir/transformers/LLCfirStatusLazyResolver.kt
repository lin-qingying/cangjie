

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirSingleResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.session
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.tryCollectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnalysisReadiness
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkDeclarationStatusIsResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusComputationSession
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusResolveTransformer
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.visitors.transformSingle

/**
 * STATUS 阶段的低阶懒解析入口。
 */
internal object LLCfirStatusLazyResolver : LLCfirLazyResolver(CfirResolvePhase.STATUS) {
    /**
     * 为 [target] 创建 STATUS 阶段目标解析器，并建立当前目标适用的状态解析模式。
     */
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver {
        val session = target.session
        val resolveMode = target.resolveMode()
        return LLCfirStatusTargetResolver(
            target = target,
            resolveMode = resolveMode,
            statusComputationSession = LLStatusComputationSession(
                session,
                session.getScopeSession(),
                resolveMode,
            ),
        )
    }

    /**
     * 校验成员声明的状态已经解析完成。
     */
    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        if (target !is CfirMemberDeclaration) return
        checkDeclarationStatusIsResolved(target)
    }
}

/**
 * STATUS 阶段解析目标的策略。
 *
 * [resolveSupertypes] 控制是否强制解析父类型状态，[shouldBeResolved] 控制 class-like 成员 callable 是否随容器一起解析。
 */
private sealed class StatusResolveMode(val resolveSupertypes: Boolean) {
    /**
     * 判断 [callableDeclaration] 是否应随当前 class-like 容器一起解析状态。
     */
    abstract fun shouldBeResolved(callableDeclaration: CfirCallableDeclaration): Boolean

    /**
     * 只解析当前目标，不主动解析全部 callable 成员。
     */
    object OnlyTarget : StatusResolveMode(resolveSupertypes = false) {
        /**
         * OnlyTarget 模式下 callable 成员不会被容器解析连带推进。
         */
        override fun shouldBeResolved(callableDeclaration: CfirCallableDeclaration): Boolean = false
    }

    /**
     * 解析目标及其 callable 成员，并在需要时解析父类型状态。
     */
    object AllCallables : StatusResolveMode(resolveSupertypes = true) {
        /**
         * AllCallables 模式下所有 callable 成员都会被容器解析连带推进。
         */
        override fun shouldBeResolved(callableDeclaration: CfirCallableDeclaration): Boolean = true
    }
}

/**
 * 根据解析目标形态选择 STATUS 阶段模式。
 */
private fun LLCfirResolveTarget.resolveMode(): StatusResolveMode = when (this) {
    is LLCfirSingleResolveTarget -> when (target) {
        is CfirClassLikeDeclaration -> StatusResolveMode.OnlyTarget
        else -> StatusResolveMode.AllCallables
    }

    else -> StatusResolveMode.AllCallables
}

/**
 * low-level STATUS 阶段使用的状态计算会话。
 *
 * 它维护 use-site 会话栈，使父类型符号查找可以从当前 class-like 所属会话逐层回退到使用点会话。
 */
private class LLStatusComputationSession(
    useSiteSession: LLCfirSession,
    useSiteScopeSession: ScopeSession,
    /**
     * 当前 STATUS 阶段解析模式。
     */
    val resolveMode: StatusResolveMode,
) : CfirStatusComputationSession(useSiteSession, useSiteScopeSession) {
    /**
     * 当前父类型解析过程中可用的 use-site 会话栈。
     */
    private val useSiteSessions: MutableList<LLCfirSession> = mutableListOf(useSiteSession)

    /**
     * 在 [classLikeDeclaration] 所属会话上下文中执行 [action]。
     */
    private inline fun withClassSession(classLikeDeclaration: CfirClassLikeDeclaration, action: () -> Unit) {
        val newSession = (classLikeDeclaration.moduleData.session as? LLCfirSession)
            ?.takeUnless { it == useSiteSessions.lastOrNull() }
        try {
            newSession?.let(useSiteSessions::add)
            action()
        } finally {
            newSession?.let { useSiteSessions.removeLast() }
        }
    }

    /**
     * 强制解析 [declaration] 父类型的状态，并在声明所属会话中完成父类型符号查找。
     */
    override fun forceResolveStatusesOfSupertypes(declaration: CfirDeclaration) {
        if (declaration !is CfirClassLikeDeclaration) return
        withClassSession(declaration) {
            super.forceResolveStatusesOfSupertypes(declaration)
        }
    }

    /**
     * 把父类型 [typeRef] 转换为可参与状态计算的 class-like 符号集合。
     */
    override fun superTypeToSymbols(typeRef: CfirTypeRef) = buildSet {
        val classId = typeRef.coneType.classId ?: return@buildSet

        for (useSiteSession in useSiteSessions.asReversed()) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.let(::add)
        }
    }

    /**
     * 递归解析父类型 class-like 声明的 STATUS。
     */
    override fun resolveClassForSuperType(classLikeDeclaration: CfirClassLikeDeclaration): Boolean {
        val target = classLikeDeclaration.tryCollectDesignation()?.asResolveTarget() ?: return false
        val resolver = LLCfirStatusTargetResolver(
            target,
            resolveMode = resolveMode,
            statusComputationSession = this,
        )

        resolver.resolveDesignation()
        return true
    }
}

/**
 * STATUS 阶段当前保持 low-level 独立 `LLStatusComputationSession` 分层，
 * 同时仍然以仓颉主干 `CfirStatusResolveTransformer` 为真实变换器。
 */
private class LLCfirStatusTargetResolver(
    target: LLCfirResolveTarget,
    /**
     * 当前目标使用的 STATUS 解析模式。
     */
    private val resolveMode: StatusResolveMode,
    statusComputationSession: CfirStatusComputationSession,
) : LLCfirTargetResolver(target, CfirResolvePhase.STATUS) {
    /**
     * 当前 resolver 绑定的状态计算会话。
     */
    private val statusComputationSession: CfirStatusComputationSession = statusComputationSession
    /**
     * 委托主干状态解析逻辑的 transformer 包装。
     */
    private val transformer = Transformer(statusComputationSession)

    /**
     * 进入 class-like 容器时确保其 STATUS 已解析，并在 transformer 中登记当前 class。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        if (cfirClassLike is CfirClass || cfirClassLike is CfirInterface || cfirClassLike is CfirStruct || cfirClassLike is CfirEnum) {
            doResolveWithoutLock(cfirClassLike)
            transformer.storeClass(cfirClassLike) {
                action()
            }

            transformer.statusComputationSession.endComputing(cfirClassLike)
        } else {
            action()
        }
    }

    /**
     * 解析 class-like 类型参数的状态。
     */
    private fun resolveClassLikeTypeParameters(classLike: CfirClassLikeDeclaration) {
        classLike.transformTypeParameters(transformer, data = null)
    }

    /**
     * 按 [resolveMode] 解析 class-like 中需要随容器推进的 callable 成员状态。
     */
    private fun resolveCallableMembers(classLike: CfirClassLikeDeclaration) {
        for (member in classLike.declarations) {
            if (member !is CfirCallableDeclaration || !resolveMode.shouldBeResolved(member)) continue

            member.lazyResolveToPhase(resolverPhase.previous)
            performResolve(member)
        }
    }

    /**
     * 在无目标锁阶段执行 STATUS 解析，并在需要时自行进入自定义写锁。
     */
    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean = when (target) {
        is CfirClass -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClassLike(target)
            }

            true
        }

        is CfirInterface -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClassLike(target)
            }

            true
        }

        is CfirStruct -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClassLike(target)
            }

            true
        }

        is CfirEnum -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClassLike(target)
            }

            true
        }

        is CfirNamedFunction -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenFunctions(it, transformer.containingClass) },
                { element, overridden -> transformer.transformNamedFunction(element, overridden) },
            )

            true
        }

        is CfirFunction -> {
            if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) {
                true
            } else {
                performCustomResolveUnderLock(target) {
                    transformer.transformFunctionStatusWithoutPhaseGuard(target)
                }

                true
            }
        }

        is CfirExtend -> {
            if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) {
                true
            } else {
                performCustomResolveUnderLock(target) {
                    transformer.transformExtendStatusWithoutPhaseGuard(target)
                }

                true
            }
        }

        is CfirProperty -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenProperties(it, transformer.containingClass) },
                { element, overridden -> transformer.transformProperty(element, overridden) },
            )

            true
        }

        is CfirTypeAlias -> {
            if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase) && target.status is CfirResolvedDeclarationStatus) {
                true
            } else {
                performCustomResolveUnderLock(target) {
                    transformer.transformTypeAliasStatusWithoutPhaseGuard(target)
                }

                true
            }
        }

        is CfirPatternVariable -> {
            performCustomResolveUnderLock(target) {
                transformer.transformVariableStatusWithoutPhaseGuard(target)
            }

            true
        }

        is CfirPatternBindingVariable -> {
            performCustomResolveUnderLock(target) {
                transformer.transformVariableStatusWithoutPhaseGuard(target)
            }

            true
        }

        else -> false
    }

    /** 在解析 callable 状态前收集 override 目标，并把 override 集合交给 [transform]。 */
    private inline fun <T : CfirCallableDeclaration> performResolveWithOverriddenCallables(
        target: T,
        getOverridden: (T) -> List<T>,
        crossinline transform: (T, List<T>) -> Unit,
    ) {
        if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) return
        val overriddenDeclarations = getOverridden(target)
        performCustomResolveUnderLock(target) {
            transform(target, overriddenDeclarations)
        }
    }

    /**
     * 解析 class-like 声明本身的状态、类型参数和必要的 callable 成员状态。
     */
    private fun resolveClassLike(classLike: CfirClassLikeDeclaration) {
        transformer.statusComputationSession.startComputing(classLike)

        if (resolveMode.resolveSupertypes) {
            transformer.statusComputationSession.forceResolveStatusesOfSupertypes(classLike)
        }

        performCustomResolveUnderLock(classLike) {
            when (classLike) {
                is CfirClass -> transformer.transformClassStatus(classLike)
                is CfirInterface -> transformer.transformInterfaceStatus(classLike)
                is CfirStruct -> transformer.transformStructStatus(classLike)
                is CfirEnum -> transformer.transformEnumStatus(classLike)
                else -> error("Unexpected class-like declaration ${classLike::class.simpleName} for low-level STATUS resolver")
            }
            transformer.storeClass(classLike) {
                resolveClassLikeTypeParameters(classLike)
            }
        }

        if (resolveMode.resolveSupertypes) {
            transformer.storeClass(classLike) {
                withContainingDeclaration(classLike) {
                    resolveCallableMembers(classLike)
                }
            }

            transformer.statusComputationSession.endComputing(classLike)
        } else {
            transformer.statusComputationSession.computeOnlyDeclarationStatus(classLike)
        }
    }

    /**
     * 在目标锁内执行常规 STATUS transformer。
     */
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirClass -> error("should be resolved in doResolveWithoutLock")
            is CfirInterface -> error("should be resolved in doResolveWithoutLock")
            is CfirFile -> Unit
            else -> target.transformSingle(transformer, data = null)
        }
    }

    /**
     * STATUS 阶段使用的 transformer 包装。
     *
     * class 和 interface 的常规 transform 入口在 low-level 中由 [resolveClassLike] 控制，因此这里直接返回原声明。
     */
    private class Transformer(statusComputationSession: CfirStatusComputationSession) :
        CfirStatusResolveTransformer(statusComputationSession) {
        /**
         * class 状态由外层 resolver 显式处理。
         */
        override fun transformClass(klass: CfirClass, data: Nothing?): CfirClass {
            return klass
        }

        /**
         * interface 状态由外层 resolver 显式处理。
         */
        override fun transformInterface(`interface`: CfirInterface, data: Nothing?): CfirInterface {
            return `interface`
        }
    }
}
