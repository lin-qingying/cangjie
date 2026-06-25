package org.cangnova.cangjie.cfir.tree.generator.context

import org.cangnova.cangjie.cfir.tree.generator.model.*
import org.cangnova.cangjie.cfir.tree.generator.model.ListField
import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.ElementOrRef as GenericElementOrRef
import org.cangnova.cangjie.generators.tree.config.AbstractElementConfigurator

/**
 * CFIR tree 元素配置器基类。
 */
abstract class AbstractCfirTreeBuilder : AbstractElementConfigurator<Element, Field, Element.Kind>() {
    /**
     * 创建 CFIR 元素元模型实例。
     */
    override fun createElement(name: String, propertyName: String, category: Element.Kind): Element {
        return Element(name, propertyName, category)
    }

    /**
     * 按显式名称和类型创建普通字段。
     */
    protected fun field(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        isChild: Boolean = true,
        initializer: SimpleField.() -> Unit = {},
    ): SimpleField {
        val isMutable = type is GenericElementOrRef<*> || withReplace
        return SimpleField(
            name = name,
            typeRef = type.copy(nullable),
            isChild = isChild,
            isMutable = isMutable,
            withReplace = withReplace,
            withTransform = withTransform,
        ).apply(initializer)
    }

    /**
     * 按类型自动推导字段名称并创建普通字段。
     */
    protected fun field(
        type: ClassOrElementRef,
        nullable: Boolean = false,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        isChild: Boolean = true,
        initializer: SimpleField.() -> Unit = {},
    ): SimpleField {
        val name = when (type) {
            is ClassRef<*> -> type.simpleName
            is GenericElementOrRef<*> -> type.element.name
        }.replaceFirstChar(Char::lowercaseChar)

        return field(
            name = name,
            type = type,
            nullable = nullable,
            withReplace = withReplace,
            withTransform = withTransform,
            isChild = isChild,
            initializer = initializer,
        )
    }

    /**
     * 按显式名称和元素类型创建列表字段。
     */
    protected fun listField(
        name: String,
        baseType: TypeRef,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        useMutableOrEmpty: Boolean = false,
        isChild: Boolean = true,
        initializer: ListField.() -> Unit = {},
    ): Field {
        return ListField(
            name = name,
            baseType = baseType,
            withReplace = withReplace,
            withTransform = withTransform,
            isChild = isChild,
            isMutableOrEmptyList = useMutableOrEmpty,
        ).apply(initializer)
    }

    /**
     * 按元素引用自动推导字段名称并创建列表字段。
     */
    protected fun listField(
        elementOrRef: GenericElementOrRef<*>,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        useMutableOrEmpty: Boolean = false,
        isChild: Boolean = true,
        initializer: ListField.() -> Unit = {},
    ): Field {
        val name = elementOrRef.element.name.replaceFirstChar(Char::lowercaseChar) + "s"
        return listField(
            name = name,
            baseType = elementOrRef,
            withReplace = withReplace,
            withTransform = withTransform,
            useMutableOrEmpty = useMutableOrEmpty,
            isChild = isChild,
            initializer = initializer,
        )
    }

    /**
     * 创建声明自身持有的 symbol 字段。
     */
    protected fun declaredSymbol(name: String, symbolType: ClassRef<*>): Field =
        field(name, symbolType).apply {
            symbolFieldRole = AbstractField.SymbolFieldRole.DECLARED
            skippedInCopy = true
        }

    /**
     * 创建默认名为 `symbol` 的声明 symbol 字段。
     */
    protected fun declaredSymbol(symbolType: ClassRef<*>): Field = declaredSymbol("symbol", symbolType)

    /**
     * 创建引用到其他声明的 symbol 字段。
     */
    protected fun referencedSymbol(
        name: String,
        symbolType: ClassRef<*>,
        nullable: Boolean = false,
        withReplace: Boolean = false,
        initializer: SimpleField.() -> Unit = {},
    ): Field = field(name, symbolType, nullable, withReplace)
        .apply { symbolFieldRole = AbstractField.SymbolFieldRole.REFERENCED }
        .apply(initializer)

    /**
     * 创建默认名为 `symbol` 的引用 symbol 字段。
     */
    protected fun referencedSymbol(
        symbolType: ClassRef<*>,
        nullable: Boolean = false,
        withReplace: Boolean = false,
        initializer: SimpleField.() -> Unit = {},
    ): Field = referencedSymbol("symbol", symbolType, nullable, withReplace, initializer)

    /**
     * 为元素批量创建 Boolean 标志字段。
     */
    protected fun Element.generateBooleanFields(vararg names: String) {
        names.forEach {
            +field(
                if (it.startsWith("is") || it.startsWith("has")) it else "is${it.replaceFirstChar(Char::uppercaseChar)}",
                StandardTypes.boolean,
            )
        }
    }

    /**
     * 标记元素需要生成 transformOtherChildren。
     */
    protected fun Element.needTransformOtherChildren() {
        _needTransformOtherChildren = true
    }
}
