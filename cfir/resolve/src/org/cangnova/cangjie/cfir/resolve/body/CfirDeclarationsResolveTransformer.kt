package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.*
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
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
open class CfirDeclarationsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {
    private val specificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session)

    override fun transformFile(file: CfirFile, data: ResolutionMode): CfirFile {
        val savedContext = context.towerDataContext
        context.withFile(file) {
            val importScopes = createImportingScopes(file)
            context.addNonLocalScopes(importScopes)

            file.transformDeclarations(transformer, ResolutionMode.ContextIndependent)
        }
        context.replaceTowerDataContext(savedContext)
        return file
    }

    override fun transformClass(klass: CfirClass, data: ResolutionMode): CfirClass {
        return transformClassLikeDeclaration(klass)
    }

    override fun transformInterface(`interface`: CfirInterface, data: ResolutionMode): CfirInterface {
        return transformClassLikeDeclaration(`interface`)
    }

    override fun transformStruct(struct: CfirStruct, data: ResolutionMode): CfirStruct {
        return transformClassLikeDeclaration(struct)
    }

    override fun transformEnum(enum: CfirEnum, data: ResolutionMode): CfirEnum {
        return transformClassLikeDeclaration(enum)
    }

    private fun <T : CfirClassLikeDeclaration> transformClassLikeDeclaration(classLike: T): T {
        val savedContext = context.towerDataContext

        context.withContainer(classLike) {
            val resolveDeclarations = {
                // 接口成员在 CFIR 树中只保留 declarations 这一条主存。
                // 不能再把它回写到并行镜像列表，否则会重新引入重复遍历。
                classLike.transformDeclarations(transformer, ResolutionMode.ContextIndependent)
            }

            when (classLike) {
                is CfirClass -> {
                    context.withContainingClass(classLike) {
                        context.withScopesForClass(classLike, components, resolveDeclarations)
                    }
                }

                else -> context.withScopesForClass(classLike, components, resolveDeclarations)
            }
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(classLike)
        return classLike
    }

    override fun transformFunction(function: CfirFunction, data: ResolutionMode): CfirFunction {
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
                function.transformBody(transformer, ResolutionMode.ContextIndependent)
            }

            if (function.returnTypeRef is CfirImplicitTypeRef) {
                val bodyType = function.body?.coneTypeOrNull ?: session.builtinTypes.unitType
                val resolvedType = IdealTypeResolver.resolveIfIdeal(bodyType)
                function.replaceReturnTypeRef(
                    function.returnTypeRef.resolvedTypeFromPrototype(resolvedType, function.returnTypeRef.source),
                )
            }
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(function)
        return function
    }

    override fun transformConstructor(constructor: CfirConstructor, data: ResolutionMode): CfirConstructor {
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
                val ownerClass = context.containers.filterIsInstance<CfirClassLikeDeclaration>().lastOrNull()
                val ownerType = ownerClass?.let(::buildConstructedTypeForClass)
                    ?: ConeErrorType(ConeSimpleDiagnostic("constructor has no owning class"))
                constructor.replaceReturnTypeRef(
                    returnTypeRef.resolvedTypeFromPrototype(ownerType, returnTypeRef.source),
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
                constructor.transformBody(transformer, ResolutionMode.ContextIndependent)
            }
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(constructor)
        return constructor
    }

    override fun transformEnumConstructor(
        enumConstructor: CfirEnumConstructor,
        data: ResolutionMode,
    ): CfirEnumConstructor {
        val savedContext = context.towerDataContext

        context.withContainer(enumConstructor) {
            if (enumConstructor.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(enumConstructor.typeParameters))
            }

            enumConstructor.valueParameters.forEach { parameter ->
                parameter.replaceReturnTypeRef(
                    resolveExplicitTypeRefIfNeeded(parameter.returnTypeRef, enumConstructor.typeParameters),
                )
            }

            val returnTypeRef = enumConstructor.returnTypeRef
            if (returnTypeRef is CfirImplicitTypeRef) {
                val ownerEnum = context.containers.filterIsInstance<CfirEnum>().lastOrNull()
                val ownerType = ownerEnum?.let(::buildConstructedTypeForClass)
                    ?: ConeErrorType(ConeSimpleDiagnostic("enum constructor has no owning enum"))
                enumConstructor.replaceReturnTypeRef(
                    returnTypeRef.resolvedTypeFromPrototype(ownerType, returnTypeRef.source),
                )
            } else {
                enumConstructor.replaceReturnTypeRef(
                    resolveExplicitTypeRefIfNeeded(returnTypeRef, enumConstructor.typeParameters),
                )
            }
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(enumConstructor)
        return enumConstructor
    }

    override fun transformProperty(property: CfirProperty, data: ResolutionMode): CfirProperty {
        val savedContext = context.towerDataContext

        context.withContainer(property) {
            property.replaceReturnTypeRef(resolveExplicitTypeRefIfNeeded(property.returnTypeRef, property.typeParameters))
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(property)
        return property
    }

    override fun transformVariable(variable: CfirVariable, data: ResolutionMode): CfirVariable {
        bumpPhase(variable)
        return variable
    }

    override fun transformFieldVariable(
        fieldVariable: CfirFieldVariable,
        data: ResolutionMode,
    ): CfirFieldVariable {
        fieldVariable.replaceReturnTypeRef(
            resolveExplicitTypeRefIfNeeded(fieldVariable.returnTypeRef, fieldVariable.typeParameters),
        )

        val explicitTypeRef = fieldVariable.returnTypeRef
        val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
            ResolutionMode.WithExpectedType(explicitTypeRef )
        } else {
            ResolutionMode.ContextIndependent
        }

        val initializer = fieldVariable.initializer
        if (initializer != null) {
            fieldVariable.transformInitializer(transformer, initializerMode)
        }

        if (fieldVariable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = fieldVariable.initializer?.coneTypeOrNull
            if (initType != null) {
                val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                fieldVariable.replaceReturnTypeRef(
                    fieldVariable.returnTypeRef.resolvedTypeFromPrototype(
                        resolvedType,
                        fieldVariable.returnTypeRef.source,
                    ),
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

    override fun transformDeclaration(declaration: CfirDeclaration, data: ResolutionMode): CfirDeclaration {
        bumpPhase(declaration)
        return declaration
    }

    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: ResolutionMode,
    ): CfirPatternBindingVariable {
        patternBindingVariable.replaceReturnTypeRef(
            resolveExplicitTypeRefIfNeeded(patternBindingVariable.returnTypeRef, patternBindingVariable.typeParameters),
        )
        bumpPhase(patternBindingVariable)
        return patternBindingVariable
    }

    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: ResolutionMode,
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
            ResolutionMode.WithExpectedType(explicitTypeRef)
        } else {
            ResolutionMode.ContextIndependent
        }

        val initializer = patternVariable.initializer
        if (initializer != null) {
            patternVariable.transformInitializer(transformer, initializerMode)
        }

        if (patternVariable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = patternVariable.initializer?.coneTypeOrNull
            if (initType != null) {
                val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                patternVariable.replaceReturnTypeRef(
                    patternVariable.returnTypeRef.resolvedTypeFromPrototype(
                        resolvedType,
                        patternVariable.returnTypeRef.source,
                    ),
                )
            }
        }

        patternVariable.transformPattern(transformer, ResolutionMode.ContextIndependent)
        resolvePatternBindingTypes(
            pattern = patternVariable.pattern,
            expectedType = patternVariable.returnTypeRef.coneTypeOrNull,
            typeResolver = specificTypeResolverTransformer,
        )
        registerPatternBindings(patternVariable.pattern)

        bumpPhase(patternVariable)
        return patternVariable
    }

    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression {
        val savedContext = context.towerDataContext
        val blockScope = CfirLocalScopeImpl()
        context.addLocalScope(blockScope)

        val result = transformer.expressionsTransformer.transformBlock(block, data)

        context.replaceTowerDataContext(savedContext)
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
            // 声明解析阶段同样要先看到当前文件顶层声明，保证后续类型与 extend 规则建立在正确的本地符号之上。
            add(CfirFileDeclaredTopLevelScope(file))
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
            is CfirInterface -> declaration.typeParameters
            is CfirStruct -> declaration.typeParameters
            is CfirEnum -> declaration.typeParameters
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
            is CfirFunction -> declaration.typeParameters

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

    private fun resolveClassId(klass: CfirClassLikeDeclaration): ClassId? {
        val packageFqName = try {
            context.file.packageDirective.packageFqName
        } catch (_: UninitializedPropertyAccessException) {
            FqName.ROOT
        }

        // 当前 class-like 自身会出现在容器栈里；只有当它外面还包着别的 class-like，
        // 才说明它不属于公开类型标识体系，此时不能再为它构造稳定 ClassId。
        val classLikeContainers = context.containers.filterIsInstance<CfirClassLikeDeclaration>()
        val hasOuterClassLike = classLikeContainers.any { it !== klass }
        return if (hasOuterClassLike) {
            null
        } else {
            ClassId(packageFqName, klass.name)
        }
    }

    private fun buildConstructedTypeForClass(klass: CfirClassLikeDeclaration): ConeCangJieType {
        val classId = resolveClassId(klass)
            ?: return ConeErrorType(ConeSimpleDiagnostic("cannot resolve class id for constructor owner"))
        val typeArguments = klass.typeParameters.map { parameter ->
            ConeTypeProjection(ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag()))
        }
        val lookupTag = ConeClassLikeLookupTagImpl(classId)
        return when (klass) {
            is CfirInterface -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
                isInterface = true,
            )
            is CfirStruct -> ConeStructType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
            )
            is CfirEnum -> ConeEnumType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
                isRefEnum = klass.isRefEnum,
            )
            else -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
            )
        }
    }
}
