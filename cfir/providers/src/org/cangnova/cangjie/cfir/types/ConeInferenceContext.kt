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

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.common.canSeeInternalsOf
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeRefiner
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.*

/** 创建简单诊断信息的辅助函数 */
private fun simpleDiagnostic(reason: String): ConeDiagnostic = object : ConeDiagnostic {
    override val reason: String = reason
}

/**
 * ConeInferenceContext
 *
 * 仓颉（类似 Kotlin FIR）的类型推断上下文接口。
 * 主要职责：
 *   - 提供类型系统操作（创建类型、替换类型、判断类型）
 *   - 支持类型推断（Type Inference）
 *   - 提供类型检查（Type Checking）所需能力
 *
 * 可以理解为：K2 中 TypeSystemContext + InferenceContext 的组合
 */
interface ConeInferenceContext : TypeSystemInferenceExtensionContext, ConeTypeContext {

    /**
     * 当前 session 的符号提供器
     */
    val symbolProvider: CfirSymbolProvider
        get() = session.symbolProvider

    // =========================================================================
    // 类型构造器查询
    // =========================================================================

    /**
     * 获取刚性类型的类型构造器。
     *
     * 名义类型 → lookupTag
     * 结构类型（func/tuple 等）→ 自身（实现了 ConeTypeConstructorMarker）
     * 类型变量 → ConeTypeVariableTypeConstructor
     */
    override fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker {
        require(this is ConeRigidType)
        return when (this) {
            is ConeLookupTagBasedType -> lookupTag
            is ConeTypeVariableType -> typeConstructor
            is ConeStubType -> constructor
            is ConeTypeConstructorMarker -> this
            else -> error("无法获取类型构造器: $this (${this::class})")
        }
    }

    /**
     * 获取类型构造器的类型参数列表。
     *
     * 只有名义类型（通过 lookup tag → symbol → declaration）有类型参数。
     * 结构类型（func/tuple 等）和原始类型没有声明级别的类型参数。
     */
    override fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> {
        return when (this) {
            is ConeClassLikeLookupTag -> {
                val symbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
                    ?: return emptyList()
                if (!symbol.isBound) return emptyList()
                val typeParams = when (val decl = symbol.cfir) {
                    is CfirClass -> decl.typeParameters
                    is CfirInterface -> decl.typeParameters
                    is CfirStruct -> decl.typeParameters
                    is CfirEnum -> decl.typeParameters
                    is CfirTypeAlias -> decl.typeParameters
                    else -> return emptyList()
                }
                typeParams.mapNotNull { (it.symbol as? org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol)?.toLookupTag() }
            }
            is ConeTypeParameterLookupTag -> emptyList()
            is ConeTypeVariableTypeConstructor -> emptyList()
            is ConeStubTypeConstructor -> emptyList()
            else -> emptyList()
        }
    }

    /**
     * 获取类型构造器的所有父类型。
     */
    override fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker> {
        return when (this) {
            is ConeClassLikeLookupTag -> {
                val symbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
                    ?: return emptyList()
                if (!symbol.isBound) return emptyList()
                symbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
                declarationSelfType(symbol)
                    ?.let { declarationSelfType ->
                        session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(declarationSelfType)
                    }
                    ?.takeIf { it.isNotEmpty() }
                    ?: symbol.cfir.superTypeRefs.mapNotNull { (it as? CfirResolvedTypeRef)?.coneType }
            }
            is ConeTypeParameterLookupTag -> {
                typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
                typeParameterSymbol.resolvedBounds.map { it.coneType }
            }

            // 结构类型/原始类型默认以 Any 为父类型
            is ConeAnyType -> emptyList()
            is ConePrimitiveType -> if (kind == PrimitiveTypeKind.NOTHING) {
                emptyList()
            } else {
                session.typeAwareSupertypeProviderOrNull
                    ?.getDirectSupertypes(this)
                    .orEmpty()
            }
            is ConeIntersectionType -> intersectedTypes.toList()
            else -> listOf()
        }
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        return this is ConeClassLikeErrorLookupTag
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        return this is ConeIntersectionType
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        if (this !is ConeClassLikeLookupTag) return false
        val symbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
            ?: return false
        return symbol is org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
                || symbol is org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
    }

    override fun TypeConstructorMarker.isInterface(): Boolean {
        if (this !is ConeClassLikeLookupTag) return false
        val symbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
            ?: return false
        return symbol is org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        return this is ConeAnyType
                || (this is ConeClassLikeLookupTag && classId == StdlibClassIds.Any)
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        return this is ConePrimitiveType && kind == PrimitiveTypeKind.NOTHING
    }

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && classId == StdlibClassIds.Array
    }

    override fun TypeConstructorMarker.isFinalClassConstructor(): Boolean {
        // 结构类型/原始类型/枚举类型是 final 的
        if (this is ConePrimitiveType || this is ConeFunctionType || this is ConeTupleType ||
            this is ConeVArrayType || this is ConePointerType || this is ConeCStringType) {
            return true
        }
        if (this !is ConeClassLikeLookupTag) return false
        val symbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
            ?: return false
        // struct 和 enum 在仓颉中不可被继承
        return symbol is org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
                || symbol is org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        return isFinalClassConstructor()
    }

    override fun TypeConstructorMarker.isDenotable(): Boolean {
        return when (this) {
            is ConeClassifierLookupTag -> true
            is ConePrimitiveType -> true
            is ConeFunctionType -> true
            is ConeTupleType -> true
            is ConeVArrayType -> true
            is ConePointerType -> true
            is ConeCStringType -> true
            is ConeAnyType -> true
            is ConeQuestType -> true
            is ConeTypeAliasType -> true
            // 类型变量、stub、captured、intersection 等不可表示
            else -> false
        }
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        return this is ConeTypeVariableTypeConstructor
    }

    override fun TypeConstructorMarker.isValueTypeConstructor(): Boolean {
        // struct 是值类型
        if (this is ConeClassLikeLookupTag) {
            val symbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
                ?: return false
            return symbol is org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
        }
        // 原始类型也是值类型
        return this is ConePrimitiveType
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        return this is ConeIdealLiteralType ||
                this is ConePrimitiveType && kind.isIdeal
    }

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? {
        return when (this) {
            is ConeClassLikeLookupTag -> classId.asSingleFqName().toUnsafe()
            else -> null
        }
    }



    // =========================================================================
    // 可访问性检查
    // =========================================================================

    /**
     * 判断类型构造器对应的类型声明是否可访问。
     * 对齐 C++ ImportManager::IsTyAccessible。
     */
    override fun TypeConstructorMarker.isTypeAccessible(): Boolean {
        // 非名义类型（原始类型、函数类型、元组类型等）始终可访问
        val lookupTag = this as? ConeClassLikeLookupTag ?: return true
        val symbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(lookupTag.classId) }
            .getOrNull() ?: return true
        if (!symbol.isBound) return true
        val declaration = symbol.cfir as? org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration ?: return true

        // === 可见性语义检查 ===
        val file = CfirAccessibilityFileScope.get() ?: return true
        val filePackage = file.packageDirective.packageFqName
        val declPackage = lookupTag.classId.packageFqName
        val visibility = declaration.status.visibility
        when {
            visibility == Visibilities.Public -> Unit
            visibility == Visibilities.Protected -> {
                if (
                    declaration.moduleData != session.moduleData &&
                    !session.moduleData.canSeeInternalsOf(declaration.moduleData)
                ) {
                    return false
                }
            }
            visibility == Visibilities.Internal -> {
                if (!canAccessPackageInternalDeclaration(filePackage, declPackage)) {
                    return false
                }
            }
            else -> return false  // Private, Local 等
        }

        // === 名字可达性检查（对齐 C++ IsDeclAccessible）===
        // 同包 → 可访问
        if (filePackage == declPackage) return true

        // 检查 import 可见性
        val bindings = session.importBindingStoreOrNull?.getBindings(file) ?: return true
        for (binding in bindings.imports) {
            for (target in binding.targets) {
                when (target) {
                    is CfirResolvedImportTarget.ClassLike -> {
                        if (target.classId == lookupTag.classId) return true
                    }
                    is CfirResolvedImportTarget.Package -> {
                        if (target.fqName == declPackage) return true
                    }
                    else -> {}
                }
            }
        }
        return false
    }

    // =========================================================================
    // 简单类型谓词
    // =========================================================================

    override fun CangJieTypeMarker.isUnit(): Boolean {
        return this is ConePrimitiveType && kind == PrimitiveTypeKind.UNIT
    }

    override fun CangJieTypeMarker.isFunctionType(): Boolean {
        return this is ConeFunctionType
    }

    override fun CangJieTypeMarker.isTupleType(): Boolean {
        return this is ConeTupleType
    }

    override fun CangJieTypeMarker.extractElementsForTupleType(): List<CangJieTypeMarker> {
        require(this is ConeTupleType)
        return elementTypes
    }

    override fun CangJieTypeMarker.isVArrayType(): Boolean {
        return this is ConeVArrayType
    }

    override fun CangJieTypeMarker.extractElementTypeForVArrayType(): CangJieTypeMarker {
        require(this is ConeVArrayType)
        return elementType
    }

    override fun CangJieTypeMarker.extractSizeForVArrayType(): Long {
        require(this is ConeVArrayType)
        return size
    }

    override fun createFunctionType(parameterTypes: List<CangJieTypeMarker>, returnType: CangJieTypeMarker): CangJieTypeMarker {
        return ConeFunctionType(
            parameterTypes = parameterTypes.map { it as ConeCangJieType },
            returnType = returnType as ConeCangJieType,
        )
    }

    override fun createTupleType(elementTypes: List<CangJieTypeMarker>): CangJieTypeMarker {
        return ConeTupleType(elementTypes.map { it as ConeCangJieType })
    }

    override fun CangJieTypeMarker.isSpecial(): Boolean {
        return this is ConeErrorType || this is ConeStubType || this is ConeQuestType
    }

    override fun CangJieTypeMarker.isTypeVariableType(): Boolean {
        return this is ConeTypeVariableType
    }

    override fun CangJieTypeMarker.isSignedOrUnsignedNumberType(): Boolean {
        return this is ConePrimitiveType && kind.isNumeric && !kind.isIdeal
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        return this is ConePrimitiveType
    }

    override fun CangJieTypeMarker.isArray(): Boolean {
        val cone = this as? ConeCangJieType ?: return false
        return cone.isArray
    }

    override fun RigidTypeMarker.isSingleClassifierType(): Boolean {
        return this !is ConeIntersectionType && this !is ConeUnionType
    }

    override fun CangJieTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        val constructor = (this as? ConeCangJieType)?.let {
            (it as? ConeRigidType)?.typeConstructor()
        } ?: return false
        return constructor.isInterface()
    }

    // =========================================================================
    // 类型判断（contains）
    // =========================================================================

    override fun CangJieTypeMarker.contains(predicate: (CangJieTypeMarker) -> Boolean): Boolean {
        require(this is ConeCangJieType)
        @Suppress("UNCHECKED_CAST")
        return this.contains(predicate as (ConeCangJieType) -> Boolean)
    }

    // =========================================================================
    // 类型创建
    // =========================================================================

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        require(typeVariable is ConeTypeVariable)
        return ConeStubType(typeVariable.typeConstructor, ConeStubType.Kind.FOR_BUILDER_INFERENCE)
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        require(typeVariable is ConeTypeVariable)
        return ConeStubType(typeVariable.typeConstructor, ConeStubType.Kind.FOR_SUBTYPING)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        attributes: List<AnnotationMarker>?
    ): SimpleTypeMarker {
        val coneArguments = arguments.map { it as ConeTypeProjection }
        val coneAttributes = ConeAttributes.Empty
        return when (constructor) {
            is ConeClassLikeLookupTag -> ConeClassLikeType(constructor, coneArguments, coneAttributes)
            is ConeTypeParameterLookupTag -> org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl(constructor)
            is ConePrimitiveType -> constructor
            is ConeFunctionType -> {
                val coneTypes = arguments.map { (it as ConeTypeProjection).type }
                val parameterTypes = coneTypes.dropLast(1)
                val returnType = coneTypes.lastOrNull() ?: ConePrimitiveType.NOTHING
                ConeFunctionType(
                    parameterTypes = parameterTypes,
                    returnType = returnType,
                    isCFunc = constructor.isCFunc,
                    isClosureType = constructor.isClosureType,
                    hasVariableLenArg = constructor.hasVariableLenArg,
                    attributes = constructor.attributes,
                )
            }
            is ConeIntersectionType -> if (coneAttributes === constructor.attributes) {
                constructor
            } else {
                ConeIntersectionType(
                    intersectedTypes = constructor.intersectedTypes,
                    upperBoundForApproximation = constructor.upperBoundForApproximation,
                    attributes = coneAttributes,
                )
            }
            is ConeTupleType -> constructor
            is ConeVArrayType -> {
                val elementType = coneArguments.firstOrNull()?.type ?: return constructor
                ConeVArrayType(elementType, constructor.size, constructor.attributes)
            }
            is ConeAnyType -> ConeAnyType
            is ConeTypeVariableTypeConstructor -> ConeTypeVariableType(constructor)
            else -> ConeClassLikeType(
                ConeClassLikeErrorLookupTag(
                    org.cangnova.cangjie.name.ClassId.fromString("<unknown>"),
                    simpleDiagnostic("无法为构造器创建简单类型: $constructor"),
                ),
                coneArguments,
                coneAttributes,
            )
        }
    }

    override fun createTypeArgument(type: CangJieTypeMarker): TypeArgumentMarker {
        require(type is ConeCangJieType)
        return type
    }

    override fun createErrorType(debugName: String, delegatedType: RigidTypeMarker?): SimpleTypeMarker {
        return ConeErrorType(
            simpleDiagnostic(debugName),
            delegatedType = delegatedType as? ConeCangJieType,
        )
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker {
        return ConeErrorType(
            simpleDiagnostic("未推断的类型参数: $constructor"),
            isUninferredParameter = true,
        )
    }

    override fun createTypeWithUpperBoundForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker
    ): CangJieTypeMarker {
        val intersectionType = firstCandidate as? ConeIntersectionType ?: error {
            "Expected intersection type for approximation result, found $firstCandidate"
        }
        return intersectionType.withUpperBound(secondCandidate as ConeCangJieType)
    }

    override fun RigidTypeMarker.getUpperBoundForApproximationOfIntersectionType(): CangJieTypeMarker? {
        return (this as? ConeIntersectionType)?.upperBoundForApproximation
    }

    override fun intersectTypes(types: Collection<CangJieTypeMarker>): CangJieTypeMarker {
        val coneTypes = types.map { it as ConeCangJieType }
        if (coneTypes.size == 1) return coneTypes.single()
        return ConeIntersectionType(coneTypes)
    }

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker {
        if (types.size == 1) return types.single()
        return ConeIntersectionType(types.map { it as ConeCangJieType })
    }

    override fun nothingType(): SimpleTypeMarker = ConePrimitiveType.NOTHING

    override fun anyType(): SimpleTypeMarker = ConeAnyType

    override fun arrayType(componentType: CangJieTypeMarker): SimpleTypeMarker {
        require(componentType is ConeCangJieType)
        return ConeClassLikeType(
            StdlibClassIds.Array.toLookupTag(),
            listOf(componentType),
        )
    }

    // =========================================================================
    // 类型操作
    // =========================================================================

    override fun RigidTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): RigidTypeMarker {
        require(this is ConeRigidType)
        if (newArguments.isEmpty() && typeArguments.isEmpty()) return this
        val coneArgs = newArguments.map { it as ConeTypeProjection }
        return when (this) {
            is ConeClassLikeType -> ConeClassLikeType(lookupTag, coneArgs, attributes, isInterface, isThisType)
            is ConeStructType -> ConeStructType(lookupTag, coneArgs, attributes)
            is ConeEnumType -> ConeEnumType(lookupTag, coneArgs, attributes, isRefEnum)
            is ConeFunctionType -> {
                if (coneArgs.isEmpty()) return this
                val paramTypes = coneArgs.dropLast(1).map { it.type }
                val retType = coneArgs.last().type
                ConeFunctionType(paramTypes, retType, isCFunc, isClosureType, hasVariableLenArg, attributes)
            }
            is ConeTupleType -> ConeTupleType(coneArgs.map { it.type }, attributes)
            is ConeVArrayType -> {
                if (coneArgs.isEmpty()) return this
                ConeVArrayType(coneArgs.first().type, size, attributes)
            }
            is ConePointerType -> {
                if (coneArgs.isEmpty()) return this
                ConePointerType(coneArgs.first().type, attributes)
            }
            is ConeIntersectionType -> ConeIntersectionType(
                intersectedTypes = coneArgs.map { it.type },
                upperBoundForApproximation = upperBoundForApproximation,
                attributes = attributes,
            )
            is ConeTypeAliasType -> ConeTypeAliasType(classId, expandedType, coneArgs, attributes)
            is ConeErrorType -> ConeErrorType(diagnostic, isUninferredParameter, delegatedType, coneArgs, attributes)
            else -> this
        }
    }

    override fun RigidTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker {
        require(this is ConeRigidType)
        if (typeArguments.isEmpty()) return this
        val newArgs = typeArguments.map { replacement(it) }
        return replaceArguments(newArgs)
    }

    override fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker {
        require(this is ConeCangJieType)
        // 如果类型自身是类型参数，用其上界替代
        if (this is org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType) {
            val bounds = lookupTag.typeParameterSymbol.resolvedBounds
            return if (bounds.isNotEmpty()) bounds.first().coneType else ConeAnyType
        }
        // 递归替换类型实参中的类型参数
        if (typeArguments.isEmpty()) return this
        var changed = false
        val newArgs = typeArguments.map { proj ->
            val erased = (proj.type as CangJieTypeMarker).eraseContainingTypeParameters()
            if (erased !== proj.type) {
                changed = true
                erased as ConeCangJieType
            } else {
                proj
            }
        }
        if (!changed) return this
        return (this as ConeRigidType).replaceArguments(newArgs)
    }

    override fun Collection<CangJieTypeMarker>.singleBestRepresentative(): CangJieTypeMarker? {
        if (isEmpty()) return null
        // 取第一个非 error 类型
        return firstOrNull { !(it as ConeCangJieType).isError } ?: first()
    }

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(expectedType: CangJieTypeMarker?): CangJieTypeMarker {
        if (this is ConeIdealLiteralType) {
            return getApproximatedType(expectedType as? ConeCangJieType)
        }
        // 不是理想字面量类型，返回 Int64 作为默认
        return ConePrimitiveType.INT64
    }



    // =========================================================================
    // 类型变量 / Stub
    // =========================================================================

    override fun TypeVariableMarker.freshTypeConstructor(): TypeVariableTypeConstructorMarker {
        require(this is ConeTypeVariable)
        return this.typeConstructor
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        require(this is ConeTypeVariable)
        return this.defaultType
    }



    override fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker {
        require(this is ConeStubType)
        return constructor
    }

    // =========================================================================
    // 类型参数操作
    // =========================================================================

    override fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker> {
        require(this is ConeTypeParameterLookupTag)
        typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        return typeParameterSymbol.resolvedBounds.map { it.coneType }
    }

    override fun TypeParameterMarker.getRepresentativeUpperBound(): CangJieTypeMarker {
        val bounds = getUpperBounds()
        // 选第一个非 Any 的上界，如果没有则返回 Any
        return bounds.firstOrNull {
            val cone = it as ConeCangJieType
            cone !== ConeAnyType && !(cone is ConeClassLikeType && cone.classId == StdlibClassIds.Any)
        } ?: bounds.firstOrNull() ?: ConeAnyType
    }

    override fun TypeParameterMarker.getName(): Name {
        require(this is ConeTypeParameterLookupTag)
        return name
    }

    override fun TypeParameterMarker.isReified(): Boolean {
        // 仓颉目前不支持 reified 类型参数
        return false
    }

    // =========================================================================
    // 替换器操作
    // =========================================================================

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        @Suppress("UNCHECKED_CAST")
        return createInferenceTypeSubstitutor(
            map = map as Map<TypeConstructorMarker, ConeCangJieType>,
        )
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return ConeEmptySubstitutor
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker {
        if (this is ConeEmptySubstitutor) return type
        require(this is ConeSubstitutor)
        require(type is ConeCangJieType)
        return this.substituteOrSelf(type)
    }

    override fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker {
        // 默认返回空替换器，具体实现在约束系统求解时由调用方提供
        return ConeEmptySubstitutor
    }

    override fun createSubstitutorForSuperTypes(baseType: CangJieTypeMarker): TypeSubstitutorMarker? {
        require(baseType is ConeCangJieType)
        val rigid = baseType as? ConeRigidType ?: return null
        val constructor = rigid.typeConstructor()
        val parameters = constructor.getParameters()
        if (parameters.isEmpty()) return null
        val arguments = rigid.typeArguments
        if (arguments.size != parameters.size) return null

        val map = mutableMapOf<TypeConstructorMarker, CangJieTypeMarker>()
        for (i in parameters.indices) {
            val param = parameters[i]
            val arg = arguments[i]
            map[param.getTypeConstructor()] = arg.type
        }
        return typeSubstitutorByTypeConstructor(map)
    }

    // =========================================================================
    // 函数类型操作
    // =========================================================================

    override fun CangJieTypeMarker.contextParameterCount(): Int {
        // 仓颉无 context receiver
        return 0
    }

    override fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker> {
        require(this is ConeFunctionType)
        // 返回参数类型 + 返回值类型
        return parameterTypes + returnType
    }

    override fun getFunctionTypeConstructor(parametersNumber: Int): TypeConstructorMarker {
        // 仓颉函数类型不像 Kotlin 有 Function0/Function1 等独立构造器
        // 返回一个占位的 ConeFunctionType 作为构造器标记
        val params = (0 until parametersNumber).map { ConePrimitiveType.NOTHING as ConeCangJieType }
        return ConeFunctionType(params, ConePrimitiveType.NOTHING)
    }

    // =========================================================================
    // 注解/属性操作
    // =========================================================================

    override fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker> {
        require(this is ConeCangJieType)
        return attributes.toList()
    }

    override fun CangJieTypeMarker.hasAnnotation(fqName: FqName): Boolean {
        // 仓颉类型级注解尚未实现
        return false
    }

    override fun CangJieTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? {
        // 仓颉类型级注解尚未实现
        return null
    }

    override fun unionTypeAttributes(types: List<CangJieTypeMarker>): List<AnnotationMarker> {
        var result = ConeAttributes.Empty
        for (type in types) {
            val coneType = type as ConeCangJieType
            result = result.union(coneType.attributes)
        }
        return result.toList()
    }

    override fun CangJieTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): CangJieTypeMarker {
        // 目前仓颉类型的属性替换暂不需要
        return this
    }

    // =========================================================================
    // 状态与工具
    // =========================================================================

    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState {
        return TypeCheckerState(
            isErrorTypeEqualsToAnything = errorTypesEqualToAnything,
            isStubTypeEqualsToAnything = stubTypesEqualToAnything,
            allowedTypeVariable = false,
            typeSystemContext = this,
            cangjieTypePreparator = ConeTypePreparator(session),
            cangjieTypeRefiner = AbstractTypeRefiner.Default,
        )
    }

    override fun substitutionSupertypePolicy(type: RigidTypeMarker): TypeCheckerState.SupertypesPolicy {
        require(type is ConeCangJieType)
        val substitutor = createSubstitutorForSuperTypes(type) ?: return TypeCheckerState.SupertypesPolicy.Direct

        return object : TypeCheckerState.SupertypesPolicy.DoCustomTransform() {
            override fun transformType(
                state: TypeCheckerState,
                type: CangJieTypeMarker,
            ): RigidTypeMarker {
                val concreteSupertype = with(this@ConeInferenceContext) {
                    substitutor.safeSubstitute(type) as ConeCangJieType
                }
                return concreteSupertype as? RigidTypeMarker
                    ?: error("Concrete supertype should remain rigid: $concreteSupertype")
            }
        }
    }

    override fun RigidTypeMarker.typeDepth(): Int {
        require(this is ConeRigidType)
        if (typeArguments.isEmpty()) return 0
        return 1 + (typeArguments.maxOfOrNull { proj ->
            (proj.type as? ConeRigidType)?.typeDepth() ?: 0
        } ?: 0)
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<RigidTypeMarker>): RigidTypeMarker? {
        // 查找理想整数类型的公共具体类型
        val idealTypes = explicitSupertypes.filterIsInstance<ConeIdealLiteralType>()
        if (idealTypes.isEmpty()) return null
        // 如果都是 IdealInt，返回 Int64
        if (idealTypes.all { it is ConeIdealIntLiteralType }) return ConePrimitiveType.INT64
        // 如果都是 IdealFloat，返回 Float64
        if (idealTypes.all { it is ConeIdealFloatLiteralType }) return ConePrimitiveType.FLOAT64
        return null
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        return ConeErrorType(simpleDiagnostic("错误类型构造器: $this"))
    }

    override fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is ConeRigidType)
        val args = ArgumentList(typeArguments.size)
        args.addAll(typeArguments)
        return args
    }

    override fun CangJieTypeMarker.getUnsubstitutedUnderlyingType(): CangJieTypeMarker? {
        // 仓颉无 inline class / value class 的 underlying type 概念
        return null
    }

    override fun TypeConstructorMarker.isFinalClassOrAnnotationClassConstructor(): Boolean {
        return isFinalClassConstructor()
    }
}

private fun ConeInferenceContext.createInferenceTypeSubstitutor(
    map: Map<TypeConstructorMarker, ConeCangJieType>,
): ConeSubstitutor {
    if (map.isEmpty()) return ConeEmptySubstitutor

    return object : AbstractConeSubstitutor(this) {
        override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
            val constructor = when (type) {
                is ConeLookupTagBasedType -> type.lookupTag
                is ConeTypeVariableType -> type.typeConstructor
                else -> null
            } ?: return null
            return map[constructor]
        }
    }
}
