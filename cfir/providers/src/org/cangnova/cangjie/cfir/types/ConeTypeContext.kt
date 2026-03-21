package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypePreparator
import org.cangnova.cangjie.type.AbstractTypeRefiner
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.TypeSystemCommonBackendContext

/**
 * 类型系统操作抽象接口，将 Cone 类型体系桥接到抽象类型系统接口。
 *
 * 所有 [TypeSystemContext] 中的 marker 接口操作在此提供默认实现，
 * 通过向下转型将抽象标记操作映射到具体的 Cone 类型。
 *
 * 参考 K2 ConeTypeContext（646行），但融入仓颉语义差异：
 * - 无可空类型，使用 Option<T> 替代
 * - 泛型不变（invariant only），无 in/out
 * - 无星号投影，泛型实参即类型本身
 * - 无弹性类型，所有类型均为刚性类型
 */
interface ConeTypeContext : TypeSystemContext, TypeSystemOptimizationContext, TypeCheckerProviderContext,
    TypeSystemCommonBackendContext {
    val session: CfirSession

    /** 获取指定类型的直接超类型列表 */
    fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType>

    /** 判断两个类型是否拥有相同的类型构造器（忽略类型参数） */
    fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean

    // =====================================================================
    // TypeSystemOptimizationContext
    // =====================================================================

    override fun identicalArguments(a: RigidTypeMarker, b: RigidTypeMarker): Boolean {
        val aCone = a as ConeCangJieType
        val bCone = b as ConeCangJieType
        return aCone.typeArguments === bCone.typeArguments
    }

    // =====================================================================
    // 类型转换（安全 downcast）
    // =====================================================================

    override fun CangJieTypeMarker.asRigidType(): RigidTypeMarker? {
        require(this is ConeCangJieType)
        return this as? ConeRigidType
    }

    // =====================================================================
    // 错误与未推断类型判断
    // =====================================================================

    override fun CangJieTypeMarker.isError(): Boolean {
        require(this is ConeCangJieType)
        return this is ConeErrorType
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        // ConeLookupTag 本身不是 error，但关联的类型可能是
        return false
    }

    override fun CangJieTypeMarker.isUninferredParameter(): Boolean {
        // 仓颉无 uninferred parameter 概念，通过 ConeErrorType 标记
        require(this is ConeCangJieType)
        return this is ConeErrorType && this.reason.startsWith("Uninferred")
    }

    // =====================================================================
    // 捕获类型（仓颉无通配符，捕获类型仅内部推断使用）
    // =====================================================================

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        return this as? ConeCapturedType
    }

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = false

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker {
        require(this is ConeCapturedType)
        return this.constructor
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is ConeCapturedType)
        return this.status
    }

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker {
        require(this is ConeCapturedTypeConstructor)
        return this.projection
    }

    override fun CapturedTypeMarker.lowerType(): CangJieTypeMarker? {
        require(this is ConeCapturedType)
        return this.lowerType
    }

    // =====================================================================
    // 泛型实参访问
    // =====================================================================

    override fun CangJieTypeMarker.argumentsCount(): Int {
        require(this is ConeCangJieType)
        return typeArguments.size
    }

    override fun CangJieTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        require(this is ConeCangJieType)
        // ConeCangJieType 同时实现了 TypeArgumentMarker，直接返回
        return typeArguments[index]
    }

    override fun CangJieTypeMarker.getArguments(): List<TypeArgumentMarker> {
        require(this is ConeCangJieType)
        return typeArguments
    }

    // =====================================================================
    // 泛型实参属性
    // 仓颉无型变注解，实参就是类型本身
    // =====================================================================

    override fun TypeArgumentMarker.getType(): CangJieTypeMarker? {
        // ConeCangJieType 同时是 TypeArgumentMarker，直接返回自身
        require(this is ConeCangJieType)
        return this
    }

    override fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker {
        // 仓颉泛型实参就是类型，替换即返回新类型
        require(newType is ConeCangJieType)
        return newType
    }

    override fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is ConeCangJieType)
        return this
    }

    // =====================================================================
    // 存根类型（推断中间状态）
    // =====================================================================

    override fun RigidTypeMarker.isStubType(): Boolean = this is ConeStubType

    override fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean =
        this is ConeStubType && this.kind == ConeStubType.Kind.FOR_SUBTYPING

    override fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean =
        this is ConeStubType && this.kind == ConeStubType.Kind.FOR_BUILDER_INFERENCE

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker = this

    // =====================================================================
    // 类型构造器
    // =====================================================================

    override fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker {
        require(this is ConeRigidType)
        return when (this) {
            is ConeClassLikeType -> lookupTag
            is ConeStructType -> lookupTag
            is ConeEnumType -> lookupTag
            is ConeTypeParameterType -> lookupTag
            is ConeTypeAliasType -> ConeClassLookupTagImpl(this.classId)
            // 原始类型、函数类型等没有 lookupTag，使用类型自身的合成构造器
            is ConePrimitiveType -> ConePrimitiveTypeConstructor(kind)
            is ConeFuncType -> ConeFuncTypeConstructor(parameterTypes.size, isCFunc)
            is ConeTupleType -> ConeTupleTypeConstructor(elementTypes.size)
            is ConeArrayType -> ConeArrayTypeConstructor
            is ConeVArrayType -> ConeVArrayTypeConstructor
            is ConePointerType -> ConePointerTypeConstructor
            is ConeCStringType -> ConeCStringTypeConstructor
            is ConeAnyType -> ConeAnyTypeConstructor
            is ConeIntersectionType -> ConeIntersectionTypeConstructor(intersectedTypes)
            is ConeUnionType -> ConeUnionTypeConstructor(unionTypes)
            is ConeErrorType -> ConeErrorTypeConstructor
            is ConeQuestType -> ConeQuestTypeConstructor
            is ConeTypeVariableType -> typeVariableConstructor
            is ConeStubType -> constructor
            is ConeCapturedType -> constructor
        }
    }

    override fun TypeConstructorMarker.parametersCount(): Int {
        return when (this) {
            is ConeClassLikeLookupTag -> 0 // 实际参数数量需要通过 session 查询，这里返回 0 作为默认
            is ConeTypeParameterLookupTag -> 0
            is ConePrimitiveTypeConstructor -> 0
            is ConeFuncTypeConstructor -> 0
            is ConeTupleTypeConstructor -> 0
            is ConeIntersectionTypeConstructor -> 0
            else -> 0
        }
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        error("仓颉类型参数需通过具体类型查询，不通过构造器索引")
    }

    override fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> = emptyList()

    override fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker> {
        return when (this) {
            is ConeClassLikeLookupTag -> {
                // 通过 classId 查询超类型
                val classType = ConeClassLikeType(this)
                supertypes(classType)
            }
            is ConeTypeParameterLookupTag -> {
                // 类型参数的超类型就是其上界
                emptyList() // 上界信息在 ConeTypeParameterType 中，这里无法直接访问
            }
            is ConePrimitiveTypeConstructor -> {
                // 原始类型的超类型：所有原始类型都是 Any 的子类型
                listOf(ConeAnyType)
            }
            else -> listOf(ConeAnyType)
        }
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        return this is ConeIntersectionTypeConstructor
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        return this is ConeClassLikeLookupTag
    }

    override fun TypeConstructorMarker.isInterface(): Boolean {
        // 需要通过 session 查询，这里做基本判断
        return false
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        return this is ConePrimitiveTypeConstructor &&
            (kind == PrimitiveTypeKind.IDEAL_INT || kind == PrimitiveTypeKind.IDEAL_FLOAT)
    }

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean {
        return this is ConePrimitiveTypeConstructor && kind == PrimitiveTypeKind.IDEAL_INT
    }

    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean = false

    override fun TypeConstructorMarker.isLocalType(): Boolean = false

    override fun TypeConstructorMarker.isAnonymous(): Boolean = false

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? {
        return this as? ConeTypeParameterLookupTag
    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean {
        return this is ConeTypeParameterLookupTag
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() = null

    // =====================================================================
    // 类型参数属性访问
    // =====================================================================

    override fun TypeParameterMarker.upperBoundCount(): Int {
        // ConeTypeParameterLookupTag 本身不存储上界，需要通过关联的 ConeTypeParameterType 获取
        return 1 // 默认有一个隐式上界 Any
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker {
        // 默认上界为 Any
        return ConeAnyType
    }

    override fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker> {
        return listOf(ConeAnyType)
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is ConeTypeParameterLookupTag)
        return this
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        return false
    }

    // =====================================================================
    // 类型构造器相等性
    // =====================================================================

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        if (c1 === c2) return true
        return c1 == c2
    }

    override fun TypeConstructorMarker.isDenotable(): Boolean {
        return when (this) {
            is ConeClassLikeLookupTag -> true
            is ConeTypeParameterLookupTag -> true
            is ConePrimitiveTypeConstructor -> true
            is ConeIntersectionTypeConstructor -> false
            is ConeUnionTypeConstructor -> false
            else -> true
        }
    }

    // =====================================================================
    // 内置类型构造器判断
    // =====================================================================

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        return this is ConeAnyTypeConstructor ||
            (this is ConeClassLikeLookupTag && classId == StdlibClassIds.Any)
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        return this is ConePrimitiveTypeConstructor && kind == PrimitiveTypeKind.NOTHING
    }

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && classId == StdlibClassIds.Array
    }

    // =====================================================================
    // 值类型/引用类型
    // =====================================================================

    override fun RigidTypeMarker.isSingleClassifierType(): Boolean {
        require(this is ConeRigidType)
        return this !is ConeIntersectionType && this !is ConeUnionType
    }

    override fun TypeConstructorMarker.isValueTypeConstructor(): Boolean {
        // struct 和 enum（非引用枚举）是值类型
        return false // 默认实现，具体实现需要 session 查询
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        // 仓颉中 struct 和非 open 的 class 是 final
        return false // 默认实现
    }

    // =====================================================================
    // 整型字面量类型
    // =====================================================================

    override fun RigidTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker> {
        require(this is ConeRigidType)
        return when {
            this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_INT -> listOf(
                ConePrimitiveType.INT8, ConePrimitiveType.INT16, ConePrimitiveType.INT32, ConePrimitiveType.INT64,
                ConePrimitiveType.UINT8, ConePrimitiveType.UINT16, ConePrimitiveType.UINT32, ConePrimitiveType.UINT64,
            )
            this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_FLOAT -> listOf(
                ConePrimitiveType.FLOAT16, ConePrimitiveType.FLOAT32, ConePrimitiveType.FLOAT64,
            )
            else -> emptyList()
        }
    }

    // =====================================================================
    // 原始类型
    // =====================================================================

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        return this is ConePrimitiveType
    }

    // =====================================================================
    // 类型操作
    // =====================================================================

    override fun intersectTypes(types: Collection<CangJieTypeMarker>): CangJieTypeMarker {
        @Suppress("UNCHECKED_CAST")
        val coneTypes = types as Collection<ConeCangJieType>
        if (coneTypes.size == 1) return coneTypes.single()
        return ConeIntersectionType(coneTypes.toList())
    }

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker {
        val coneTypes = types as Collection<ConeRigidType>
        if (coneTypes.size == 1) return coneTypes.single()
        return ConeIntersectionType(coneTypes.toList())
    }

    override fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker> {
        // 仓颉类型属性系统暂不映射到 AnnotationMarker
        return emptyList()
    }

    override fun CangJieTypeMarker.isTypeVariableType(): Boolean {
        return this is ConeTypeVariableType
    }

    // =====================================================================
    // 替换策略
    // =====================================================================

    override fun substitutionSupertypePolicy(type: RigidTypeMarker): TypeCheckerState.SupertypesPolicy {
        require(type is ConeRigidType)
        // 仓颉无弹性类型，所有类型均为刚性类型，直接使用
        return TypeCheckerState.SupertypesPolicy.RigidOnly
    }

    // =====================================================================
    // 泛型实参列表
    // =====================================================================

    override fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is ConeRigidType)
        return this
    }

    // =====================================================================
    // 捕获（仓颉无通配符，简化处理）
    // =====================================================================

    override fun captureFromArguments(type: RigidTypeMarker, status: CaptureStatus): RigidTypeMarker? {
        // 仓颉无通配符/星号投影，无需捕获
        return null
    }

    override fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker? {
        return null
    }

    // =====================================================================
    // 类型替换器
    // =====================================================================

    override fun typeSubstitutorByTypeConstructor(
        map: Map<TypeConstructorMarker, CangJieTypeMarker>
    ): TypeSubstitutorMarker {
        return ConeTypeSubstitutor(map)
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return ConeTypeSubstitutor(emptyMap())
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker {
        require(this is ConeTypeSubstitutor)
        require(type is ConeCangJieType)
        return substitute(type) ?: type
    }

    // =====================================================================
    // TypeCheckerProviderContext
    // =====================================================================

    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean,
    ): TypeCheckerState {
        return TypeCheckerState(
            isErrorTypeEqualsToAnything = errorTypesEqualToAnything,
            isStubTypeEqualsToAnything = stubTypesEqualToAnything,
            allowedTypeVariable = false,
            typeSystemContext = this,
            cangjieTypePreparator = AbstractTypePreparator.Default,
            cangjieTypeRefiner = AbstractTypeRefiner.Default,
        )
    }

    // =====================================================================
    // TypeSystemCommonBackendContext
    // =====================================================================

    override fun arrayType(componentType: CangJieTypeMarker): SimpleTypeMarker {
        require(componentType is ConeCangJieType)
        return ConeArrayType(componentType)
    }

    override fun CangJieTypeMarker.isArray(): Boolean {
        require(this is ConeCangJieType)
        return this is ConeArrayType || this is ConeVArrayType || this.isArray
    }

    override fun TypeConstructorMarker.isFinalClassOrAnnotationClassConstructor(): Boolean {
        return false // 默认实现，完整实现需要 session 查询
    }

    override fun CangJieTypeMarker.hasAnnotation(fqName: FqName): Boolean {
        return false // 仓颉注解系统尚未完整实现
    }

    override fun CangJieTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? {
        return null
    }

    override fun TypeParameterMarker.getRepresentativeUpperBound(): CangJieTypeMarker {
        return getUpperBound(0)
    }

    override fun CangJieTypeMarker.getUnsubstitutedUnderlyingType(): CangJieTypeMarker? {
        return null
    }

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? {
        return when (this) {
            is ConeClassLikeLookupTag -> classId.asSingleFqName().toUnsafe()
            else -> null
        }
    }

    override fun TypeParameterMarker.getName(): Name {
        require(this is ConeTypeParameterLookupTag)
        return Name.identifier(name)
    }

    override fun TypeParameterMarker.isReified(): Boolean = false

    override fun CangJieTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        require(this is ConeCangJieType)
        return this is ConeClassLikeType && this.isInterface
    }
}

// =====================================================================
// 合成类型构造器（为没有 LookupTag 的类型提供 TypeConstructorMarker 实现）
// =====================================================================

/** 原始类型的合成构造器 */
data class ConePrimitiveTypeConstructor(val kind: PrimitiveTypeKind) : ConeLookupTag() {
    override val name: String get() = kind.typeName
}

/** 函数类型的合成构造器 */
data class ConeFuncTypeConstructor(val arity: Int, val isCFunc: Boolean) : ConeLookupTag() {
    override val name: String get() = if (isCFunc) "CFunc/$arity" else "Func/$arity"
}

/** 元组类型的合成构造器 */
data class ConeTupleTypeConstructor(val arity: Int) : ConeLookupTag() {
    override val name: String get() = "Tuple/$arity"
}

/** 交叉类型的合成构造器 */
class ConeIntersectionTypeConstructor(
    val types: List<ConeCangJieType>
) : ConeLookupTag(), IntersectionTypeConstructorMarker {
    override val name: String get() = types.joinToString(" & ") { it.toString() }
    override fun equals(other: Any?): Boolean =
        other is ConeIntersectionTypeConstructor && types == other.types
    override fun hashCode(): Int = types.hashCode()
}

/** 联合类型的合成构造器 */
class ConeUnionTypeConstructor(
    val types: Set<ConeCangJieType>
) : ConeLookupTag() {
    override val name: String get() = types.joinToString(" | ") { it.toString() }
    override fun equals(other: Any?): Boolean =
        other is ConeUnionTypeConstructor && types == other.types
    override fun hashCode(): Int = types.hashCode()
}

/** 数组类型合成构造器（单例） */
object ConeArrayTypeConstructor : ConeLookupTag() {
    override val name: String get() = "Array"
}

/** VArray 类型合成构造器（单例） */
object ConeVArrayTypeConstructor : ConeLookupTag() {
    override val name: String get() = "VArray"
}

/** CPointer 类型合成构造器（单例） */
object ConePointerTypeConstructor : ConeLookupTag() {
    override val name: String get() = "CPointer"
}

/** CString 类型合成构造器（单例） */
object ConeCStringTypeConstructor : ConeLookupTag() {
    override val name: String get() = "CString"
}

/** Any 类型合成构造器（单例） */
object ConeAnyTypeConstructor : ConeLookupTag() {
    override val name: String get() = "Any"
}

/** Error 类型合成构造器（单例） */
object ConeErrorTypeConstructor : ConeLookupTag() {
    override val name: String get() = "ERROR"
}

/** Quest 类型合成构造器（单例） */
object ConeQuestTypeConstructor : ConeLookupTag() {
    override val name: String get() = "?"
}

// =====================================================================
// 简单类型替换器实现
// =====================================================================

/**
 * 基于类型构造器映射的类型替换器。
 * 将类型中出现的特定构造器替换为目标类型。
 */
class ConeTypeSubstitutor(
    private val map: Map<TypeConstructorMarker, CangJieTypeMarker>
) : TypeSubstitutorMarker {

    fun substitute(type: ConeCangJieType): ConeCangJieType? {
        return when (type) {
            is ConeTypeParameterType -> {
                map[type.lookupTag] as? ConeCangJieType
            }
            is ConeTypeVariableType -> {
                map[type.typeVariableConstructor] as? ConeCangJieType
            }
            is ConeClassLikeType -> substituteArguments(type)?.let { args ->
                ConeClassLikeType(type.lookupTag, args, type.attributes, type.isInterface, type.isThisType)
            }
            is ConeStructType -> substituteArguments(type)?.let { args ->
                ConeStructType(type.lookupTag, args, type.attributes)
            }
            is ConeEnumType -> substituteArguments(type)?.let { args ->
                ConeEnumType(type.lookupTag, args, type.attributes, type.isRefEnum)
            }
            is ConeFuncType -> {
                val newParams = type.parameterTypes.map { substitute(it) ?: it }
                val newReturn = substitute(type.returnType) ?: type.returnType
                if (newParams == type.parameterTypes && newReturn == type.returnType) null
                else ConeFuncType(newParams, newReturn, type.isCFunc, type.isClosureType, type.hasVariableLenArg, type.attributes)
            }
            is ConeTupleType -> {
                val newElements = type.elementTypes.map { substitute(it) ?: it }
                if (newElements == type.elementTypes) null
                else ConeTupleType(newElements, type.attributes)
            }
            is ConeArrayType -> {
                val newElement = substitute(type.elementType) ?: return null
                ConeArrayType(newElement, type.dims, type.attributes)
            }
            is ConeVArrayType -> {
                val newElement = substitute(type.elementType) ?: return null
                ConeVArrayType(newElement, type.size, type.attributes)
            }
            is ConePointerType -> {
                val newPointee = substitute(type.pointeeType) ?: return null
                ConePointerType(newPointee, type.attributes)
            }
            is ConeIntersectionType -> {
                val newTypes = type.intersectedTypes.map { substitute(it) ?: it }
                if (newTypes == type.intersectedTypes) null
                else ConeIntersectionType(newTypes, type.attributes)
            }
            is ConeUnionType -> {
                val newTypes = type.unionTypes.map { substitute(it) ?: it }.toSet()
                if (newTypes == type.unionTypes) null
                else ConeUnionType(newTypes, type.attributes)
            }
            else -> null
        }
    }

    private fun substituteArguments(type: ConeCangJieType): List<ConeCangJieType>? {
        if (type.typeArguments.isEmpty()) return null
        val newArgs = type.typeArguments.map { substitute(it) ?: it }
        return if (newArgs == type.typeArguments) null else newArgs
    }
}
