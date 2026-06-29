

package org.cangnova.cangjie.generators.tree

/**
 * 表示存储任意元素列表的字段。
 */
interface ListField {

    /**
     * 列表的元素类型。
     */
    val baseType: TypeRef

    /**
     * 字段的列表类型，例如 [List] 或 [MutableList]。
     */
    val listType: ClassRef<PositionTypeParameterRef>

    /**
     * 将列表类型和元素类型组合后的完整字段类型。
     *
     * 生成属性、构造参数和函数签名时应使用该类型，而不是分别读取 [listType] 与 [baseType]。
     */
    val typeRef: ClassRef<PositionTypeParameterRef>
        get() = listType.withArgs(baseType)
}
