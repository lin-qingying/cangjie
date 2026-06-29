

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnnotationTypeIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkReturnTypeRefIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkTypeRefIsResolved
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTypeResolveTransformer
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.symbolProvider
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
            is CfirProperty -> resolve(target, TypeStateKeepers.PROPERTY)
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
                -> target.accept(transformer, buildConfiguration(target))
            else -> errorWithAttachment("Unknown declaration ${target::class.simpleName}") {
                withCfirEntry("declaration", target)
            }
        }
    }

    /**
     * 为 [topContainer] 创建类型解析配置。
     */
    private fun buildConfiguration(topContainer: CfirDeclaration): CfirTypeResolutionConfiguration {
        val containingFile = containingDeclarations.lastOrNull { it is CfirFile } as? CfirFile ?: resolveTarget.cfirFile
        val containingClasses = containingDeclarations.filterIsInstance<CfirClass>()
        val containingClassLikes = containingDeclarations.filterIsInstance<CfirClassLikeDeclaration>()

        var configuration = CfirTypeResolutionConfiguration.EMPTY.withTopContainer(topContainer)
        if (containingFile != null) {
            configuration = configuration
                .withUseSiteFile(containingFile)
                .withScopes(createImportingScopes(containingFile))
        }
        if (containingClasses.isNotEmpty()) {
            configuration = configuration.withContainingClassDeclarations(containingClasses)
        }
        if (containingClassLikes.isNotEmpty()) {
            for (containingClassLike in containingClassLikes) {
                configuration = configuration.withAdditionalTypeParameters(containingClassLike.typeParametersForResolution())
            }
        }
        return configuration
    }

    /**
     * low-level TYPES 需要和主 TYPES 解析器保持相同的外层类型参数可见性：
     * interface / struct / enum 的成员签名同样可以引用所属 class-like 的类型参数。
     */
    private fun CfirClassLikeDeclaration.typeParametersForResolution(): List<CfirTypeParameter> = when (this) {
        is CfirClass -> typeParameters
        is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> emptyList()
        is CfirInterface -> typeParameters
        is CfirStruct -> typeParameters
        is CfirEnum -> typeParameters
        is CfirTypeAlias -> typeParameters
    }

    /**
     * 为 [file] 创建与普通类型解析顺序一致的导入作用域列表。
     */
    private fun createImportingScopes(file: CfirFile): List<CfirScope> {
        val symbolProvider = resolveTargetSession.symbolProvider
        val imports = file.imports
        val defaultImports = resolveTargetSession.defaultImportsProvider
            .getDefaultImports(includeLowPriorityImports = true)
            .filter { it.fqName !in resolveTargetSession.defaultImportsProvider.excludedImports }
            .map { importPath ->
                buildImport {
                    source = null
                    importedFqName = importPath.fqName
                    isAllUnder = importPath.isAllUnder
                    aliasName = importPath.alias
                    aliasSource = null
                }
            }

        return buildList {
            // CfirTypeResolver 按顺序查找 scope；lazy type resolve 与普通类型解析保持一致。
            add(CfirFileDeclaredTopLevelScope(file))
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, resolveTargetSession))
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
            add(CfirExplicitStarImportingScope(imports, symbolProvider))
            add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
            add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
        }
    }
}

/**
 * TYPES 阶段需要在局部解析失败时恢复的状态集合。
 */
private object TypeStateKeepers {
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
