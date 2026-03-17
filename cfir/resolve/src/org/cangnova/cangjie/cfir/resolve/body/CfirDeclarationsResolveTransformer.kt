package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternVariableImpl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.*
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.explicitTypeRefResolver
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Declaration-level resolve transformer.
 *
 * Responsibilities:
 * - manage scope stack for files/classes/functions/blocks
 * - resolve declaration initializers and infer implicit types where needed
 * - register local declarations into local scopes
 */
class CfirDeclarationsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {

    override fun transformFile(file: CfirFile, data: CfirResolutionMode): CfirFile {
        val savedContext = context.towerDataContext
        context.withFile(file) {
            val importScopes = createImportingScopes(file)
            context.addNonLocalScopes(importScopes)

            (file as? org.cangnova.cangjie.cfir.declarations.impl.CfirFileImpl)?.declarations = file.declarations.map { decl ->
                decl.transform<CfirDeclaration, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
            }
        }
        context.withTowerDataContext(savedContext) {}
        return file
    }

    override fun transformClass(klass: CfirClass, data: CfirResolutionMode): CfirClass {
        val savedContext = context.towerDataContext

        context.withContainer(klass) {
            if (klass.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(klass.typeParameters))
            }

            val classSymbol = klass.symbol as? CfirClassSymbol
            if (classSymbol != null) {
                context.addNonLocalScope(CfirClassUseSiteMemberScope(classSymbol, session.symbolProvider))

                val extendProvider = components.extendProvider
                if (extendProvider != null) {
                    val classId = resolveClassId(klass)
                    if (classId != null) {
                        context.addNonLocalScope(CfirExtendMemberScope(classId, extendProvider))
                    }
                }
            }

            (klass as? org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl)?.declarations = klass.declarations.map { decl ->
                decl.transform<CfirDeclaration, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
            }
        }

        context.withTowerDataContext(savedContext) {}
        bumpPhase(klass)
        return klass
    }

    override fun transformFunction(function: CfirFunction, data: CfirResolutionMode): CfirFunction {
        val savedContext = context.towerDataContext

        context.withContainer(function) {
            if (function.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(function.typeParameters))
            }

            val paramScope = CfirLocalScopeImpl()
            for (param in function.valueParameters) {
                val paramSymbol = param.symbol as? CfirCallableSymbol<*> ?: continue
                paramScope.addVariable(param.name, paramSymbol)
            }
            context.addLocalScope(paramScope)

            val body = function.body
            if (body != null) {
                (function as? org.cangnova.cangjie.cfir.declarations.impl.CfirFunctionImpl)?.body = body.transform<CfirBlock, CfirResolutionMode>(
                    transformer, CfirResolutionMode.ContextIndependent
                )
            }

            if (function.returnTypeRef is CfirImplicitTypeRef) {
                val bodyType = function.body?.coneTypeOrNull ?: session.builtinTypes.unitType
                val resolvedType = IdealTypeResolver.resolveIfIdeal(bodyType)
                function.replaceReturnTypeRef(
                    buildResolvedTypeRef {
                        source = function.returnTypeRef.source
                        coneType = resolvedType
                        delegatedTypeRef = function.returnTypeRef
                    },
                )
            }
        }

        context.withTowerDataContext(savedContext) {}
        bumpPhase(function)
        return function
    }

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirProperty {
        val savedContext = context.towerDataContext

        context.withContainer(property) {
            val explicitTypeRef = property.returnTypeRef
            val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
                CfirResolutionMode.WithExpectedType(explicitTypeRef.coneType)
            } else {
                CfirResolutionMode.ContextIndependent
            }

            val initializer = property.initializer
            if (initializer != null) {
                (property as? org.cangnova.cangjie.cfir.declarations.impl.CfirPropertyImpl)?.initializer = initializer.transform<CfirExpression, CfirResolutionMode>(
                    transformer, initializerMode
                )
            }

            if (property.returnTypeRef is CfirImplicitTypeRef) {
                val initType = property.initializer?.coneTypeOrNull
                if (initType != null) {
                    val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                    property.replaceReturnTypeRef(
                        buildResolvedTypeRef {
                            source = property.returnTypeRef.source
                            coneType = resolvedType
                            delegatedTypeRef = property.returnTypeRef
                        },
                    )
                }
            }
        }

        context.withTowerDataContext(savedContext) {}
        bumpPhase(property)
        return property
    }

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirVariable {
        val explicitTypeRef = variable.returnTypeRef
        val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
            CfirResolutionMode.WithExpectedType(explicitTypeRef.coneType)
        } else {
            CfirResolutionMode.ContextIndependent
        }

        val initializer = variable.initializer
        if (initializer != null) {
            (variable as? org.cangnova.cangjie.cfir.declarations.impl.CfirVariableImpl)?.initializer = initializer.transform<CfirExpression, CfirResolutionMode>(
                transformer, initializerMode
            )
        }

        if (variable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = variable.initializer?.coneTypeOrNull
            if (initType != null) {
                val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                variable.replaceReturnTypeRef(
                    buildResolvedTypeRef {
                        source = variable.returnTypeRef.source
                        coneType = resolvedType
                        delegatedTypeRef = variable.returnTypeRef
                    },
                )
            }
        }

        val varSymbol = variable.symbol as? CfirVariableSymbol
        if (varSymbol != null) {
            context.storeVariable(variable.name, varSymbol)
        }

        bumpPhase(variable)
        return variable
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: CfirResolutionMode): CfirDeclaration {
        bumpPhase(declaration)
        return declaration
    }

    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: CfirResolutionMode,
    ): CfirPatternVariable {
        val rawTypeRef = patternVariable.returnTypeRef
        if (rawTypeRef !is CfirResolvedTypeRef && rawTypeRef !is CfirImplicitTypeRef) {
            val resolved = session.explicitTypeRefResolver.resolveExplicitTypeRef(rawTypeRef, emptyMap())
            patternVariable.replaceReturnTypeRef(resolved)
        }

        val explicitTypeRef = patternVariable.returnTypeRef
        val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
            CfirResolutionMode.WithExpectedType(explicitTypeRef.coneType)
        } else {
            CfirResolutionMode.ContextIndependent
        }

        val initializer = patternVariable.initializer
        if (initializer != null) {
            (patternVariable as? CfirPatternVariableImpl)?.initializer =
                initializer.transform<CfirExpression, CfirResolutionMode>(transformer, initializerMode)
        }

        if (patternVariable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = patternVariable.initializer?.coneTypeOrNull
            if (initType != null) {
                val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                patternVariable.replaceReturnTypeRef(
                    buildResolvedTypeRef {
                        source = patternVariable.returnTypeRef.source
                        coneType = resolvedType
                        delegatedTypeRef = patternVariable.returnTypeRef
                    },
                )
            }
        }

        val pvSymbol = patternVariable.symbol as? CfirPatternVariableSymbol
        if (pvSymbol != null) {
            val bindingNames = collectBindingNames(patternVariable.pattern)
            for (name in bindingNames) {
                context.storeVariable(name, pvSymbol)
            }
        }

        bumpPhase(patternVariable)
        return patternVariable
    }

    private fun collectBindingNames(pattern: CfirPattern): List<Name> {
        return when (pattern) {
            is CfirBindingPattern -> {
                val names = mutableListOf(pattern.name)
                pattern.nestedPattern?.let { names.addAll(collectBindingNames(it)) }
                names
            }
            is CfirTuplePattern -> pattern.elements.flatMap { collectBindingNames(it) }
            is CfirEnumPattern -> pattern.arguments.flatMap { collectBindingNames(it) }
            is CfirTypePattern -> listOfNotNull(pattern.bindingName)
            is CfirWildcardPattern, is CfirConstPattern -> emptyList()
            else -> emptyList()
        }
    }

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        val savedContext = context.towerDataContext
        val blockScope = CfirLocalScopeImpl()
        context.addLocalScope(blockScope)

        val result = transformer.expressionsTransformer.transformBlock(block, data)

        context.withTowerDataContext(savedContext) {}
        return result
    }

    private fun createImportingScopes(file: CfirFile): List<CfirScope> {
        val symbolProvider = session.symbolProvider
        val imports = file.imports
        return buildList {
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, symbolProvider))
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
            add(CfirExplicitStarImportingScope(imports, symbolProvider))
        }
    }

    private fun bumpPhase(declaration: CfirDeclaration) {
        if (declaration.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES &&
            declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE
        ) {
            declaration.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
        }
    }

    private fun resolveClassId(klass: CfirClass): ClassId? {
        val packageFqName = try {
            context.file.packageDirective.packageFqName
        } catch (_: UninitializedPropertyAccessException) {
            FqName.ROOT
        }
        val classNesting = context.containers
            .asSequence()
            .filterIsInstance<CfirClass>()
            .map(CfirClass::name)
            .toList()
        return classIdForClassNesting(packageFqName, classNesting) ?: ClassId(packageFqName, klass.name)
    }
}

