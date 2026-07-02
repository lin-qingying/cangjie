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
import java.util.ArrayDeque

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

    /**
     * 判断两个 rigid type 的类型实参是否完全相同。
     */
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

    /**
     * 判断类型是否为错误类型。
     */
    override fun CangJieTypeMarker.isError(): Boolean = (this as? ConeCangJieType)?.isError == true

    /**
     * 判断类型是否为 class-like 类型。
     */
    override fun CangJieTypeMarker.isClassLikeType(): Boolean {
        return asRigidType() is ConeClassLikeType
    }

    /**
     * 判断类型是否代表尚未推断出的类型参数。
     */
    override fun CangJieTypeMarker.isUninferredParameter(): Boolean {
        return this is ConeErrorType && this.isUninferredParameter
    }



    /**
     * 返回类型实参数量。
     */
    override fun CangJieTypeMarker.argumentsCount(): Int {
        require(this is ConeCangJieType)
        return typeArguments.size
    }

    /**
     * 返回指定索引处的类型实参。
     */
    override fun CangJieTypeMarker.getArgument(index: Int): ConeTypeProjection {
        require(this is ConeCangJieType)
        return this.typeArguments[index]
    }

    /**
     * 返回当前类型的全部类型实参。
     */
    override fun CangJieTypeMarker.getArguments(): List<ConeTypeProjection> {
        require(this is ConeCangJieType)
        return this.typeArguments.toList()
    }

    /**
     * 判断类型是否为函数类型。
     */
    override fun CangJieTypeMarker.isFunctionType(): Boolean {
        return this is ConeFunctionType
    }

    /**
     * 提取函数类型的参数类型和返回类型。
     */
    override fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker> {
        require(this is ConeFunctionType)
        return parameterTypes + returnType
    }

    /**
     * 判断两个函数类型的函数种类是否一致。
     */
    override fun areEqualFunctionTypeKinds(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean {
        require(subType is ConeFunctionType)
        require(superType is ConeFunctionType)
        return subType.isCFunc == superType.isCFunc &&
                subType.hasVariableLenArg == superType.hasVariableLenArg
    }

    /**
     * 判断类型是否为元组类型。
     */
    override fun CangJieTypeMarker.isTupleType(): Boolean {
        return this is ConeTupleType
    }

    /**
     * 提取元组元素类型。
     */
    override fun CangJieTypeMarker.extractElementsForTupleType(): List<CangJieTypeMarker> {
        require(this is ConeTupleType)
        return elementTypes
    }

    /**
     * 判断类型是否为 VArray 类型。
     */
    override fun CangJieTypeMarker.isVArrayType(): Boolean {
        return this is ConeVArrayType
    }

    /**
     * 提取 VArray 元素类型。
     */
    override fun CangJieTypeMarker.extractElementTypeForVArrayType(): CangJieTypeMarker {
        require(this is ConeVArrayType)
        return elementType
    }

    /**
     * 提取 VArray 固定长度。
     */
    override fun CangJieTypeMarker.extractSizeForVArrayType(): Long {
        require(this is ConeVArrayType)
        return size
    }

    /**
     * 判断 rigid type 是否为 stub type。
     */
    override fun RigidTypeMarker.isStubType(): Boolean = this is ConeStubType

    /**
     * 判断 stub type 是否用于子类型检查中的类型变量占位。
     */
    override fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean =
        this is ConeStubType && kind == ConeStubType.Kind.FOR_SUBTYPING

    /**
     * 判断 stub type 是否用于 builder inference。
     */
    override fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean =
        this is ConeStubType && kind == ConeStubType.Kind.FOR_BUILDER_INFERENCE

    /**
     * 从 stub type constructor 中取回真实类型变量 constructor。
     */
    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): ConeTypeConstructorMarker {
        require(this is ConeTypeConstructorMarker)
        if (this !is ConeStubTypeConstructor) return this
        if (this.isTypeVariableInSubtyping) return this
        if (this.isForFixation) return this
        return this.variable.typeConstructor
    }

    /**
     * 将类型本身作为类型实参 marker。
     */
    override fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is ConeCangJieType)
        return this
    }

    /**
     * 从类型实参 marker 中取出类型。
     */
    override fun TypeArgumentMarker.getType(): CangJieTypeMarker? {
        require(this is ConeTypeProjection)
        return type
    }

    /**
     * 返回替换为 [newType] 后的类型实参。
     */
    override fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker {
        require(newType is ConeCangJieType)
        return newType
    }

    /**
     * 提取 Option 类型的装箱元素类型。
     */
    override fun CangJieTypeMarker.optionBoxedElementType(): CangJieTypeMarker? {
        val coneType = this as? ConeCangJieType ?: return null
        if (coneType.classId != StdlibClassIds.Option) return null
        return coneType.typeArguments.singleOrNull()?.type
    }

    /**
     * 返回类型构造器参数数量。
     */
    override fun TypeConstructorMarker.parametersCount(): Int = getParameters().size

    /**
     * 返回指定索引处的类型构造器参数。
     */
    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker = getParameters()[index]

    /**
     * CFIR 的 ideal literal 自身就是类型构造器。
     *
     * Kotlin K2 通过 IntegerLiteralConstantType / IntegerConstantOperatorType 区分字面量与
     * 运算结果；仓颉对应为 [ConeIdealIntConstantType] / [ConeIdealFloatConstantType] 和
     * [ConeIdealIntOperatorType] / [ConeIdealFloatOperatorType]。这里把它们暴露给公共
     * type approximator，否则推断完成阶段不会按 expected type 收束 IdealInt/IdealFloat。
     */
    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean =
        this is ConeIdealIntConstantType ||
                this is ConeIdealFloatConstantType ||
                this is ConePrimitiveType && kind.isIdeal

    /**
     * 判断由 ideal 数字运算产生的构造器。
     */
    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean =
        this is ConeIdealIntOperatorType || this is ConeIdealFloatOperatorType

    /**
     * 仓颉当前没有匿名类型构造器。
     */
    override fun TypeConstructorMarker.isAnonymous(): Boolean = false

    /**
     * 若构造器来自类型参数，则返回类型参数 marker。
     */
    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? =
        this as? ConeTypeParameterLookupTag

    /**
     * 判断构造器是否是类型参数构造器。
     */
    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean =
        this is ConeTypeParameterLookupTag

    /**
     * 当前类型变量 constructor 不直接携带来源类型参数。
     */
    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() = null

    /**
     * 返回类型参数上界数量。
     */
    override fun TypeParameterMarker.upperBoundCount(): Int = getUpperBounds().size

    /**
     * 返回指定索引的类型参数上界。
     */
    override fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker = getUpperBounds()[index]

    /**
     * 返回类型参数自身的类型构造器。
     */
    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is ConeTypeParameterLookupTag)
        return this
    }

    /**
     * 判断类型参数是否包含递归上界。
     */
    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        require(this is ConeTypeParameterLookupTag)
        this.typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        return this.bounds().any { typeRef ->
            typeRef.coneType.contains { it.typeConstructor() == this.getTypeConstructor() }
                    && (selfConstructor == null || typeRef.coneType.typeConstructor() == selfConstructor)
        }
    }

    /**
     * 返回类型参数已解析上界。
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun ConeTypeParameterLookupTag.bounds(): List<CfirTypeRef> = symbol.resolvedBounds

    /**
     * 判断两个类型构造器是否相同。
     */
    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean = c1 == c2

    /**
     * 快速查找当前 rigid type 中与 [constructor] 对应的父类型。
     */
    override fun RigidTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? {
        val type = this as? ConeCangJieType ?: return null
        cTypeCorrespondingSupertype(type, constructor)?.let { return listOf(it) }

        val supertypeProvider = session.typeAwareSupertypeProviderOrNull ?: return null
        val correspondingSupertypes = mutableListOf<SimpleTypeMarker>()
        val visitedTypes = linkedSetOf<ConeCangJieType>()
        val queue = ArrayDeque<ConeCangJieType>()
        queue += type

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visitedTypes.add(current)) continue

            for (supertype in supertypeProvider.getDirectSupertypes(current)) {
                val rigidSupertype = supertype as? ConeRigidType ?: continue
                if (areEqualTypeConstructors(rigidSupertype.typeConstructor(), constructor)) {
                    correspondingSupertypes += rigidSupertype
                }
                queue += rigidSupertype
            }
        }

        return correspondingSupertypes.takeIf { it.isNotEmpty() }
    }

    /**
     * 如果 [type] 满足 `CType`，为其合成标准库 `CType` 接口父类型。
     */
    private fun cTypeCorrespondingSupertype(
        type: ConeCangJieType,
        constructor: TypeConstructorMarker,
    ): ConeClassLikeType? {
        val lookupTag = constructor as? ConeClassLikeLookupTag ?: return null
        if (lookupTag.classId != StdlibClassIds.CType) return null
        if (!CfirCTypeSemantics.isMetCType(session, type)) return null
        return ConeClassLikeType(lookupTag = lookupTag, isInterface = true)
    }

    /**
     * 返回 ideal/integer literal 类型可能对应的具体类型集合。
     */
    override fun RigidTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker> =
        when (this) {
            is ConeIdealLiteralType -> possibleTypes.withDefaultIdealTypeFirst(defaultType)
            is ConePrimitiveType -> when (kind) {
                PrimitiveTypeKind.IDEAL_INT -> ConeIdealIntLiteralType.POSSIBLE_INT_TYPES.withDefaultIdealTypeFirst(ConePrimitiveType.INT64)
                PrimitiveTypeKind.IDEAL_FLOAT -> ConeIdealFloatLiteralType.POSSIBLE_FLOAT_TYPES.withDefaultIdealTypeFirst(ConePrimitiveType.FLOAT64)
                else -> emptyList()
            }
            else -> emptyList()
        }

    /**
     * 约束系统会采用第一个成功的 ideal 分支派生约束。
     * 因此子类型检查枚举候选时先尝试官方默认落地类型，避免
     * `IdealInt <: Comparable<T>` 将 `T` 固定成候选列表里的 Int8。
     */
    private fun Collection<ConePrimitiveType>.withDefaultIdealTypeFirst(defaultType: ConePrimitiveType): List<ConePrimitiveType> =
        if (firstOrNull() == defaultType) toList() else listOf(defaultType) + filterNot { it == defaultType }


    /**
     * 解析声明侧直接父类型。
     */
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
