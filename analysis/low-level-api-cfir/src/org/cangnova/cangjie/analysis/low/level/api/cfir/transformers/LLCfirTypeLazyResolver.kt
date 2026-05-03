/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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

internal object LLCfirTypeLazyResolver : LLCfirLazyResolver(CfirResolvePhase.TYPES) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirTypeTargetResolver(target)

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
 * This resolver is responsible for [TYPES][CfirResolvePhase.TYPES] phase.
 *
 * This resolver:
 * - Transform explicitly written types in declaration headers.
 *
 * Special rules:
 * - Cfirst resolves outer classes to this phase.
 *
 * @see CfirTypeResolveTransformer
 * @see CfirResolvePhase.TYPES
 */
private class LLCfirTypeTargetResolver(target: LLCfirResolveTarget) : LLCfirTargetResolver(target, CfirResolvePhase.TYPES) {
    private val transformer = CfirTypeResolveTransformer(resolveTargetSession, resolveTargetScopeSession)

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(cfirFile: CfirFile, action: () -> Unit) {
        transformer.withFileScope(cfirFile, action)
    }

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

    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        if (cfirExtend.resolvePhase < resolverPhase) {
            performCustomResolveUnderLock(cfirExtend) {
                transformer.resolveExtendTypes(cfirExtend)
            }
        }
        action()
    }

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

    private fun <T : CfirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, Unit>) {
        resolveWithKeeper(target, Unit, keeper) {
            rawResolve(target)
        }
    }

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

    private fun buildConfiguration(topContainer: CfirDeclaration): CfirTypeResolutionConfiguration {
        val containingFile = containingDeclarations.lastOrNull { it is CfirFile } as? CfirFile ?: resolveTarget.cfirFile
        val containingClasses = containingDeclarations.filterIsInstance<CfirClass>()

        var configuration = CfirTypeResolutionConfiguration.EMPTY.withTopContainer(topContainer)
        if (containingFile != null) {
            configuration = configuration
                .withUseSiteFile(containingFile)
                .withScopes(createImportingScopes(containingFile))
        }
        if (containingClasses.isNotEmpty()) {
            configuration = configuration.withContainingClassDeclarations(containingClasses)
            for (containingClass in containingClasses) {
                configuration = configuration.withAdditionalTypeParameters(containingClass.typeParameters)
            }
        }
        return configuration
    }

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
            add(CfirFileDeclaredTopLevelScope(file))
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, resolveTargetSession))
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
            add(CfirExplicitStarImportingScope(imports, symbolProvider))
            add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
            add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
        }
    }
}

private object TypeStateKeepers {
    val FUNCTION: StateKeeper<CfirFunction, Unit> = stateKeeper { builder, function, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entityList(function.valueParameters, CALLABLE_DECLARATION, context)
    }

    val PROPERTY: StateKeeper<CfirProperty, Unit> = stateKeeper { builder, property, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entity(property.getter, FUNCTION, context)
        builder.entity(property.setter, FUNCTION, context)
    }

    private val CALLABLE_DECLARATION: StateKeeper<CfirCallableDeclaration, Unit> = stateKeeper { builder, _, _ ->
        builder.add(CfirCallableDeclaration::returnTypeRef, CfirCallableDeclaration::replaceReturnTypeRef)
    }
}
