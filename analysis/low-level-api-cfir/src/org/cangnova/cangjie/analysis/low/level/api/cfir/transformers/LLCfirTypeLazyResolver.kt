

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnnotationTypeIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkReturnTypeRefIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkTypeRefIsResolved
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTypeResolveTransformer
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * TYPES 阶段的低阶懒解析入口。
 */
internal object LLCfirTypeLazyResolver : LLCfirLazyResolver(CfirResolvePhase.TYPES) {
    /**
     * 为 [target] 创建 TYPES 阶段目标解析器。
     */
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirTypeTargetResolver(target)

    /**
     * 校验注解类型、callable 返回类型和类型参数上界已经完成类型解析。
     */
    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        if (target is CfirAnnotationContainer) {
            checkAnnotationTypeIsResolved(target)
        }

        when (target) {
            is CfirPatternVariable -> {
                checkReturnTypeRefIsResolved(target, acceptImplicitTypeRef = true)
                for (binding in target.pattern.bindingVariables()) {
                    checkReturnTypeRefIsResolved(binding, acceptImplicitTypeRef = true)
                    checkAnnotationTypeIsResolved(binding)
                }
            }
            is CfirEnumConstructor -> {
                checkReturnTypeRefIsResolved(target, acceptImplicitTypeRef = false)
                for (parameter in target.valueParameters) {
                    checkTypeRefIsResolved(parameter.returnTypeRef, "enum payload type", target)
                }
            }
            is CfirCallableDeclaration -> checkReturnTypeRefIsResolved(target, acceptImplicitTypeRef = true)
            is CfirTypeParameter -> {
                for (bound in target.bounds) {
                    checkTypeRefIsResolved(bound, "type parameter bound", target)
                }
            }
        }
    }
}

/**
 * TYPES 阶段的目标解析器。
 *
 * 该解析器负责解析声明头部中显式书写的类型，并在进入外围 class-like 时先把外层声明推进到 TYPES，保证成员签名中的外层类型参数
 * 和导入作用域可见性与普通类型解析一致。
 *
 * @see CfirTypeResolveTransformer
 * @see CfirResolvePhase.TYPES
 */
private class LLCfirTypeTargetResolver(target: LLCfirResolveTarget) : LLCfirTargetResolver(target, CfirResolvePhase.TYPES) {
    /**
     * 当前 TYPES 阶段使用的主干类型解析 transformer。
     */
    private val transformer = CfirTypeResolveTransformer(resolveTargetSession, resolveTargetScopeSession)

    /**
     * 在文件导入作用域中执行 [action]。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(cfirFile: CfirFile, action: () -> Unit) {
        transformer.withFileScope(cfirFile, action)
    }

    /**
     * 进入 class-like 前先解析其类型头部，并在类作用域中执行 [action]。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        cfirClassLike.lazyResolveToPhase(resolverPhase.previous)
        transformer.withClassDeclarationCleanup(cfirClassLike) {
            performCustomResolveUnderLock(cfirClassLike) {
                transformer.resolveClassTypes(cfirClassLike)
            }
            transformer.withClassScopes(cfirClassLike, action)
        }
    }

    /**
     * 进入 extend 容器前确保 extend 类型已经解析。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        if (cfirExtend.resolvePhase < resolverPhase) {
            performCustomResolveUnderLock(cfirExtend) {
                transformer.resolveExtendTypes(cfirExtend)
            }
        }
        action()
    }

    /**
     * 在目标锁内执行 TYPES 阶段解析。
     */
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirFunction -> resolve(target, TypeStateKeepers.FUNCTION)
            is CfirEnumConstructor -> resolve(target, TypeStateKeepers.ENUM_CONSTRUCTOR)
            is CfirProperty -> resolve(target, TypeStateKeepers.PROPERTY)
            is CfirPatternVariable -> resolve(target, TypeStateKeepers.PATTERN_VARIABLE)
            is CfirCallableDeclaration,
            is CfirExtend,
            is CfirFile,
            is CfirTypeAlias,
            is CfirClass,
            is CfirInterface,
            is CfirStruct,
            is CfirEnum,
            is CfirTypeParameter,
            is CfirValueParameter,
                -> rawResolve(target)

            is CfirCodeFragment -> {}
            else -> errorWithAttachment("Unknown declaration ${target::class.simpleName}") {
                withCfirEntry("declaration", target)
            }
        }
    }

    /**
     * 使用 [keeper] 保存 [target] 的类型引用状态后执行 raw 解析。
     */
    private fun <T : CfirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, Unit>) {
        resolveWithKeeper(target, Unit, keeper) {
            rawResolve(target)
        }
    }

    /**
     * 对 [target] 执行实际 TYPES 阶段转换。
     */
    private fun rawResolve(target: CfirElementWithResolveState) {
        when (target) {
            is CfirFile -> transformer.resolveFileTypes(target)
            is CfirClass -> transformer.withClassDeclarationCleanup(target) { transformer.resolveClassTypes(target) }
            is CfirInterface -> transformer.resolveClassTypes(target)
            is CfirStruct -> transformer.resolveClassTypes(target)
            is CfirEnum -> transformer.resolveClassTypes(target)
            is CfirExtend -> transformer.resolveExtendTypes(target)
            is CfirTypeAlias,
            is CfirCallableDeclaration,
            is CfirTypeParameter,
            is CfirValueParameter,
                -> transformer.resolveDesignatedDeclarationTypes(
                    target,
                    containingDeclarations.lastOrNull { it is CfirFile } as? CfirFile ?: resolveTarget.cfirFile,
                    containingDeclarations,
                )
            else -> errorWithAttachment("Unknown declaration ${target::class.simpleName}") {
                withCfirEntry("declaration", target)
            }
        }
    }

}

/**
 * TYPES 阶段需要在局部解析失败时恢复的状态集合。
 */
private object TypeStateKeepers {
    /** 模式头解析是同一事务，恢复原 binding 类型与阶段以及 pattern 显式类型引用。 */
    val PATTERN_VARIABLE: StateKeeper<CfirPatternVariable, Unit> = stateKeeper { builder, declaration, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entityList(declaration.pattern.bindingVariables(), PATTERN_BINDING, context)
        builder.entity(declaration.pattern, PatternStateKeepers.PATTERN, context)
    }

    private val PATTERN_BINDING: StateKeeper<CfirPatternBindingVariable, Unit> = stateKeeper { builder, _, context ->
        builder.add(CALLABLE_DECLARATION, context)
        @OptIn(ResolveStateAccess::class)
        builder.add({ it.resolveState }, { declaration, state -> declaration.resolveState = state })
    }

    /** 构造项的返回类型和 payload 类型都属于同一次声明头解析，取消时一起恢复。 */
    val ENUM_CONSTRUCTOR: StateKeeper<CfirEnumConstructor, Unit> = stateKeeper { builder, declaration, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entityList(declaration.valueParameters, CALLABLE_DECLARATION, context)
    }

    /**
     * 函数解析状态保持器，会同时保护函数值参数上的 callable 类型状态。
     */
    val FUNCTION: StateKeeper<CfirFunction, Unit> = stateKeeper { builder, function, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entityList(function.valueParameters, CALLABLE_DECLARATION, context)
    }

    /**
     * 属性解析状态保持器，会保护属性本身以及 getter/setter 的返回类型状态。
     */
    val PROPERTY: StateKeeper<CfirProperty, Unit> = stateKeeper { builder, property, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entity(property.getter, FUNCTION, context)
        builder.entity(property.setter, FUNCTION, context)
    }

    /**
     * callable 声明返回类型引用的基础状态保持器。
     */
    private val CALLABLE_DECLARATION: StateKeeper<CfirCallableDeclaration, Unit> = stateKeeper { builder, _, _ ->
        builder.add(CfirCallableDeclaration::returnTypeRef, CfirCallableDeclaration::replaceReturnTypeRef)
    }
}
