/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.ThisTypeResolutionContext
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * TYPES 阶段处理器。
 * 它负责把声明头中的显式类型引用解析成 `CfirResolvedTypeRef`。
 */
class CfirTypeResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(session, scopeSession, CfirResolvePhase.TYPES) {
    /** TYPES 阶段实际使用的树转换器实例。 */
    private val typeResolveTransformer = CfirTypeResolveTransformer(session, scopeSession)

    /** 通用 resolve processor 暴露的 transformer 视图。 */
    @Suppress("UNCHECKED_CAST")
    override val transformer get() = typeResolveTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    /**
     * 推进单个文件的 TYPES 阶段。
     */
    override fun processFile(file: CfirFile) {
        typeResolveTransformer.transformFile(file, CfirTypeResolutionConfiguration.EMPTY)
    }
}

/**
 * TYPES 阶段转换器。
 * 它遍历 CFIR 树，只处理声明头中的类型引用，不触碰函数体和隐式类型。
 */
class CfirTypeResolveTransformer(
    /** 当前 CFIR 会话，提供 symbol provider、import binding、默认导入和类型上下文。 */
    override val session: CfirSession,
    /** 当前文件解析过程中复用的 scope session。 */
    private val scopeSession: ScopeSession,
) : CfirAbstractTreeTransformer<CfirTypeResolutionConfiguration>(CfirResolvePhase.TYPES) {
    /** 单个类型引用解析器，负责把具体 `CfirTypeRef` 转为 resolved type ref。 */
    private val typeResolverTransformer = CfirSpecificTypeResolverTransformer(session)
    /** 当前 use-site 文件，供 low-level TYPES 和导入 scope 构造复用。 */
    private var currentFile: CfirFile? = null
    /** 当前 low-level 指定解析路径中的外围 class-like 栈。 */
    private val classDeclarationsStack: ArrayDeque<CfirClassLikeDeclaration> = ArrayDeque()

    // ---- 声明遍历 ----

    /**
     * 解析文件级 TYPES 阶段，并建立当前文件的导入与声明 scope。
     */
    override fun transformFile(file: CfirFile, data: CfirTypeResolutionConfiguration): CfirFile {
        checkSessionConsistency(file)
        return withFileScope(file) {
            super.transformFile(file, buildConfiguration(file))
        }
    }

    /**
     * 解析 class 声明头，包括隐式默认构造器、类型参数、父类型、注解和成员声明头。
     */
    override fun transformClass(klass: CfirClass, data: CfirTypeResolutionConfiguration): CfirClass {
        ensureImplicitDefaultConstructorIfNeeded(klass)

        transformClassLikeHeader(klass, data)
        bumpPhase(klass)
        return klass
    }

    /**
     * 解析 interface 声明头。
     */
    override fun transformInterface(interfaceDeclaration: CfirInterface, data: CfirTypeResolutionConfiguration): CfirInterface {
        transformClassLikeHeader(interfaceDeclaration, data)
        bumpPhase(interfaceDeclaration)
        return interfaceDeclaration
    }

    /**
     * 解析 struct 声明头。
     */
    override fun transformStruct(struct: CfirStruct, data: CfirTypeResolutionConfiguration): CfirStruct {
        transformClassLikeHeader(struct, data)
        bumpPhase(struct)
        return struct
    }

    /**
     * 解析 enum 声明头。
     */
    override fun transformEnum(enum: CfirEnum, data: CfirTypeResolutionConfiguration): CfirEnum {
        transformClassLikeHeader(enum, data)
        bumpPhase(enum)
        return enum
    }

    /**
     * 解析通用 class-like 声明头。
     */
    private fun transformClassLikeHeader(
        declaration: CfirClassLikeDeclaration,
        data: CfirTypeResolutionConfiguration,
    ) {
        val configuration = data
            .withTopContainer(declaration)
            .withAdditionalTypeParameters(declaration.typeParametersForResolution())
        declaration.transformTypeParameters(this, configuration)
        declaration.transformSuperTypeRefs(this, configuration)
        declaration.transformAnnotations(this, configuration)
        declaration.transformDeclarations(this, configuration)
    }

    /**
     * 解析 extend 声明头，并在解析父类型前建立被扩展类型的 `This` 类型上下文。
     */
    override fun transformExtend(extend: CfirExtend, data: CfirTypeResolutionConfiguration): CfirExtend {
        var configuration = data
            .withTopContainer(extend)
            .withAdditionalTypeParameters(extend.typeParameters)
        extend.transformTypeParameters(this, configuration)
        extend.transformExtendedTypeRef(this, configuration)
        extend.exposeExtendedTypeAssumptionBounds()
        configuration = configuration.withThisTypeContext(thisTypeContextForExtend(extend))
        extend.transformSuperTypeRefs(this, configuration)
        extend.transformAnnotations(this, configuration)
        extend.transformDeclarations(this, configuration)
        bumpPhase(extend)
        return extend
    }

    /**
     * 解析普通命名函数声明头。
     */
    override fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        data: CfirTypeResolutionConfiguration,
    ): CfirNamedFunction = transformFunctionHeader(namedFunction, data) as CfirNamedFunction

    /**
     * 解析 `main` 函数声明头。
     */
    override fun transformMainFunction(
        mainFunction: CfirMainFunction,
        data: CfirTypeResolutionConfiguration,
    ): CfirMainFunction = transformFunctionHeader(mainFunction, data) as CfirMainFunction

    /**
     * 解析宏声明头。
     */
    override fun transformMacroDeclaration(
        macroDeclaration: CfirMacroDeclaration,
        data: CfirTypeResolutionConfiguration,
    ): CfirMacroDeclaration = transformFunctionHeader(macroDeclaration, data) as CfirMacroDeclaration

    /**
     * 解析 finalizer 声明头。
     */
    override fun transformFinalizer(
        finalizer: CfirFinalizer,
        data: CfirTypeResolutionConfiguration,
    ): CfirFinalizer = transformFunctionHeader(finalizer, data) as CfirFinalizer

    /**
     * 解析通用函数声明头。
     */
    override fun transformFunction(function: CfirFunction, data: CfirTypeResolutionConfiguration): CfirFunction {
        return transformFunctionHeader(function, data)
    }

    /**
     * 解析函数型声明的公共头部。
     *
     * TYPES 阶段只处理类型参数、返回类型、值参数和注解，不进入函数体。
     */
    private fun transformFunctionHeader(function: CfirFunction, data: CfirTypeResolutionConfiguration): CfirFunction {
        val configuration = data
            .withTopContainer(function)
            .withAdditionalTypeParameters(function.typeParameters)
        function.transformTypeParameters(this, configuration)
        function.transformReturnTypeRef(
            this,
            configuration.withThisTypeContext(thisTypeContextForFunctionReturn(function, data)),
        )
        function.transformValueParameters(this, configuration)
        function.transformAnnotations(this, configuration)
        // TYPES 阶段不解析函数体
        bumpPhase(function)
        return function
    }

    /**
     * 计算函数返回类型位置可见的 `This` 类型解析上下文。
     *
     * class 成员函数可在非 static 返回类型中使用 `This`；static 成员和 extend 语境下会保留诊断信息或禁用标记。
     */
    private fun thisTypeContextForFunctionReturn(
        function: CfirFunction,
        data: CfirTypeResolutionConfiguration,
    ): ThisTypeResolutionContext? {
        if (function !is CfirNamedFunction) return null

        val inherited = data.thisTypeContext?.asDisallowed()
        return when (val containingDeclaration = data.topContainer) {
            is CfirClass -> {
                val thisType = containingDeclaration.constructClassThisType() ?: return inherited
                ThisTypeResolutionContext(
                    type = thisType,
                    isAllowed = !function.status.isStatic,
                    disallowedDiagnosticKind = if (function.status.isStatic) {
                        DiagnosticKind.InvalidThisTypePosition
                    } else {
                        DiagnosticKind.ThisTypeNotAllowed
                    },
                )
            }

            is CfirExtend -> inherited ?: thisTypeContextForExtend(containingDeclaration)
            else -> inherited
        }
    }

    /**
     * 为 extend 声明创建禁用状态的 `This` 类型解析上下文。
     */
    private fun thisTypeContextForExtend(extend: CfirExtend): ThisTypeResolutionContext? {
        val extendedType = extend.extendedTypeRef.coneTypeOrNull ?: return null
        return ThisTypeResolutionContext(extendedType, isAllowed = false)
    }

    /**
     * 构造 class 内部 `This` 类型，类型实参使用该 class 自身的类型参数。
     */
    private fun CfirClass.constructClassThisType(): ConeCangJieType? {
        val classSymbol = symbol as? CfirClassSymbol ?: return null
        val typeArguments = typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        return classSymbol.constructThisType(typeArguments)
    }

    /**
     * 解析普通构造器声明头，并为隐式返回类型写入 owner 构造类型。
     */
    override fun transformConstructor(constructor: CfirConstructor, data: CfirTypeResolutionConfiguration): CfirConstructor {
        val configuration = data
            .withTopContainer(constructor)
            .withAdditionalTypeParameters(constructor.typeParameters)
        constructor.transformTypeParameters(this, configuration)
        if (constructor.returnTypeRef is CfirImplicitTypeRef) {
            val ownerClass = data.topContainer as? CfirClassLikeDeclaration
            val ownerType = ownerClass?.let(::buildConstructedTypeForConstructorOwner)
                ?: ConeErrorType(ConeSimpleDiagnostic("cannot resolve constructor owner type"))
            constructor.replaceReturnTypeRef(
                constructor.returnTypeRef.resolvedTypeFromPrototype(ownerType, constructor.returnTypeRef.source),
            )
        } else {
            constructor.transformReturnTypeRef(this, configuration)
        }
        constructor.transformValueParameters(this, configuration)
        constructor.transformAnnotations(this, configuration)
        bumpPhase(constructor)
        return constructor
    }

    /**
     * 解析 enum constructor 声明头，并为隐式返回类型写入 enum owner 构造类型。
     */
    override fun transformEnumConstructor(
        enumConstructor: CfirEnumConstructor,
        data: CfirTypeResolutionConfiguration,
    ): CfirEnumConstructor {
        val configuration = data
            .withTopContainer(enumConstructor)
            .withAdditionalTypeParameters(enumConstructor.typeParameters)
        enumConstructor.transformTypeParameters(this, configuration)
        if (enumConstructor.returnTypeRef is CfirImplicitTypeRef) {
            val ownerEnum = data.topContainer as? CfirEnum
            val ownerType = ownerEnum?.let(::buildConstructedTypeForConstructorOwner)
                ?: ConeErrorType(ConeSimpleDiagnostic("cannot resolve enum constructor owner type"))
            enumConstructor.replaceReturnTypeRef(
                enumConstructor.returnTypeRef.resolvedTypeFromPrototype(
                    ownerType,
                    enumConstructor.returnTypeRef.source,
                ),
            )
        } else {
            enumConstructor.transformReturnTypeRef(this, configuration)
        }
        enumConstructor.transformValueParameters(this, configuration)
        enumConstructor.transformAnnotations(this, configuration)
        bumpPhase(enumConstructor)
        return enumConstructor
    }

    /**
     * 解析属性声明头，包括类型参数、属性类型、访问器和注解。
     */
    override fun transformProperty(property: CfirProperty, data: CfirTypeResolutionConfiguration): CfirProperty {
        val configuration = data
            .withTopContainer(property)
            .withAdditionalTypeParameters(property.typeParameters)
        property.transformTypeParameters(this, configuration)
        property.transformReturnTypeRef(this, configuration)
        property.transformGetter(this, configuration)
        property.transformSetter(this, configuration)
        property.transformAnnotations(this, configuration)
        bumpPhase(property)
        return property
    }

    /**
     * 解析属性访问器声明头。
     */
    override fun transformPropertyAccessor(
        propertyAccessor: CfirPropertyAccessor,
        data: CfirTypeResolutionConfiguration,
    ): CfirPropertyAccessor = transformFunctionHeader(propertyAccessor, data) as CfirPropertyAccessor

    /**
     * 普通局部变量在 TYPES 阶段不解析隐式类型，只推进阶段标记。
     */
    override fun transformVariable(variable: CfirVariable, data: CfirTypeResolutionConfiguration): CfirVariable {
        bumpPhase(variable)
        return variable
    }

    /**
     * 解析字段变量的显式类型与注解。
     */
    override fun transformFieldVariable(
        fieldVariable: CfirFieldVariable,
        data: CfirTypeResolutionConfiguration,
    ): CfirFieldVariable {
        fieldVariable.transformReturnTypeRef(this, data)
        fieldVariable.transformAnnotations(this, data)
        bumpPhase(fieldVariable)
        return fieldVariable
    }

    /**
     * 解析模式绑定变量的显式类型与注解。
     */
    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: CfirTypeResolutionConfiguration,
    ): CfirPatternBindingVariable {
        patternBindingVariable.transformReturnTypeRef(this, data)
        patternBindingVariable.transformAnnotations(this, data)
        bumpPhase(patternBindingVariable)
        return patternBindingVariable
    }

    /**
     * 解析模式变量的显式类型与注解。
     */
    override fun transformPatternVariable(patternVariable: CfirPatternVariable, data: CfirTypeResolutionConfiguration): CfirPatternVariable {
        patternVariable.transformReturnTypeRef(this, data)
        patternVariable.transformAnnotations(this, data)
        bumpPhase(patternVariable)
        return patternVariable
    }

    /**
     * 解析值参数的类型引用和注解。
     */
    override fun transformValueParameter(valueParameter: CfirValueParameter, data: CfirTypeResolutionConfiguration): CfirValueParameter {
        valueParameter.transformReturnTypeRef(this, data)
        valueParameter.transformAnnotations(this, data)
        return valueParameter
    }

    /**
     * 解析类型参数的注解与上界。
     */
    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: CfirTypeResolutionConfiguration): CfirTypeParameter {
        typeParameter.transformAnnotations(this, data)
        typeParameter.transformBounds(this, data)
        bumpPhase(typeParameter)
        return typeParameter
    }

    /**
     * 解析 typealias 的类型参数、展开类型和注解。
     */
    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: CfirTypeResolutionConfiguration): CfirTypeAlias {
        val configuration = data
            .withTopContainer(typeAlias)
            .withAdditionalTypeParameters(typeAlias.typeParameters)
        typeAlias.transformTypeParameters(this, configuration)
        typeAlias.transformExpandedTypeRef(this, configuration)
        typeAlias.transformAnnotations(this, configuration)
        bumpPhase(typeAlias)
        return typeAlias
    }

    // ---- 类型解析 ----

    /**
     * 解析任意类型引用。
     */
    override fun transformTypeRef(typeRef: CfirTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        return typeResolverTransformer.transformTypeRef(typeRef, data)
    }

    /**
     * 解析用户书写的类型引用。
     */
    override fun transformUserTypeRef(userTypeRef: CfirUserTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        return transformTypeRef(userTypeRef, data)
    }

    /**
     * 已解析类型引用在 TYPES 阶段保持不变。
     */
    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        return resolvedTypeRef
    }

    /**
     * 隐式类型引用留给 IMPLICIT_TYPES 阶段处理。
     */
    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        // 隐式类型留给 IMPLICIT_TYPES 阶段处理
        return implicitTypeRef
    }

    /**
     * 基础类型引用统一交给类型解析器处理。
     */
    override fun transformBasicTypeRef(basicTypeRef: CfirBasicTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        // 基础类型也统一走类型引用解析逻辑
        return transformTypeRef(basicTypeRef, data)
    }

    // ---- 跳过函数体 ----

    /**
     * TYPES 阶段不进入函数体 block。
     */
    override fun transformBlock(block: CfirBlock, data: CfirTypeResolutionConfiguration): CfirExpression {
        return block
    }

    /**
     * 对齐 Kotlin `FirTypeResolveTransformer.withFileScope`：
     * 统一建立当前 use-site file 上下文，供 low-level TYPES 指定解析复用。
     */
    fun <R> withFileScope(file: CfirFile, action: () -> R): R {
        val previousFile = currentFile
        currentFile = file
        return try {
            action()
        } finally {
            currentFile = previousFile
        }
    }

    /**
     * 对齐 Kotlin `withClassDeclarationCleanup`：
     * 维护外围 class-like 声明栈，保证 designated lazy resolve 的类型配置与主干一致。
     */
    fun <R> withClassDeclarationCleanup(classLike: CfirClassLikeDeclaration, action: () -> R): R {
        classDeclarationsStack.addLast(classLike)
        return try {
            action()
        } finally {
            classDeclarationsStack.removeLast()
        }
    }

    /**
     * 对齐 Kotlin `withScopeCleanup`。
     *
     * 仓颉当前类型解析配置是值对象拼装，没有 Kotlin FIR 那套持久化 scope 栈状态，
     * 因而这里主要承担结构对齐与未来扩展挂载点职责。
     */
    fun <R> withScopeCleanup(action: () -> R): R = action()

    /**
     * 对齐 Kotlin `withClassScopes`：
     * 在当前 file / 外围 class 语境下执行 action，但不在这里递归改写 nested declarations。
     */
    fun <R> withClassScopes(classLike: CfirClassLikeDeclaration, action: () -> R): R {
        return withClassDeclarationCleanup(classLike, action)
    }

    /**
     * 对齐 Kotlin `transformClassTypeParameters + resolveClassTypes`：
     * low-level TYPES 在 class 锁内只解析 class 自身头部，不递归推进其成员。
     */
    fun resolveClassTypes(classLike: CfirClassLikeDeclaration) {
        if (classLike is CfirClass) {
            ensureImplicitDefaultConstructorIfNeeded(classLike)
        }

        val configuration = buildConfiguration(classLike)
        transformClassTypeParameters(classLike, configuration)
        withScopeCleanup {
            classLike.transformAnnotations(this, configuration)
        }
    }

    /**
     * 对齐 Kotlin `transformClassTypeParameters`。
     */
    fun transformClassTypeParameters(
        classLike: CfirClassLikeDeclaration,
        data: CfirTypeResolutionConfiguration,
    ) {
        val configuration = data
            .withTopContainer(classLike)
            .withAdditionalTypeParameters(classLike.typeParametersForResolution())
        classLike.transformTypeParameters(this, configuration)
    }

    /**
     * `extend` 在 low-level TYPES 中只处理自身头部；
     * 其成员声明由各自 designated target 在独立锁下推进。
     */
    fun resolveExtendTypes(extend: CfirExtend) {
        val configuration = buildConfiguration(extend)
            .withTopContainer(extend)
            .withAdditionalTypeParameters(extend.typeParameters)
        extend.transformTypeParameters(this, configuration)
        extend.transformExtendedTypeRef(this, configuration)
        extend.exposeExtendedTypeAssumptionBounds()
        extend.transformSuperTypeRefs(this, configuration)
        extend.transformAnnotations(this, configuration)
    }

    /**
     * 对齐官方 `AddAssumptionForExtendDecls`：
     *
     * `extend<R> A<R>` 必须把 `A<T> where T <: C` 投射成 `R <: C`，
     * 否则 extend 体内的 `R.i` 这类上界静态访问和目标类型实参检查都会丢失依据。
     */
    private fun CfirExtend.exposeExtendedTypeAssumptionBounds() {
        if (typeParameters.isEmpty()) return
        val extendedType = (extendedTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeLookupTagBasedType ?: return
        val targetDeclaration = extendedType.toAssumptionTargetDeclaration() ?: return
        val targetTypeParameters = targetDeclaration.typeParameters
        if (targetTypeParameters.isEmpty() || extendedType.typeArguments.isEmpty()) return

        val reverseMapping = linkedMapOf<TypeConstructorMarker, MutableSet<TypeConstructorMarker>>()
        collectReverseTypeMapping(
            parameterTypes = targetTypeParameters.map { it.symbol.constructType() },
            argumentTypes = extendedType.typeArguments.map { it.type },
            destination = reverseMapping,
        )
        if (reverseMapping.isEmpty()) return

        val targetSubstitutor = CfirTypeSubstitutorByMap(
            targetTypeParameters.zip(extendedType.typeArguments).associate { (parameter, argument) ->
                parameter.symbol.toLookupTag() to argument.type
            },
        )
        val assumptionBounds = collectAssumptionBounds(targetDeclaration)

        for (typeParameter in typeParameters) {
            val constraintConstructors = reverseMapping[typeParameter.symbol.toLookupTag()].orEmpty()
            if (constraintConstructors.isEmpty()) continue

            val projectedBounds = constraintConstructors
                .flatMap { assumptionBounds[it].orEmpty() }
                .map { targetSubstitutor.substituteOrSelf(it) }
                .filter { it.isValidAssumptionUpperBound() }

            typeParameter.appendAssumptionBounds(projectedBounds)
        }
    }

    /**
     * 递归构造官方 `GetRevTypeMapping` 的 CFIR 等价物。
     *
     * key 是 extend 自身类型参数，value 是它在目标类型实参结构中对应到的声明侧类型参数。
     */
    private fun collectReverseTypeMapping(
        parameterTypes: List<ConeCangJieType>,
        argumentTypes: List<ConeCangJieType>,
        destination: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>>,
    ) {
        if (parameterTypes.size != argumentTypes.size) return
        for (index in argumentTypes.indices) {
            val parameterType = parameterTypes[index] as? ConeTypeParameterType ?: continue
            when (val argumentType = argumentTypes[index]) {
                is ConeTypeParameterType -> {
                    destination.getOrPut(argumentType.lookupTag) { linkedSetOf() } += parameterType.lookupTag
                }
                is ConeLookupTagBasedType -> {
                    val nestedDeclaration = argumentType.toAssumptionTargetDeclaration() ?: continue
                    collectReverseTypeMapping(
                        parameterTypes = nestedDeclaration.typeParameters.map { it.symbol.constructType() },
                        argumentTypes = argumentType.typeArguments.map { it.type },
                        destination = destination,
                    )
                }
                else -> Unit
            }
        }
    }

    /**
     * 收集目标声明及其上界中出现的声明侧泛型 assumptions。
     */
    private fun collectAssumptionBounds(
        declaration: CfirTypeParameterRefsOwner,
        destination: MutableMap<TypeConstructorMarker, MutableSet<ConeCangJieType>> = linkedMapOf(),
        visitedDeclarations: MutableSet<CfirTypeParameterRefsOwner> = linkedSetOf(),
    ): Map<TypeConstructorMarker, Set<ConeCangJieType>> {
        if (!visitedDeclarations.add(declaration)) return destination

        for (typeParameter in declaration.typeParameters) {
            val key = typeParameter.symbol.toLookupTag()
            val bounds = destination.getOrPut(key) { linkedSetOf() }
            for (bound in typeParameter.symbol.resolvedBounds) {
                val boundType = bound.coneType
                bounds += boundType
                (boundType as? ConeLookupTagBasedType)?.toAssumptionTargetDeclaration()?.let {
                    collectAssumptionBounds(it, destination, visitedDeclarations)
                }
            }
        }

        return destination
    }

    /**
     * assumption bound 只合成可参与上界成员/子类型推导的 class-like 类型。
     *
     * 非 class/interface 上界继续由声明侧 checker 报告，避免在同一个类型参数上制造级联诊断。
     */
    private fun ConeCangJieType.isValidAssumptionUpperBound(): Boolean {
        if (this is ConeErrorType) return false
        return fullyExpandedType(session) is ConeClassLikeType
    }

    /**
     * 将 lookup-tag 类型解析为可继续收集 assumption bound 的声明。
     */
    private fun ConeLookupTagBasedType.toAssumptionTargetDeclaration(): CfirTypeParameterRefsOwner? {
        val classId = classIdOrPrimitiveClassId ?: return null
        return session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirTypeParameterRefsOwner
    }

    /**
     * 把推导出的 assumption bounds 追加到当前类型参数声明。
     */
    private fun CfirTypeParameter.appendAssumptionBounds(boundsToAdd: List<ConeCangJieType>) {
        if (boundsToAdd.isEmpty()) return
        if (this !is org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl) return

        val existingBounds = bounds.mapNotNull { it.coneTypeOrNull }.toMutableSet()
        for (boundType in boundsToAdd) {
            if (!existingBounds.add(boundType)) continue
            bounds += boundType.toCfirResolvedTypeRef()
        }
    }

    /**
     * 文件级 TYPES 只建立 file 语境并处理文件自身头部信息，
     * 不能像全量编译那样在同一把 file 锁里递归推进所有 nested declarations。
     */
    fun resolveFileTypes(file: CfirFile) {
        withFileScope(file) {
            file.transformAnnotations(this, buildConfiguration(file))
        }
    }

    /**
     * 推进声明的 resolve phase。
     */
    private fun bumpPhase(declaration: CfirDeclaration) {
        declaration.replaceResolvePhase(CfirResolvePhase.TYPES)
    }

    /**
     * 为没有显式构造器的 class 补入隐式默认构造器。
     *
     * 该构造器在 TYPES 阶段获得 owner 类型作为返回类型，保证后续 low-level 阶段不会看到未完成声明头。
     */
    private fun ensureImplicitDefaultConstructorIfNeeded(klass: CfirClass) {
        if (klass.declarations.any { it is CfirConstructor }) return

        val classImpl = klass as? CfirClassImpl ?: return
        val symbol = CfirConstructorSymbol(CallableId(SpecialNames.INIT))
        val constructor = buildConstructor {
            source = klass.source
            moduleData = klass.moduleData
            this.symbol = symbol
            origin = CfirDeclarationOrigin.ImplicitDefault
            attributes = CfirDeclarationAttributes.EMPTY
            status = klass.status
            returnTypeRef = buildImplicitTypeRef {}
            body = null
        }

        classImpl.declarations += constructor
    }

    /**
     * 构造器返回类型在仓颉 CFIR 中直接建模为 `returnTypeRef`。
     * 它不依赖 body 推断，TYPES 阶段就应回填为所属 class-like 的构造后类型，
     * 否则 low-level 的 IMPLICIT_TYPES 校验会把 constructor 误判为未完成。
     */
    private fun buildConstructedTypeForConstructorOwner(owner: CfirClassLikeDeclaration): ConeCangJieType {
        val ownerSymbol = owner.symbol as? CfirClassLikeSymbol<*>
            ?: return ConeErrorType(ConeSimpleDiagnostic("constructor owner has no class-like symbol"))
        val typeArguments = owner.typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        return ownerSymbol.constructType(typeArguments)
    }

    /**
     * 基于当前文件和外围 class-like 栈构造类型解析配置。
     */
    private fun buildConfiguration(topContainer: CfirDeclaration): CfirTypeResolutionConfiguration {
        var configuration = CfirTypeResolutionConfiguration.EMPTY.withTopContainer(topContainer)

        currentFile?.let { file ->
            configuration = configuration
                .withUseSiteFile(file)
                .withScopes(createImportingScopes(file))
        }

        val containingClasses = classDeclarationsStack.filterIsInstance<CfirClass>()
        if (containingClasses.isNotEmpty()) {
            configuration = configuration.withContainingClassDeclarations(containingClasses)
            for (containingClass in containingClasses) {
                configuration = configuration.withAdditionalTypeParameters(containingClass.typeParameters)
            }
        }

        return configuration
    }

    /**
     * 构造 TYPES 阶段类型名查找使用的导入 scope 列表。
     *
     * 顺序从高到低排列：文件声明、当前包成员、显式 simple import、显式 star import、
     * 默认 simple import、默认 star import。
     */
    private fun createImportingScopes(file: CfirFile): List<CfirScope> {
        val symbolProvider = session.symbolProvider
        val imports = file.imports
        val resolvedImports = session.importBindingStoreOrNull?.getBindings(file)?.imports
        val defaultImports = session.defaultImportsProvider
            .getDefaultImports(includeLowPriorityImports = true)
            .filter { it.fqName !in session.defaultImportsProvider.excludedImports }
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
            // CfirTypeResolver 按顺序查找 scope；这里必须高优先级在前。
            add(CfirFileDeclaredTopLevelScope(file))
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, session))
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider, resolvedImports))
            add(CfirExplicitStarImportingScope(imports, symbolProvider, resolvedImports))
            add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
            add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
        }
    }

    /**
     * 返回 class-like 声明在类型解析中可见的自身类型参数。
     */
    private fun CfirClassLikeDeclaration.typeParametersForResolution(): List<CfirTypeParameter> = when (this) {
        is CfirClass -> typeParameters
        is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> emptyList()
        is CfirInterface -> typeParameters
        is CfirStruct -> typeParameters
        is CfirEnum -> typeParameters
        is CfirTypeAlias -> typeParameters
    }
}
