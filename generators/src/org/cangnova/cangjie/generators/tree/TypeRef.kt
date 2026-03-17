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

    fun substitute(map: TypeParameterSubstitutionMap): TypeRef

    fun renderTo(appendable: Appendable, importCollector: ImportCollecting)

    val typeAnnotations: List<String>
        get() = emptyList()

    object Star : TypeRef {

        override fun substitute(map: TypeParameterSubstitutionMap) = this

        override fun toString(): String = "*"

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
    val kind: TypeKind,
    names: List<String>,
    override val args: Map<P, TypeRef>,
    override val nullable: Boolean = false,
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

    private val names = Collections.unmodifiableList(names)

    val canonicalName: String = if (names[0].isEmpty()) typeName else names.joinToString(".")

    override val packageName: String
        get() = names[0]

    val simpleName: String get() = names[names.size - 1]

    override val typeName: String
        get() = simpleNames.joinToString(separator = ".")

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

    val simpleNames: List<String> get() = names.subList(1, names.size)

    override fun copy(args: Map<P, TypeRef>) = ClassRef(kind, names, args, nullable, typeAnnotations)
    override fun copy(nullable: Boolean) = ClassRef(kind, names, args, nullable, typeAnnotations)

    override fun toString() = canonicalName
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassRef<*>) return false
        return kind == other.kind &&
                args == other.args &&
                nullable == other.nullable &&
                names == other.names &&
                typeAnnotations == other.typeAnnotations
    }

    override fun hashCode(): Int = Objects.hash(kind, args, nullable, names)
}

/**
 * 带型变信息的类型引用包装。
 */
data class TypeRefWithVariance<out T : TypeRef>(val variance: Variance, val typeRef: T) : TypeRef {

    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        if (variance != Variance.INVARIANT) {
            appendable.append(variance.label)
            appendable.append(' ')
        }
        typeRef.renderTo(appendable, importCollector)
    }

    override fun substitute(map: TypeParameterSubstitutionMap): TypeRefWithVariance<*> =
        TypeRefWithVariance(variance, typeRef.substitute(map))
}

/**
 * 元素类型或其引用的统一抽象。
 */
sealed interface ElementOrRef<Element> : ParametrizedTypeRef<ElementOrRef<Element>, NamedTypeParameterRef>, ClassOrElementRef
        where Element : AbstractElement<Element, *, *> {
    val element: Element

    override fun copy(nullable: Boolean): ElementRef<Element>
}

fun <Element : AbstractElement<Element, *, *>> ElementOrRef<Element>.toRef(): ElementRef<Element> =
    ElementRef(element, args, nullable)

/**
 * 树元素类型引用。
 */
data class ElementRef<Element : AbstractElement<Element, *, *>>(
    override val element: Element,
    override val args: Map<NamedTypeParameterRef, TypeRef> = emptyMap(),
    override val nullable: Boolean = false,
) : ElementOrRef<Element> {
    override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementRef(element, args, nullable)
    override fun copy(nullable: Boolean) = ElementRef(element, args, nullable)

    override val typeName: String
        get() = element.typeName

    override val packageName: String
        get() = element.packageName

    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(element)
        appendable.append(element.typeName)
        renderArgsTo(appendable, importCollector)
        renderNullabilityTo(appendable)
    }

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
    val receiver: TypeRefWithNullability?,
    val parameterTypes: List<TypeRefWithNullability> = emptyList(),
    val returnType: TypeRefWithNullability,
    override val nullable: Boolean = false,
) : TypeRefWithNullability {
    override fun substitute(map: TypeParameterSubstitutionMap) =
        Lambda(
            receiver?.substitute(map) as TypeRefWithNullability?,
            parameterTypes.map { it.substitute(map) as TypeRefWithNullability },
            returnType.substitute(map) as TypeRefWithNullability,
            nullable,
        )

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

    override fun copy(nullable: Boolean) = Lambda(receiver, parameterTypes, returnType, nullable)
}

/**
 * 类型参数引用抽象。
 */
sealed interface TypeParameterRef : TypeRef, TypeRefWithNullability {
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
    val index: Int,
    override val nullable: Boolean = false,
) : TypeParameterRef {
    override fun toString() = index.toString()

    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        renderingIsNotSupported()
    }

    override fun copy(nullable: Boolean) = PositionTypeParameterRef(index, nullable)
}

/**
 * 按名称引用的类型参数。
 */
open class NamedTypeParameterRef(
    val name: String,
    override val nullable: Boolean = false,
) : TypeParameterRef {
    override fun equals(other: Any?): Boolean {
        return other is NamedTypeParameterRef && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString() = name

    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        appendable.append(name)
        renderNullabilityTo(appendable)
    }

    final override fun copy(nullable: Boolean) = NamedTypeParameterRef(name, nullable)
}

/**
 * 支持可空性的类型引用。
 */
interface TypeRefWithNullability : TypeRef {
    val nullable: Boolean

    fun copy(nullable: Boolean): TypeRefWithNullability
}

fun TypeRefWithNullability.renderNullabilityTo(appendable: Appendable) {
    if (nullable) {
        appendable.append('?')
    }
}

/**
 * 参数化类型引用抽象。
 */
interface ParametrizedTypeRef<Self : ParametrizedTypeRef<Self, P>, P : TypeParameterRef> : TypeRef {
    val args: Map<P, TypeRef>

    fun copy(args: Map<P, TypeRef>): Self

    override fun substitute(map: TypeParameterSubstitutionMap): Self =
        copy(args.mapValues { it.value.substitute(map) })
}

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

fun <Self : ParametrizedTypeRef<Self, NamedTypeParameterRef>> ParametrizedTypeRef<Self, NamedTypeParameterRef>.withArgs(
    vararg args: Pair<String, TypeRef>
) = copy(args.associate { (k, v) -> NamedTypeParameterRef(k) to v })

fun <Self : ParametrizedTypeRef<Self, PositionTypeParameterRef>> ParametrizedTypeRef<Self, PositionTypeParameterRef>.withArgs(
    vararg args: TypeRef
) = copy(args.withIndex().associate { (i, t) -> PositionTypeParameterRef(i) to t })


/**
 * 类型变量定义（可带边界与型变）。
 */
class TypeVariable(
    name: String,
    val bounds: List<TypeRef> = emptyList(),
    val variance: Variance = Variance.INVARIANT,
) : NamedTypeParameterRef(name)

fun <P : TypeParameterRef> KClass<*>.asRef(): ClassRef<P> {
    val qualifiedName = this.qualifiedName ?: error("$this doesn't have qualified name and thus cannot be converted to ClassRef")
    return java.asRef(qualifiedName)
}

fun <P : TypeParameterRef> Class<*>.asRef(qualifiedName: String = this.name): ClassRef<P> {
    val kind = if (isInterface) TypeKind.Interface else TypeKind.Class
    val parts = qualifiedName.split('.')
    val indexWhereClassNameStarts = parts.indexOfFirst { it.first().isUpperCase() }
    val packageName = parts.take(indexWhereClassNameStarts).joinToString(separator = ".")
    val simpleNames = parts.drop(indexWhereClassNameStarts)
    return ClassRef(kind, packageName, *simpleNames.toTypedArray())
}

inline fun <reified T : Any> type() = T::class.asRef<PositionTypeParameterRef>()
inline fun <reified T : Any> refNamed() = T::class.asRef<NamedTypeParameterRef>()
inline fun <reified T : Any> type(vararg args: Pair<String, TypeRef>) = T::class.asRef<NamedTypeParameterRef>().withArgs(*args)
inline fun <reified T : Any> type(vararg args: TypeRef) = T::class.asRef<PositionTypeParameterRef>().withArgs(*args)

fun type(packageName: String, name: String, kind: TypeKind = TypeKind.Interface) =
    ClassRef<PositionTypeParameterRef>(kind, packageName, name)

val ClassOrElementRef.typeKind: TypeKind
    get() = when (this) {
        is ElementOrRef<*> -> element.kind!!.typeKind
        is ClassRef<*> -> kind
    }

val TypeRef.nullable: Boolean
    get() = (this as? TypeRefWithNullability)?.nullable ?: false

fun TypeRef.renderingIsNotSupported(): Nothing = error("Rendering is not supported for ${this::class.simpleName}")
