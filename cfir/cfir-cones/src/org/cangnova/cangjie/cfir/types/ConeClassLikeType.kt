package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

/**
 * 统一承载"lookup tag + 实参 + 属性"的名义类型基类。
 *
 * 这一层只解决共享结构，不表达具体语言语义。
 * 具体语义由其子类分别表示 class/interface、struct、enum。
 */
abstract class ConeLookupTagBasedType : ConeSimpleCangJieType() {
    /** 用于在符号表中查找对应声明的 tag，是类型的"身份标识"。 */
    abstract val lookupTag: ConeClassifierLookupTag

    /**
     * 结构相等：lookupTag、类型实参、属性三者同时相等才视为同一类型。
     * 子类如有额外字段（如 isRefEnum），需覆盖此方法追加比较。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeLookupTagBasedType) return false

        if (lookupTag != other.lookupTag) return false
        if (typeArguments != other.typeArguments) return false
        if (attributes != other.attributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lookupTag.hashCode()
        result = 31 * result + typeArguments.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }
}

/**
 * 所有具名分类器类型的抽象基类（class、struct、enum 均继承于此）。
 *
 * 相对于 [ConeLookupTagBasedType]，此层将 lookupTag 的类型收窄为
 * [ConeClassifierLookupTag]，明确表达"这是一个分类器类型"的语义。
 */
abstract class ConeClassifierType : ConeLookupTagBasedType() {
    abstract override val lookupTag: ConeClassLikeLookupTag
}

/**
 * 类/接口实例类型，对应仓颉编译器中的 ClassTy / InterfaceTy / ClassThisTy。
 *
 * 语义上这里专门负责"引用语义的名义类型"：
 * - 普通 `class`
 * - `interface`
 * - 类内部的 `This` 类型视图
 */
class ConeClassLikeType(
    /** 携带 ClassId 的 lookup tag，用于唯一定位类/接口声明。 */
    override val lookupTag: ConeClassLikeLookupTag,
    /** 泛型实参列表，非泛型类型时为空列表。 */
    override val typeArguments: List<ConeTypeProjection> = emptyList(),
    /** 类型附加属性（如注解投影等），默认为空。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
    /** 是否为 interface 类型实例。 */
    val isInterface: Boolean = false,
    /** 是否为类体内部出现的 `This` 类型（即 ClassThisTy）。 */
    val isThisType: Boolean = false,
) : ConeClassifierType() {

    /** 从 lookupTag 中直接提取 ClassId，方便调用方使用。 */
    val classId: ClassId get() = lookupTag.classId


}

/**
 * 结构体类型（值类型），对应仓颉编译器中的 StructTy。
 *
 * 与 [ConeClassLikeType] 的区别在于语义上是**值语义**：
 * 赋值时复制整个结构体，而非共享引用。
 */
class ConeStructType(
    /** 携带 ClassId 的 lookup tag，用于唯一定位结构体声明。 */
    override val lookupTag: ConeClassLikeLookupTag,
    /** 泛型实参列表，非泛型结构体时为空列表。 */
    override val typeArguments: List<ConeTypeProjection> = emptyList(),
    /** 类型附加属性，默认为空。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeClassifierType() {

    /** 从 lookupTag 中直接提取 ClassId，方便调用方使用。 */
    val classId: ClassId get() = lookupTag.classId

}

/**
 * 枚举类型（代数数据类型），对应仓颉编译器中的 EnumTy / RefEnumTy。
 *
 * 仓颉的 enum 是 ADT（代数数据类型），构造器可以携带参数。
 * 根据 [isRefEnum] 区分两种变体：
 * - `false`：值语义枚举（EnumTy），构造器存储在栈上。
 * - `true`：引用语义枚举（RefEnumTy），构造器存储在堆上。
 */
class ConeEnumType(
    /** 携带 ClassId 的 lookup tag，用于唯一定位枚举声明。 */
    override val lookupTag: ConeClassLikeLookupTag,
    /** 泛型实参列表，非泛型枚举时为空列表。 */
    override val typeArguments: List<ConeTypeProjection> = emptyList(),
    /** 类型附加属性，默认为空。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
    /** 是否为引用枚举类型（RefEnumTy）。 */
    val isRefEnum: Boolean = false,
) : ConeClassifierType() {

    /** 从 lookupTag 中直接提取 ClassId，方便调用方使用。 */
    val classId: ClassId get() = lookupTag.classId

    /**
     * 除基类的三项（lookupTag、typeArguments、attributes）外，
     * 还需比较 [isRefEnum]，因为值枚举与引用枚举是不同的类型。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeEnumType) return false
        return super.equals(other) &&
                isRefEnum == other.isRefEnum
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        // 将 isRefEnum 混入哈希，避免值枚举和引用枚举哈希碰撞
        result = 31 * result + isRefEnum.hashCode()
        return result
    }


}