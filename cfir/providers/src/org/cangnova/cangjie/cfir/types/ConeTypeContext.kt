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

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.TypeSystemCommonBackendContext

/**
 * CFIR 层的 Cone 类型系统上下文。
 *
 * 设计原则：
 * 1. 外部统一类型表示仍然是 [ConeCangJieType]。
 * 2. 仓颉语义类型优先：class/interface、struct、enum、func、tuple、Quest、Nothing。
 * 3. Kotlin 风格的 flexible / nullable / variance / star projection 不在这里出现。
 * 4. 类型变量、stub、captured 仍可保留，但仅作为推断期内部机制。
 *
 * 这份上下文同时给 `common` 模块的抽象类型检查器和 CFIR 自身的中层推断逻辑
 * 提供一套可落到 `Cone*` 数据结构上的默认实现。
 */
interface ConeTypeContext :
    TypeSystemContext,
    TypeSystemTypeFactoryContext,
    TypeCheckerProviderContext,
    TypeSystemCommonBackendContext {

    /**
     * 生产实现通常会覆盖这个 `session`。
     *
     * 测试上下文如果只依赖纯内存 Cone 类型比较，也可以直接使用默认实现。
     */
    val session: CfirSession
        get() = error("ConeTypeContext.session is not available in this context")

    /**
     * 判断两个类型是否拥有同一个“类型头”。
     *
     * 对名义类型来说比较声明身份；
     * 对函数/元组/数组等结构类型来说比较结构头部。
     */
    fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean =
        a.typeConstructor() == b.typeConstructor()

    override fun identicalArguments(a: RigidTypeMarker, b: RigidTypeMarker): Boolean {
        require(a is ConeRigidType)
        require(b is ConeRigidType)
        return a.typeArguments == b.typeArguments
    }

    /**
     * 对齐 Kotlin FIR `ConeTypeContext.asRigidType()`：
     * 进入通用类型检查器之前，class-like 语义比较必须基于 fully expanded type，
     * 不能把 `ConeTypeAliasType` 当成独立 constructor 直接带进 subtype/equality/supertypes 计算。
     *
     * `DISABLE_TYPEALIAS_EXPANSION` 只要求保留声明/引用处的 alias 语法视图，
     * 不应改变类型系统内部做语义判定时看到的真实类型头。
     */
    override fun CangJieTypeMarker.asRigidType(): RigidTypeMarker? {
        val coneType = this as? ConeCangJieType ?: return null
        val rigidType = coneType as? ConeRigidType ?: return null
        return when (rigidType) {
            is ConeClassLikeType -> rigidType.fullyExpandedType(session) as? ConeRigidType ?: rigidType
            is ConeTypeAliasType -> rigidType.fullyExpandedType(session) as? ConeRigidType ?: rigidType
            else -> rigidType
        }
    }

    override fun CangJieTypeMarker.isError(): Boolean = (this as? ConeCangJieType)?.isError == true

    override fun CangJieTypeMarker.isUninferredParameter(): Boolean {
        return this is ConeErrorType && this.isUninferredParameter
    }



    override fun CangJieTypeMarker.argumentsCount(): Int {
        require(this is ConeCangJieType)
        return typeArguments.size
    }

    override fun CangJieTypeMarker.getArgument(index: Int): ConeTypeProjection {
        require(this is ConeCangJieType)
        return this.typeArguments[index]
    }

    override fun CangJieTypeMarker.getArguments(): List<ConeTypeProjection> {
        require(this is ConeCangJieType)
        return this.typeArguments.toList()
    }

    override fun CangJieTypeMarker.isFunctionType(): Boolean {
        return this is ConeFunctionType
    }

    override fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker> {
        require(this is ConeFunctionType)
        return parameterTypes + returnType
    }

    override fun areEqualFunctionTypeKinds(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean {
        require(subType is ConeFunctionType)
        require(superType is ConeFunctionType)
        return subType.isCFunc == superType.isCFunc &&
                subType.hasVariableLenArg == superType.hasVariableLenArg
    }

    override fun CangJieTypeMarker.isTupleType(): Boolean {
        return this is ConeTupleType
    }

    override fun CangJieTypeMarker.extractElementsForTupleType(): List<CangJieTypeMarker> {
        require(this is ConeTupleType)
        return elementTypes
    }

    override fun RigidTypeMarker.isStubType(): Boolean = this is ConeStubType

    override fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean =
        this is ConeStubType && kind == ConeStubType.Kind.FOR_SUBTYPING

    override fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean =
        this is ConeStubType && kind == ConeStubType.Kind.FOR_BUILDER_INFERENCE

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): ConeTypeConstructorMarker {
        require(this is ConeTypeConstructorMarker)
        if (this !is ConeStubTypeConstructor) return this
        if (this.isTypeVariableInSubtyping) return this
        if (this.isForFixation) return this
        return this.variable.typeConstructor
    }

    override fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is ConeCangJieType)
        return this
    }

    override fun TypeArgumentMarker.getType(): CangJieTypeMarker? {
        require(this is ConeTypeProjection)
        return type
    }

    override fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker {
        require(newType is ConeCangJieType)
        return newType
    }

    override fun CangJieTypeMarker.optionBoxedElementType(): CangJieTypeMarker? {
        val coneType = this as? ConeCangJieType ?: return null
        if (coneType.classId != StdlibClassIds.Option) return null
        return coneType.typeArguments.singleOrNull()?.type
    }

    override fun TypeConstructorMarker.parametersCount(): Int = getParameters().size

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker = getParameters()[index]

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean = false

    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean = false

    override fun TypeConstructorMarker.isAnonymous(): Boolean = false

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? =
        this as? ConeTypeParameterLookupTag

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean =
        this is ConeTypeParameterLookupTag

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() = null

    override fun TypeParameterMarker.upperBoundCount(): Int = getUpperBounds().size

    override fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker = getUpperBounds()[index]

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is ConeTypeParameterLookupTag)
        return this
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        require(this is ConeTypeParameterLookupTag)
        this.typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        return this.bounds().any { typeRef ->
            typeRef.coneType.contains { it.typeConstructor() == this.getTypeConstructor() }
                    && (selfConstructor == null || typeRef.coneType.typeConstructor() == selfConstructor)
        }
    }
    @Suppress("NOTHING_TO_INLINE")
    private inline fun ConeTypeParameterLookupTag.bounds(): List<CfirTypeRef> = symbol.resolvedBounds

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean = c1 == c2

    override fun RigidTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? =
        null

    override fun RigidTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker> =
        when (this) {
            is ConeIdealLiteralType -> possibleTypes
            is ConePrimitiveType -> when (kind) {
                PrimitiveTypeKind.IDEAL_INT -> ConeIdealIntLiteralType.POSSIBLE_INT_TYPES
                PrimitiveTypeKind.IDEAL_FLOAT -> ConeIdealFloatLiteralType.POSSIBLE_FLOAT_TYPES
                else -> emptyList()
            }
            else -> emptyList()
        }


    private fun resolveDeclaredSupertypes(classId: ClassId): List<ConeCangJieType> {
        val classSymbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
            ?: return emptyList()
        if (!classSymbol.isBound) return emptyList()
        classSymbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
        return declarationSelfType(classSymbol)
            ?.let { declarationSelfType ->
                session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(declarationSelfType)
            }
            ?.takeIf { it.isNotEmpty() }
            ?: classSymbol.cfir.superTypeRefs.mapNotNull { (it as? CfirResolvedTypeRef)?.coneType }
    }
}
