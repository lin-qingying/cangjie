package org.cangnova.cangjie.cfir.serialization.cjd

/** 与官方 AST kind 一致；不能把同名的 class/struct 或 var/prop 合并。 */
enum class CjdDeclarationKind { FUNCTION, VARIABLE, PROPERTY, CLASS, STRUCT, INTERFACE, ENUM, TYPE_ALIAS, EXTEND, MACRO, MACRO_EXPAND, MAIN, FINALIZER }

/** 泛型存在性独立于参数数量；约束顺序与上界顺序均保留。 */
data class CjdGenericSignature(val parameterNames: List<String>, val constraints: List<CjdTypeConstraint> = emptyList())
data class CjdTypeConstraint(val type: TypeKey, val upperBounds: List<TypeKey>)
data class CjdParameterKey(val name: String, val type: TypeKey?)

/**
 * 纯结构哈希键，不包含函数返回类型、修饰符、命名参数标记、默认值或别名展开。
 * 与设计示意的差异：显式 kind 和泛型存在性；不在 equals 中实现非等价的匹配谓词。
 */
sealed interface DeclarationMatchKey {
    val identifier: String
    val kind: CjdDeclarationKind
    val generic: CjdGenericSignature?

    data class Function(
        override val identifier: String,
        val parameters: List<CjdParameterKey>,
        override val generic: CjdGenericSignature? = null,
    ) : DeclarationMatchKey { override val kind: CjdDeclarationKind get() = CjdDeclarationKind.FUNCTION }

    data class Variable(
        override val identifier: String,
        val type: TypeKey?,
        override val kind: CjdDeclarationKind = CjdDeclarationKind.VARIABLE,
        override val generic: CjdGenericSignature? = null,
    ) : DeclarationMatchKey {
        init { require(kind == CjdDeclarationKind.VARIABLE || kind == CjdDeclarationKind.PROPERTY) }
    }

    data class TypeDecl(
        override val identifier: String,
        override val kind: CjdDeclarationKind,
        override val generic: CjdGenericSignature? = null,
    ) : DeclarationMatchKey

    data class Extend(
        val extendedType: TypeKey,
        val inheritedTypes: List<TypeKey>,
        override val generic: CjdGenericSignature? = null,
    ) : DeclarationMatchKey {
        override val identifier: String get() = ""
        override val kind: CjdDeclarationKind get() = CjdDeclarationKind.EXTEND
    }

    data class MacroExpand(override val identifier: String, val fullName: String) : DeclarationMatchKey {
        override val kind: CjdDeclarationKind get() = CjdDeclarationKind.MACRO_EXPAND
        override val generic: CjdGenericSignature? get() = null
    }
}

/**
 * 从 sidecar（接收者）到目标的有向比较。约束允许目标重复后缀；缺失变量类型不限制目标。
 * 不可用作 Map 的相等性。返回全部候选，由合并层报告歧义，禁止 firstOrNull 隐藏重载。
 * 类型节点/已解析类型适配由调用方完成，不在此执行 Sema。
 */
fun DeclarationMatchKey.matches(target: DeclarationMatchKey): Boolean {
    if (identifier != target.identifier || kind != target.kind) return false
    val leftGeneric = generic
    val rightGeneric = target.generic
    if ((leftGeneric == null) != (rightGeneric == null)) return false
    if (leftGeneric != null && rightGeneric != null) {
        if (leftGeneric.parameterNames != rightGeneric.parameterNames) return false
        val left = leftGeneric.constraints
        val right = rightGeneric.constraints
        if (left.isEmpty() != right.isEmpty() || left.size > right.size) return false
        if (!left.indices.all { i -> left[i].type.matches(right[i].type) && left[i].upperBounds.matchesTypes(right[i].upperBounds) }) return false
    }
    return when (this) {
        is DeclarationMatchKey.Function -> target is DeclarationMatchKey.Function && parameters.size == target.parameters.size &&
            parameters.indices.all { i -> parameters[i].name == target.parameters[i].name &&
                (parameters[i].type?.let { type -> target.parameters[i].type?.let(type::matches) } == true) }
        is DeclarationMatchKey.Variable -> target is DeclarationMatchKey.Variable && (type == null || target.type?.let(type::matches) == true)
        is DeclarationMatchKey.Extend -> target is DeclarationMatchKey.Extend && extendedType.matches(target.extendedType) && inheritedTypes.matchesTypes(target.inheritedTypes)
        is DeclarationMatchKey.MacroExpand -> target is DeclarationMatchKey.MacroExpand && fullName == target.fullName
        is DeclarationMatchKey.TypeDecl -> target is DeclarationMatchKey.TypeDecl
    }
}

/** 无解析服务依赖的类型结构；Opaque 保留未知形状而非伪装为已知类型。 */
sealed interface TypeKey {
    data class Ref(val name: String, val arguments: List<TypeKey> = emptyList()) : TypeKey
    data class Qualified(val base: TypeKey, val name: String, val arguments: List<TypeKey> = emptyList()) : TypeKey
    data class Function(val parameters: List<TypeKey>, val result: TypeKey) : TypeKey
    data class Tuple(val elements: List<TypeKey>) : TypeKey
    data class Option(val component: TypeKey) : TypeKey
    data class VArray(val component: TypeKey, val size: Constant) : TypeKey
    data class Constant(val literalKind: String, val value: String) : TypeKey
    data class Primitive(val name: String) : TypeKey
    data class Opaque(val syntaxKind: String, val text: String) : TypeKey

    companion object {
        /** Rune/UInt8 归一化入口，目标侧适配器也应使用它。 */
        fun primitive(name: String): Primitive = Primitive(if (name == "Rune") "UInt8" else name)
    }
}

/** 官方未知左类型视为可匹配；仅此有向 API 容许 wildcard，equals/hashCode 保持结构性。 */
fun TypeKey.matches(target: TypeKey): Boolean = when (this) {
    is TypeKey.Opaque -> true
    is TypeKey.Primitive -> target is TypeKey.Primitive && TypeKey.primitive(name) == TypeKey.primitive(target.name)
    is TypeKey.Ref -> target is TypeKey.Ref && name == target.name && arguments.matchesTypes(target.arguments)
    // 官方 Type/Type 的 QualifiedType 分支不比较末段类型实参；结构键仍保留它们。
    is TypeKey.Qualified -> target is TypeKey.Qualified && name == target.name && base.matches(target.base)
    is TypeKey.Function -> target is TypeKey.Function && parameters.matchesTypes(target.parameters) && result.matches(target.result)
    is TypeKey.Tuple -> target is TypeKey.Tuple && elements.matchesTypes(target.elements)
    is TypeKey.Option -> target is TypeKey.Option && component.matches(target.component)
    is TypeKey.VArray -> target is TypeKey.VArray && component.matches(target.component) && size.matches(target.size)
    is TypeKey.Constant -> this == target
}

private fun List<TypeKey>.matchesTypes(target: List<TypeKey>): Boolean = size == target.size && indices.all { this[it].matches(target[it]) }
