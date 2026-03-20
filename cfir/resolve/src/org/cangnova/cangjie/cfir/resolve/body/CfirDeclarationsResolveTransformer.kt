package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirFieldVariableImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirEnumConstructorImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternVariableImpl
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.*
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
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
    private val specificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session)

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

            function.valueParameters.forEach { param ->
                param.replaceReturnTypeRef(resolveExplicitTypeRefIfNeeded(param.returnTypeRef, function.typeParameters))
            }
            function.replaceReturnTypeRef(resolveExplicitTypeRefIfNeeded(function.returnTypeRef, function.typeParameters))

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

    override fun transformConstructor(constructor: CfirConstructor, data: CfirResolutionMode): CfirConstructor {
        val savedContext = context.towerDataContext

        context.withContainer(constructor) {
            if (constructor.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(constructor.typeParameters))
            }

            constructor.valueParameters.forEach { param ->
                param.replaceReturnTypeRef(resolveExplicitTypeRefIfNeeded(param.returnTypeRef, constructor.typeParameters))
            }

            val returnTypeRef = constructor.returnTypeRef
            if (returnTypeRef is CfirImplicitTypeRef) {
                val ownerClass = context.containers.filterIsInstance<CfirClass>().lastOrNull()
                val ownerType = ownerClass?.let(::buildConstructedTypeForClass) ?: ConeErrorType("constructor has no owning class")
                constructor.replaceReturnTypeRef(
                    buildResolvedTypeRef {
                        source = returnTypeRef.source
                        coneType = ownerType
                        delegatedTypeRef = returnTypeRef
                    },
                )
            } else {
                constructor.replaceReturnTypeRef(resolveExplicitTypeRefIfNeeded(returnTypeRef, constructor.typeParameters))
            }

            val paramScope = CfirLocalScopeImpl()
            for (param in constructor.valueParameters) {
                val paramSymbol = param.symbol as? CfirCallableSymbol<*> ?: continue
                paramScope.addVariable(param.name, paramSymbol)
            }
            context.addLocalScope(paramScope)

            val body = constructor.body
            if (body != null) {
                (constructor as? org.cangnova.cangjie.cfir.declarations.impl.CfirConstructorImpl)?.body =
                    body.transform<CfirBlock, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
            }
        }

        context.withTowerDataContext(savedContext) {}
        bumpPhase(constructor)
        return constructor
    }

    override fun transformEnumConstructor(
        enumConstructor: CfirEnumConstructor,
        data: CfirResolutionMode,
    ): CfirEnumConstructor {
        val savedContext = context.towerDataContext

        context.withContainer(enumConstructor) {
            val ownerEnum = context.containers.asReversed().filterIsInstance<CfirClass>().firstOrNull {
                it.classKind == CfirClassKind.ENUM
            }
            if (ownerEnum != null && enumConstructor.typeParameters.isEmpty()) {
                (enumConstructor as? CfirEnumConstructorImpl)?.typeParameters = ownerEnum.typeParameters
            }

            if (enumConstructor.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(enumConstructor.typeParameters))
            }

            if (enumConstructor.returnTypeRef !is CfirImplicitTypeRef) {
                enumConstructor.replaceReturnTypeRef(
                    resolveExplicitTypeRefIfNeeded(enumConstructor.returnTypeRef, enumConstructor.typeParameters),
                )
            }
        }

        context.withTowerDataContext(savedContext) {}
        bumpPhase(enumConstructor)
        return enumConstructor
    }

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirProperty {
        val savedContext = context.towerDataContext

        context.withContainer(property) {
            property.replaceReturnTypeRef(resolveExplicitTypeRefIfNeeded(property.returnTypeRef, property.typeParameters))
        }

        context.withTowerDataContext(savedContext) {}
        bumpPhase(property)
        return property
    }

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirVariable {
        bumpPhase(variable)
        return variable
    }

    override fun transformFieldVariable(
        fieldVariable: CfirFieldVariable,
        data: CfirResolutionMode,
    ): CfirFieldVariable {
        fieldVariable.replaceReturnTypeRef(
            resolveExplicitTypeRefIfNeeded(fieldVariable.returnTypeRef, fieldVariable.typeParameters),
        )

        val explicitTypeRef = fieldVariable.returnTypeRef
        val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
            CfirResolutionMode.WithExpectedType(explicitTypeRef.coneType)
        } else {
            CfirResolutionMode.ContextIndependent
        }

        val initializer = fieldVariable.initializer
        if (initializer != null) {
            (fieldVariable as? CfirFieldVariableImpl)?.initializer =
                initializer.transform<CfirExpression, CfirResolutionMode>(transformer, initializerMode)
        }

        if (fieldVariable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = fieldVariable.initializer?.coneTypeOrNull
            if (initType != null) {
                val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                fieldVariable.replaceReturnTypeRef(
                    buildResolvedTypeRef {
                        source = fieldVariable.returnTypeRef.source
                        coneType = resolvedType
                        delegatedTypeRef = fieldVariable.returnTypeRef
                    },
                )
            }
        }

        val varSymbol = fieldVariable.symbol as? CfirFieldVariableSymbol
        if (varSymbol != null) {
            context.storeVariable(fieldVariable.name, varSymbol)
        }

        bumpPhase(fieldVariable)
        return fieldVariable
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
            val resolved = specificTypeResolverTransformer.transformTypeRef(
                rawTypeRef,
                CfirTypeResolutionConfiguration(
                    useSiteFile = context.file,
                    topContainer = context.containers.lastOrNull(),
                ),
            )
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
        val defaultImportsProvider = session.defaultImportsProvider
        val defaultImports = defaultImportsProvider.getDefaultImports(includeLowPriorityImports = true)
            .filter { it.fqName !in defaultImportsProvider.excludedImports }
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
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, symbolProvider))
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
            add(CfirExplicitStarImportingScope(imports, symbolProvider))
            add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
            add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
        }
    }

    private fun resolveExplicitTypeRefIfNeeded(
        typeRef: CfirTypeRef,
        additionalTypeParameters: List<CfirTypeParameter> = emptyList(),
    ): CfirTypeRef {
        if (typeRef is CfirImplicitTypeRef) return typeRef
        val typeParametersFromContainers = context.containers
            .filterIsInstance<CfirDeclaration>()
            .flatMap(::extractTypeParameters)
        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(typeParametersFromContainers + additionalTypeParameters)

        if (typeRef is CfirResolvedTypeRef) {
            val delegated = typeRef.delegatedTypeRef
            if (typeRef.coneType is ConeErrorType && delegated != null && delegated !is CfirImplicitTypeRef) {
                return specificTypeResolverTransformer.transformTypeRef(delegated, config)
            }
            return typeRef
        }

        return specificTypeResolverTransformer.transformTypeRef(
            typeRef,
            config,
        )
    }

    private fun extractTypeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> {
        return when (declaration) {
            is CfirClass -> declaration.typeParameters
            is CfirFunction -> declaration.typeParameters
            is CfirConstructor -> declaration.typeParameters
            is CfirProperty -> declaration.typeParameters
            is CfirFieldVariable -> declaration.typeParameters
            is CfirValueParameter -> declaration.typeParameters
            is CfirExtend -> declaration.typeParameters
            is CfirTypeAlias -> declaration.typeParameters
            is CfirPatternVariable -> declaration.typeParameters
            is CfirMacroDeclaration -> declaration.typeParameters
            is CfirMainFunction -> declaration.typeParameters
            is CfirFinalizer -> declaration.typeParameters
            is CfirEnumConstructor -> declaration.typeParameters
            else -> emptyList()
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

    private fun buildConstructedTypeForClass(klass: CfirClass): ConeCangjieType {
        val classId = resolveClassId(klass) ?: return ConeErrorType("cannot resolve class id for constructor owner")
        val typeArguments = klass.typeParameters.map { ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString())) }
        val lookupTag = ConeClassLookupTagImpl(classId)
        return when (klass.classKind) {
            CfirClassKind.CLASS, CfirClassKind.INTERFACE -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
                isInterface = klass.classKind == CfirClassKind.INTERFACE,
            )
            CfirClassKind.STRUCT -> ConeStructType(lookupTag, typeArguments)
            CfirClassKind.ENUM -> ConeEnumType(lookupTag, typeArguments)
        }
    }
}
