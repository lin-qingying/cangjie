/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.model

import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.ListField as AbstractListField

/**
 * CFIR tree 字段元模型基类。
 */
sealed class Field : AbstractField<Field>() {
    /**
     * 是否为该字段生成 replace 方法。
     */
    abstract var withReplace: Boolean
    /**
     * 是否为该字段生成 transform 方法。
     */
    abstract var withTransform: Boolean

    /**
     * 是否在 transformOtherChildren 中处理该字段。
     */
    open var needTransformInOtherChildren: Boolean = false
    /**
     * 生成实现初始化时是否绑定 this-owner 符号。
     */
    var withBindThis = true

    /**
     * builder 中该字段的默认值表达式。
     */
    override var defaultValueInBuilder: String? = null
    /**
     * 生成实现中该字段的自定义 setter 文本。
     */
    override var customSetter: String? = null

    /**
     * 该字段是否生成 volatile 修饰。
     */
    abstract override var isVolatile: Boolean
    /**
     * 该字段是否生成 final 语义。
     */
    abstract override var isFinal: Boolean
    /**
     * 该字段是否作为构造参数生成。
     */
    abstract override var isParameter: Boolean
    /**
     * 该字段是否为可变属性。
     */
    abstract override var isMutable: Boolean

    /**
     * replace 方法是否需要接收可空类型。
     */
    val receiveNullableTypeInReplace: Boolean
        get() = typeRef.nullable || overriddenFields.any { it.typeRef.nullable }

    /**
     * 把当前字段的 CFIR 扩展属性同步到复制结果。
     */
    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.withTransform = withTransform
        copy.needTransformInOtherChildren = needTransformInOtherChildren
        copy.customInitializationCall = customInitializationCall
        copy.skippedInCopy = skippedInCopy
    }

    /**
     * 根据父字段继承 replace / transform 等生成属性。
     */
    override fun updatePropertiesFromOverriddenFields(parentFields: List<Field>) {
        super.updatePropertiesFromOverriddenFields(parentFields)
        withTransform = withTransform || parentFields.any { it.withTransform }
        needTransformInOtherChildren = needTransformInOtherChildren || parentFields.any { it.needTransformInOtherChildren }
        withReplace = withReplace || parentFields.any { it.withReplace }
    }
}

/**
 * 普通单值字段元模型。
 */
class SimpleField(
    /**
     * 字段名称。
     */
    override val name: String,
    /**
     * 字段类型。
     */
    override var typeRef: TypeRefWithNullability,
    /**
     * 字段值是否为 CFIR 子节点。
     */
    override val isChild: Boolean,
    /**
     * 字段是否生成为可变属性。
     */
    override var isMutable: Boolean,
    /**
     * 是否生成 replace 方法。
     */
    override var withReplace: Boolean,
    /**
     * 是否生成 transform 方法。
     */
    override var withTransform: Boolean,
    /**
     * 是否生成 volatile 修饰。
     */
    override var isVolatile: Boolean = false,
    /**
     * 是否生成 final 语义。
     */
    override var isFinal: Boolean = false,
    /**
     * 是否作为构造参数生成。
     */
    override var isParameter: Boolean = false,
) : Field() {

    /**
     * 创建当前普通字段的深复制。
     */
    override fun internalCopy(): Field {
        return SimpleField(
            name = name,
            typeRef = typeRef,
            isChild = isChild,
            isMutable = isMutable,
            withReplace = withReplace,
            withTransform = withTransform,
            isVolatile = isVolatile,
            isFinal = isFinal,
            isParameter = isParameter,
        ).apply {
            withBindThis = this@SimpleField.withBindThis
        }
    }

    /**
     * 按类型参数替换表更新字段类型。
     */
    override fun substituteType(map: TypeParameterSubstitutionMap) {
        typeRef = typeRef.substitute(map) as TypeRefWithNullability
    }
}

/**
 * 列表字段元模型。
 */
class ListField(
    /**
     * 字段名称。
     */
    override val name: String,
    /**
     * 列表元素类型。
     */
    override var baseType: TypeRef,
    /**
     * 是否生成 replace 方法。
     */
    override var withReplace: Boolean,
    /**
     * 是否生成 transform 方法。
     */
    override var withTransform: Boolean,
    /**
     * 列表元素是否为 CFIR 子节点。
     */
    override val isChild: Boolean,
    /**
     * 是否使用 MutableOrEmptyList 作为可变列表表示。
     */
    val isMutableOrEmptyList: Boolean = false,
) : Field(), AbstractListField {
    /**
     * 列表字段完整类型。
     */
    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = super.typeRef

    /**
     * 列表字段使用的集合类型。
     */
    override val listType: ClassRef<PositionTypeParameterRef>
        get() = StandardTypes.list

    /**
     * 列表字段默认不生成 volatile。
     */
    override var isVolatile: Boolean = false
    /**
     * 列表字段默认不生成 final。
     */
    override var isFinal: Boolean = false
    /**
     * 列表字段默认生成为可变属性。
     */
    override var isMutable: Boolean = true
    /**
     * 列表字段默认不作为构造参数。
     */
    override var isParameter: Boolean = false

    /**
     * 创建当前列表字段的深复制。
     */
    override fun internalCopy(): Field {
        return ListField(
            name = name,
            baseType = baseType,
            withReplace = withReplace,
            withTransform = withTransform,
            isChild = isChild,
            isMutableOrEmptyList = isMutableOrEmptyList,
        )
    }

    /**
     * 按类型参数替换表更新列表元素类型。
     */
    override fun substituteType(map: TypeParameterSubstitutionMap) {
        baseType = baseType.substitute(map)
    }
}
