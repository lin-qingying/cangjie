/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.imports.Importable
import java.util.*
import kotlin.reflect.KClass

/**
 * 生成器内部的类型引用抽象。
 */
interface TypeRef {

    /**
     * 根据类型参数替换表返回替换后的类型引用。
     *
     * 不含类型参数的实现应返回自身；包含类型参数的实现需要递归替换所有类型实参。
     */
    fun substitute(map: TypeParameterSubstitutionMap): TypeRef

    /**
     * 将该类型引用渲染为可写入生成源码的 Kotlin 类型文本。
     *
     * 渲染过程中应把需要的导入记录到 [importCollector]，调用方负责最终输出 import 列表。
     */
    fun renderTo(appendable: Appendable, importCollector: ImportCollecting)

    /**
     * 直接附着在类型使用位置上的注解文本。
     *
     * 注解文本可以包含或不包含 `@` 前缀，具体渲染实现负责规范化。
     */
    val typeAnnotations: List<String>
        get() = emptyList()

    /**
     * 星号投影类型实参。
     *
     * 该引用只用于参数化类型实参位置，不携带包名、可空性或导入信息。
     */
    object Star : TypeRef {

        /**
         * 星号投影不受类型参数替换影响，始终返回自身。
         */
        override fun substitute(map: TypeParameterSubstitutionMap) = this

        /**
         * 返回 Kotlin 星号投影的源代码文本。
         */
        override fun toString(): String = "*"

        /**
         * 将星号投影写入目标输出。
         */
        override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
            appendable.append(toString())
        }
    }
}

/**
 * 可表示为类引用或元素引用的类型。
 */
sealed interface ClassOrElementRef : TypeRefWithNullability, Importable

/**
 * 普通类/接口类型引用。
 */
class ClassRef<P : TypeParameterRef> private constructor(
    /**
     * 被引用目标在源码层面的类型种类。
     *
     * 该信息用于继承子句、实现子句等需要区分类与接口的生成位置。
     */
    val kind: TypeKind,
    names: List<String>,
    /**
     * 已绑定到该类引用上的类型实参。
     *
     * 键表示位置型或命名型类型参数引用，值表示实际传入的类型。
     */
    override val args: Map<P, TypeRef>,
    /**
     * 该类型使用位置是否带有 Kotlin 可空标记。
     */
    override val nullable: Boolean = false,
    /**
     * 该类型使用位置上的注解文本。
     */
    override val typeAnnotations: List<String> = emptyList(),
) : ParametrizedTypeRef<ClassRef<P>, P>, ClassOrElementRef {

    constructor(
        kind: TypeKind,
        packageName: String,
        vararg simpleNames: String,
        args: Map<P, TypeRef> = emptyMap(),
        typeAnnotations: List<String> = emptyList(),
    ) : this(kind, listOf(packageName, *simpleNames), args, false, typeAnnotations) {
        require(simpleNames.isNotEmpty()) { "simpleNames must not be empty" }
        require(simpleNames.none { it.isEmpty() }) {
            "simpleNames must not contain empty items: ${simpleNames.contentToString()}"
        }
    }

    /**
     * 包名与嵌套类型名组成的不可变名称片段。
     *
     * 第一项固定是包名，后续项是外层到内层的简单类型名。
     */
    private val names = Collections.unmodifiableList(names)

    /**
     * 完整限定名。
     *
     * 顶层无包名类型会直接返回 [typeName]，避免生成前导点。
     */
    val canonicalName: String = if (names[0].isEmpty()) typeName else names.joinToString(".")

    /**
     * 被引用类型所在包名。
     */
    override val packageName: String
        get() = names[0]

    /**
     * 最内层类型的简单名称。
     */
    val simpleName: String get() = names[names.size - 1]

    /**
     * 用于源码中引用类型的名称。
     *
     * 对嵌套类型会保留 `Outer.Inner` 结构。
     */
    override val typeName: String
        get() = simpleNames.joinToString(separator = ".")

    /**
     * 渲染类引用、类型注解、类型实参与可空标记。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(this)
        for (annotation in typeAnnotations) {
            if (!annotation.startsWith("@")) {
                appendable.append("@")
            }
            appendable.append(annotation, " ")
        }
        simpleNames.joinTo(appendable, separator = ".")
        renderArgsTo(appendable, importCollector)
        renderNullabilityTo(appendable)
    }

    /**
     * 从外层到内层的简单类型名列表。
     */
    val simpleNames: List<String> get() = names.subList(1, names.size)

    /**
     * 使用新的类型实参复制当前类引用。
     */
    override fun copy(args: Map<P, TypeRef>) = ClassRef(kind, names, args, nullable, typeAnnotations)

    /**
     * 使用新的可空性复制当前类引用。
     */
    override fun copy(nullable: Boolean) = ClassRef(kind, names, args, nullable, typeAnnotations)

    /**
     * 返回完整限定名，便于调试和错误消息输出。
     */
    override fun toString() = canonicalName

    /**
     * 按类型种类、实参、可空性、名称和类型注解比较类引用。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassRef<*>) return false
        return kind == other.kind &&
                args == other.args &&
                nullable == other.nullable &&
                names == other.names &&
                typeAnnotations == other.typeAnnotations
    }

    /**
     * 与 [equals] 使用同一组结构字段计算哈希值。
     */
    override fun hashCode(): Int = Objects.hash(kind, args, nullable, names)
}

/**
 * 带型变信息的类型引用包装。
 */
data class TypeRefWithVariance<out T : TypeRef>(val variance: Variance, val typeRef: T) : TypeRef {

    /**
     * 渲染型变前缀和实际类型引用。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        if (variance != Variance.INVARIANT) {
            appendable.append(variance.label)
            appendable.append(' ')
        }
        typeRef.renderTo(appendable, importCollector)
    }

    /**
     * 对内部类型引用执行类型参数替换，并保留原有型变。
     */
    override fun substitute(map: TypeParameterSubstitutionMap): TypeRefWithVariance<*> =
        TypeRefWithVariance(variance, typeRef.substitute(map))
}

/**
 * 元素类型或其引用的统一抽象。
 */
sealed interface ElementOrRef<Element> : ParametrizedTypeRef<ElementOrRef<Element>, NamedTypeParameterRef>, ClassOrElementRef
        where Element : AbstractElement<Element, *, *> {
    /**
     * 被引用的树元素定义。
     */
    val element: Element

    /**
     * 使用新的可空性复制元素引用。
     */
    override fun copy(nullable: Boolean): ElementRef<Element>
}

/**
 * 将元素或元素引用规范化为 [ElementRef]。
 *
 * 已有的类型实参与可空性会被保留。
 */
fun <Element : AbstractElement<Element, *, *>> ElementOrRef<Element>.toRef(): ElementRef<Element> =
    ElementRef(element, args, nullable)

/**
 * 树元素类型引用。
 */
data class ElementRef<Element : AbstractElement<Element, *, *>>(
    /**
     * 当前引用指向的树元素。
     */
    override val element: Element,
    /**
     * 绑定到元素声明类型参数上的实参。
     */
    override val args: Map<NamedTypeParameterRef, TypeRef> = emptyMap(),
    /**
     * 当前元素类型使用位置是否可空。
     */
    override val nullable: Boolean = false,
) : ElementOrRef<Element> {
    /**
     * 使用新的类型实参复制当前元素引用。
     */
    override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementRef(element, args, nullable)

    /**
     * 使用新的可空性复制当前元素引用。
     */
    override fun copy(nullable: Boolean) = ElementRef(element, args, nullable)

    /**
     * 元素在生成源码中的类型名。
     */
    override val typeName: String
        get() = element.typeName

    /**
     * 元素生成文件所在包名。
     */
    override val packageName: String
        get() = element.packageName

    /**
     * 渲染元素类型名、类型实参和可空标记，并收集对应导入。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(element)
        appendable.append(element.typeName)
        renderArgsTo(appendable, importCollector)
        renderNullabilityTo(appendable)
    }

    /**
     * 返回包含类型实参和可空标记的调试字符串。
     */
    override fun toString() = buildString {
        append(element.typeName)
        append("<")
        append(args)
        append(">")
        if (nullable) {
            append("?")
        }
    }
}

/**
 * Lambda 类型引用。
 */
data class Lambda(
    /**
     * Lambda 的扩展接收者类型；普通函数类型没有接收者时为 `null`。
     */
    val receiver: TypeRefWithNullability?,
    /**
     * Lambda 参数类型列表。
     */
    val parameterTypes: List<TypeRefWithNullability> = emptyList(),
    /**
     * Lambda 返回值类型。
     */
    val returnType: TypeRefWithNullability,
    /**
     * 整个 Lambda 类型使用位置是否可空。
     */
    override val nullable: Boolean = false,
) : TypeRefWithNullability {
    /**
     * 递归替换接收者、参数和返回值中的类型参数。
     */
    override fun substitute(map: TypeParameterSubstitutionMap) =
        Lambda(
            receiver?.substitute(map) as TypeRefWithNullability?,
            parameterTypes.map { it.substitute(map) as TypeRefWithNullability },
            returnType.substitute(map) as TypeRefWithNullability,
            nullable,
        )

    /**
     * 渲染 Kotlin Lambda 类型文本。
     *
     * 可空 Lambda 会整体包裹在括号中，避免 `?` 与函数箭头优先级产生歧义。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        if (nullable) appendable.append("(")
        receiver?.let {
            it.renderTo(appendable, importCollector)
            appendable.append('.')
        }
        parameterTypes.joinToWithBuffer(appendable, prefix = "(", postfix = ") -> ") { it.renderTo(this, importCollector) }
        returnType.renderTo(appendable, importCollector)
        if (nullable) appendable.append(")?")
    }

    /**
     * 使用新的可空性复制当前 Lambda 类型。
     */
    override fun copy(nullable: Boolean) = Lambda(receiver, parameterTypes, returnType, nullable)
}

/**
 * 类型参数引用抽象。
 */
sealed interface TypeParameterRef : TypeRef, TypeRefWithNullability {
    /**
     * 使用替换表解析当前类型参数。
     *
     * 如果替换结果本身支持可空性，会把当前引用的可空标记合并到替换结果上。
     */
    override fun substitute(map: TypeParameterSubstitutionMap): TypeRef {
        map[this]?.let {
            return (it as? TypeRefWithNullability)?.copy(this.nullable) ?: it
        }
        return this
    }
}

/**
 * 按位置索引的类型参数引用。
 */
data class PositionTypeParameterRef(
    /**
     * 类型参数在声明中的位置索引。
     */
    val index: Int,
    /**
     * 当前类型参数使用位置是否可空。
     */
    override val nullable: Boolean = false,
) : TypeParameterRef {
    /**
     * 返回位置索引文本，主要用于调试输出。
     */
    override fun toString() = index.toString()

    /**
     * 位置型类型参数不能直接渲染为源码名称。
     *
     * 这类引用必须先通过上下文映射到真实类型实参。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        renderingIsNotSupported()
    }

    /**
     * 使用新的可空性复制当前位置型类型参数引用。
     */
    override fun copy(nullable: Boolean) = PositionTypeParameterRef(index, nullable)
}

/**
 * 按名称引用的类型参数。
 */
open class NamedTypeParameterRef(
    /**
     * 类型参数在生成源码中的名称。
     */
    val name: String,
    /**
     * 当前类型参数使用位置是否可空。
     */
    override val nullable: Boolean = false,
) : TypeParameterRef {
    /**
     * 命名类型参数只按名称判断相等性。
     */
    override fun equals(other: Any?): Boolean {
        return other is NamedTypeParameterRef && other.name == name
    }

    /**
     * 返回基于 [name] 的哈希值。
     */
    override fun hashCode(): Int {
        return name.hashCode()
    }

    /**
     * 返回类型参数名称。
     */
    override fun toString() = name

    /**
     * 渲染类型参数名称和可空标记。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        appendable.append(name)
        renderNullabilityTo(appendable)
    }

    /**
     * 使用新的可空性复制当前命名类型参数引用。
     */
    final override fun copy(nullable: Boolean) = NamedTypeParameterRef(name, nullable)
}

/**
 * 支持可空性的类型引用。
 */
interface TypeRefWithNullability : TypeRef {
    /**
     * 当前类型使用位置是否带有 Kotlin 可空标记。
     */
    val nullable: Boolean

    /**
     * 使用新的可空性复制当前类型引用。
     */
    fun copy(nullable: Boolean): TypeRefWithNullability
}

/**
 * 按需向输出追加 Kotlin 可空标记。
 */
fun TypeRefWithNullability.renderNullabilityTo(appendable: Appendable) {
    if (nullable) {
        appendable.append('?')
    }
}

/**
 * 参数化类型引用抽象。
 */
interface ParametrizedTypeRef<Self : ParametrizedTypeRef<Self, P>, P : TypeParameterRef> : TypeRef {
    /**
     * 当前参数化类型已绑定的类型实参。
     */
    val args: Map<P, TypeRef>

    /**
     * 使用新的类型实参复制当前参数化类型引用。
     */
    fun copy(args: Map<P, TypeRef>): Self

    /**
     * 对全部类型实参执行类型参数替换。
     */
    override fun substitute(map: TypeParameterSubstitutionMap): Self =
        copy(args.mapValues { it.value.substitute(map) })
}

/**
 * 将参数化类型实参列表渲染到输出。
 *
 * 没有实参时不写入任何内容。
 */
private fun ParametrizedTypeRef<*, *>.renderArgsTo(appendable: Appendable, importCollector: ImportCollecting) {
    if (args.isNotEmpty()) {
        args.values.joinTo(appendable, prefix = "<", postfix = ">") {
            it.renderTo(appendable, importCollector)
            ""
        }
    }
}

/** 类型参数替换映射。 */
typealias TypeParameterSubstitutionMap = Map<out TypeParameterRef, TypeRef>

/**
 * 为命名型参数化类型绑定类型实参。
 *
 * @param args 参数名到实际类型引用的映射项。
 */
fun <Self : ParametrizedTypeRef<Self, NamedTypeParameterRef>> ParametrizedTypeRef<Self, NamedTypeParameterRef>.withArgs(
    vararg args: Pair<String, TypeRef>
) = copy(args.associate { (k, v) -> NamedTypeParameterRef(k) to v })

/**
 * 为位置型参数化类型按传入顺序绑定类型实参。
 *
 * 每个实参会按照数组下标生成 [PositionTypeParameterRef]。
 */
fun <Self : ParametrizedTypeRef<Self, PositionTypeParameterRef>> ParametrizedTypeRef<Self, PositionTypeParameterRef>.withArgs(
    vararg args: TypeRef
) = copy(args.withIndex().associate { (i, t) -> PositionTypeParameterRef(i) to t })


/**
 * 类型变量定义（可带边界与型变）。
 */
class TypeVariable(
    name: String,
    /**
     * 类型变量声明上的上界列表。
     */
    val bounds: List<TypeRef> = emptyList(),
    /**
     * 类型变量声明上的型变。
     */
    val variance: Variance = Variance.INVARIANT,
) : NamedTypeParameterRef(name)

/**
 * 将 Kotlin 反射类转换为位置型 [ClassRef]。
 *
 * 该方法依赖 [KClass.qualifiedName]，匿名类或本地类没有限定名时会失败。
 */
fun <P : TypeParameterRef> KClass<*>.asRef(): ClassRef<P> {
    val qualifiedName = this.qualifiedName ?: error("$this doesn't have qualified name and thus cannot be converted to ClassRef")
    return java.asRef(qualifiedName)
}

/**
 * 将 Java [Class] 转换为生成器内部的 [ClassRef]。
 *
 * @param qualifiedName 用于拆分包名和简单名的限定名，默认使用 [Class.name]。
 */
fun <P : TypeParameterRef> Class<*>.asRef(qualifiedName: String = this.name): ClassRef<P> {
    val kind = if (isInterface) TypeKind.Interface else TypeKind.Class
    val parts = qualifiedName.split('.')
    val indexWhereClassNameStarts = parts.indexOfFirst { it.first().isUpperCase() }
    val packageName = parts.take(indexWhereClassNameStarts).joinToString(separator = ".")
    val simpleNames = parts.drop(indexWhereClassNameStarts)
    return ClassRef(kind, packageName, *simpleNames.toTypedArray())
}

/**
 * 获取无类型实参的 reified 类引用。
 */
inline fun <reified T : Any> type() = T::class.asRef<PositionTypeParameterRef>()

/**
 * 获取使用命名类型参数空间的 reified 类引用。
 */
inline fun <reified T : Any> refNamed() = T::class.asRef<NamedTypeParameterRef>()

/**
 * 获取 reified 类引用并按命名类型参数绑定实参。
 */
inline fun <reified T : Any> type(vararg args: Pair<String, TypeRef>) = T::class.asRef<NamedTypeParameterRef>().withArgs(*args)

/**
 * 获取 reified 类引用并按位置绑定类型实参。
 */
inline fun <reified T : Any> type(vararg args: TypeRef) = T::class.asRef<PositionTypeParameterRef>().withArgs(*args)

/**
 * 通过包名和简单类型名创建普通类/接口引用。
 */
fun type(packageName: String, name: String, kind: TypeKind = TypeKind.Interface) =
    ClassRef<PositionTypeParameterRef>(kind, packageName, name)

/**
 * 返回类或元素引用对应的源码类型种类。
 */
val ClassOrElementRef.typeKind: TypeKind
    get() = when (this) {
        is ElementOrRef<*> -> element.kind!!.typeKind
        is ClassRef<*> -> kind
    }

/**
 * 读取任意类型引用的可空性。
 *
 * 不支持可空性的类型引用按非空处理。
 */
val TypeRef.nullable: Boolean
    get() = (this as? TypeRefWithNullability)?.nullable ?: false

/**
 * 报告当前类型引用不支持直接渲染。
 */
fun TypeRef.renderingIsNotSupported(): Nothing = error("Rendering is not supported for ${this::class.simpleName}")
