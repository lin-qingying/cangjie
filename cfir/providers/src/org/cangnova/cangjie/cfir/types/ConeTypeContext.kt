package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.TypeSystemCommonBackendContext
import  org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

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

    override fun CangJieTypeMarker.asRigidType(): RigidTypeMarker? = this as? ConeRigidType

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
        return ConeTypeProjection(this)
    }

    override fun TypeArgumentMarker.getType(): CangJieTypeMarker? {
        require(this is ConeTypeProjection)
        return type
    }

    override fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker {
        require(newType is ConeCangJieType)
        return ConeTypeProjection(newType)
    }

    override fun TypeConstructorMarker.parametersCount(): Int = getParameters().size

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker = getParameters()[index]

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean = false

    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean = false

    override fun TypeConstructorMarker.isLocalType(): Boolean = false

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
        if (this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_INT) {
            listOf(
                ConePrimitiveType.INT8,
                ConePrimitiveType.INT16,
                ConePrimitiveType.INT32,
                ConePrimitiveType.INT64,
                ConePrimitiveType.INT_NATIVE,
                ConePrimitiveType.UINT8,
                ConePrimitiveType.UINT16,
                ConePrimitiveType.UINT32,
                ConePrimitiveType.UINT64,
                ConePrimitiveType.UINT_NATIVE,
            )
        } else {
            emptyList()
        }


    private fun resolveDeclaredSupertypes(classId: ClassId): List<ConeCangJieType> {
        val classSymbol = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId) }.getOrNull()
            ?: return emptyList()
        if (!classSymbol.isBound) return emptyList()
        return classSymbol.cfir.superTypeRefs.mapNotNull { (it as? CfirResolvedTypeRef)?.coneType }
    }
}
