package org.cangnova.cangjie.cfir.serialization.cjd

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.ANNOTATIONS
import java.nio.file.Path

/** 只遍历 parser 已确定的语法结构；不以文本模式重新解释仓颉声明。 */
internal class CjdStructureExtractor(private val source: String) {
    private val diagnostics = mutableListOf<CjdDiagnostic>()
    private fun text(node: CjdSyntaxNode): String = source.substring(node.start, node.end)
    private fun identifier(node: CjdSyntaxNode): String = text(node).removeSurrounding("`")
    private fun name(node: CjdSyntaxNode): String = node.child(CjTokens.IDENTIFIER)?.let(::identifier)
        ?: node.child(REFERENCE_EXPRESSION)?.let(::identifier).orEmpty()

    fun extract(root: CjdSyntaxNode, sourceId: String, path: Path?): CjdSidecarIndex {
        fun errors(node: CjdSyntaxNode) {
            if (node.kind == TokenType.ERROR_ELEMENT) diagnostic(node, CjdDiagnosticKind.SYNTAX, node.error ?: "Syntax error")
            if (node.kind == MACRO_EXPRESSION) diagnostic(node, CjdDiagnosticKind.MACRO_NOT_EXPANDED, "Macro invocation is not expanded by sidecar parsing")
            node.children.forEach(::errors)
        }
        errors(root)
        val declarations = declarations(root)
        return CjdSidecarIndexImpl(sourceId, path, declarations, diagnostics.toList(), annotationContext(root, declarations))
    }

    /** 包和导入均来自语法节点，分组导入展开但不解析其目标。 */
    private fun annotationContext(root: CjdSyntaxNode, declarations: List<CjdDeclarationEntry>): CjdAnnotationContext {
        fun path(node: CjdSyntaxNode?): Pair<String?, String> {
            val tokens = node?.let(::leaves).orEmpty()
            val names = tokens.filter { it.kind == CjTokens.IDENTIFIER }.map(::identifier)
            val organization = if (tokens.any { it.kind == CjTokens.DOUBLE_COLON }) names.firstOrNull() else null
            return organization to (if (organization == null) names else names.drop(1)).joinToString(".")
        }
        val pkg = path(root.child(PACKAGE_DIRECTIVE))
        val imports = root.child(IMPORT_LIST)?.children(IMPORT_DIRECTIVE).orEmpty().flatMap { directive ->
            val baseNodes = directive.children.filter { it.kind == DOT_QUALIFIED_EXPRESSION || it.kind == REFERENCE_EXPRESSION }
            val base = baseNodes.singleOrNull()?.let(::path) ?: (null to "")
            val organizationOnly = directive.child(CjTokens.DOUBLE_COLON) != null
            directive.children(IMPORT_ITEM).map { item ->
                val reference = item.children.firstOrNull { it.kind == DOT_QUALIFIED_EXPRESSION || it.kind == REFERENCE_EXPRESSION }
                val local = path(reference)
                CjdAnnotationImport(
                    listOf(if (organizationOnly) "" else base.second, local.second).filter(String::isNotEmpty).joinToString("."),
                    item.child(CjTokens.MUL) != null,
                    item.child(IMPORT_ALIAS)?.child(CjTokens.IDENTIFIER)?.let(::identifier),
                    local.first ?: base.first ?: base.second.takeIf { organizationOnly },
                )
            }
        }
        return CjdAnnotationContext(pkg.second, pkg.first, imports, declarations.map { it.key.identifier }.filter(String::isNotEmpty).toSet())
    }

    private fun diagnostic(node: CjdSyntaxNode, kind: CjdDiagnosticKind, message: String) {
        diagnostics += CjdDiagnostic(kind, node.range, message)
    }

    private fun declarations(container: CjdSyntaxNode): List<CjdDeclarationEntry> = container.children.flatMap { node ->
        when (node.kind) {
            FOREIGN, FOREIGN_BODY -> declarations(node)
            in declarationKinds -> declaration(node)
            MACRO_EXPRESSION -> emptyList()
            INVALID_DECLARATION -> {
                diagnostic(node, CjdDiagnosticKind.UNSUPPORTED_DECLARATION, "Invalid declaration")
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun declaration(node: CjdSyntaxNode): List<CjdDeclarationEntry> {
        val annotations = annotations(node)
        val generic = generic(node)
        val parameters = node.child(VALUE_PARAMETER_LIST)?.children(VALUE_PARAMETER).orEmpty()
        val parameterKeys = parameters.map { CjdParameterKey(name(it), it.child(TYPE_REFERENCE)?.let(::type)) }
        val members = node.children.filter { it.kind in bodyKinds }.flatMap(::declarations)
        fun entry(key: DeclarationMatchKey, parameterAnnotations: List<List<CjdAnnotation>> = parameters.map(::annotations)) =
            CjdDeclarationEntry(key, annotations, members, parameterAnnotations, node.range)
        if (node.kind == VARIABLE) {
            val declaredType = node.child(TYPE_REFERENCE)?.let(::type)
            val patterns = node.children.filter { it.kind in patternKinds }
            return patterns.flatMap { pattern -> bindings(pattern, declaredType) }.map { (id, type) ->
                entry(DeclarationMatchKey.Variable(id, type), emptyList())
            }.also {
                if (it.isEmpty() && patterns.none { p -> p.kind == WILDCARD_PATTERN }) {
                    diagnostic(node, CjdDiagnosticKind.UNSUPPORTED_DECLARATION, "Variable pattern has no extractable binding")
                }
            }
        }
        val key = when (node.kind) {
            FUNC -> DeclarationMatchKey.Function(functionName(node), parameterKeys, generic)
            PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR -> DeclarationMatchKey.Function("init", parameterKeys, generic)
            FINALIZER -> DeclarationMatchKey.Function("~init", parameterKeys, generic)
            MAIN_FUNC -> DeclarationMatchKey.TypeDecl("main", CjdDeclarationKind.MAIN, generic)
            ENUM_CONSTRUCTOR -> {
                val payload = node.child(TYPE_LIST)?.children(TYPE_REFERENCE)
                if (payload == null) DeclarationMatchKey.Variable(name(node), null)
                else DeclarationMatchKey.Function(name(node), payload.mapIndexed { i, t -> CjdParameterKey("p${i + 1}", type(t)) })
            }
            FIELD -> DeclarationMatchKey.Variable(name(node), node.child(TYPE_REFERENCE)?.let(::type), generic = generic)
            PROPERTY -> DeclarationMatchKey.Variable(name(node), node.child(TYPE_REFERENCE)?.let(::type), CjdDeclarationKind.PROPERTY, generic)
            EXTEND -> DeclarationMatchKey.Extend(
                node.child(TYPE_REFERENCE)?.let(::type) ?: opaque(node),
                node.child(SUPER_TYPE_LIST)?.children(SUPER_TYPE_ENTRY).orEmpty().map { type(checkNotNull(it.child(TYPE_REFERENCE))) }, generic)
            else -> DeclarationMatchKey.TypeDecl(name(node), checkNotNull(declarationKinds[node.kind]), generic)
        }
        if (key.identifier.isEmpty() && key !is DeclarationMatchKey.Extend) {
            diagnostic(node, CjdDiagnosticKind.UNSUPPORTED_DECLARATION, "Missing declaration identifier: ${node.kind}")
        }
        return listOf(entry(key, if (node.kind == ENUM_CONSTRUCTOR && key is DeclarationMatchKey.Function) key.parameters.map { emptyList() } else parameters.map(::annotations)))
    }

    private fun functionName(node: CjdSyntaxNode): String = name(node).ifEmpty {
        (node.child(OPERATION_NAME) ?: node.child(OPERATOR))?.let { operation ->
            leaves(operation).filterNot { it.kind == TokenType.WHITE_SPACE }.joinToString("") { text(it) }
        }.orEmpty()
    }

    private fun bindings(node: CjdSyntaxNode, inheritedType: TypeKey?): List<Pair<String, TypeKey?>> {
        val localType = node.child(TYPE_REFERENCE)?.let(::type) ?: inheritedType
        return when (node.kind) {
            BINDING_PATTERN, TYPE_PATTERN -> listOf(name(node) to localType)
            VAR_OR_ENUM_PATTERN -> {
                val children = node.children.filter { it.kind in patternKinds }
                if (children.isEmpty()) listOf(name(node) to localType) else children.flatMap { bindings(it, localType) }
            }
            TUPLE_PATTERN -> node.children.filter { it.kind in patternKinds }.flatMapIndexed { index, child ->
                bindings(child, (localType as? TypeKey.Tuple)?.elements?.getOrNull(index))
            }
            ENUM_PATTERN -> node.children.filter { it.kind in patternKinds }.flatMap { bindings(it, null) }
            WILDCARD_PATTERN -> emptyList()
            else -> {
                diagnostic(node, CjdDiagnosticKind.UNSUPPORTED_DECLARATION, "Unsupported binding pattern: ${node.kind}")
                emptyList()
            }
        }
    }

    private fun generic(node: CjdSyntaxNode): CjdGenericSignature? {
        val list = node.child(TYPE_PARAMETER_LIST) ?: return null
        return CjdGenericSignature(list.children(TYPE_PARAMETER).map(::name),
            node.child(TYPE_CONSTRAINT_LIST)?.children(TYPE_CONSTRAINT).orEmpty().map {
                CjdTypeConstraint(TypeKey.Ref(it.child(REFERENCE_EXPRESSION)?.let(::identifier).orEmpty()), it.children(TYPE_REFERENCE).map(::type))
            })
    }

    private fun annotations(node: CjdSyntaxNode): List<CjdAnnotation> {
        fun collect(current: CjdSyntaxNode): List<CjdSyntaxNode> = current.children.flatMap {
            when (it.kind) {
                ANNOTATION -> listOf(it)
                ANNOTATIONS, MODIFIER_LIST -> collect(it)
                else -> emptyList()
            }
        }
        return collect(node).map { annotation ->
            val leaves = leaves(annotation)
            val marker = leaves.firstOrNull { it.kind == CjTokens.AT || it.kind == CjTokens.ATEXCL }
            val argumentList = annotation.child(VALUE_ARGUMENT_LIST)
            val nameLeaves = leaves.dropWhile { it != marker }.drop(1).takeWhile { it.kind != CjTokens.LBRACKET }
            val annotationName = nameLeaves.filter { it.kind == CjTokens.IDENTIFIER || it.kind == CjTokens.DOT }.joinToString("") { identifier(it) }
            val bracket = leaves.firstOrNull { it.kind == CjTokens.LBRACKET }
            CjdAnnotation(annotationName, marker?.kind == CjTokens.ATEXCL, text(annotation), annotation.range,
                bracket?.let { source.substring(it.start, annotation.end) },
                argumentList?.children(VALUE_ARGUMENT).orEmpty().map { arg ->
                    val argumentName = arg.child(VALUE_ARGUMENT_NAME)
                    val significant = arg.children.filterNot { it.kind == TokenType.WHITE_SPACE || it.kind == VALUE_ARGUMENT_NAME || it.kind == CjTokens.COLON || it.kind == CjTokens.EQ }
                    val expressionText = if (significant.isEmpty()) "" else source.substring(significant.first().start, significant.last().end)
                    val expression = significant.singleOrNull()?.let(::annotationExpression)
                        ?: CjdAnnotationExpression("UNKNOWN", expressionText, arg.range, significant.map(::annotationExpression))
                    CjdAnnotationArgument(argumentName?.let { name(it).ifEmpty { identifier(it) } }, expressionText, text(arg), arg.range, expression)
                })
        }
    }

    private fun leaves(node: CjdSyntaxNode): List<CjdSyntaxNode> = if (node.children.isEmpty()) listOf(node) else node.children.flatMap(::leaves)

    /**
     * 将 annotation 实参表达式转换为语法形状快照；只保留 parser 确定的节点类型，
     * 不做求值、名字解析或字符串转义之外的解释。
     */
    private fun annotationExpression(node: CjdSyntaxNode): CjdAnnotationExpression {
        return CjdAnnotationExpression(node.kind.toString(), text(node), node.range,
            node.children.filterNot { it.kind == TokenType.WHITE_SPACE || CjTokens.COMMENTS.contains(it.kind) }.map(::annotationExpression))
    }

    private fun type(node: CjdSyntaxNode): TypeKey = when (node.kind) {
        TYPE_REFERENCE, TYPE_PROJECTION, PARENTHESIZED_TYPE -> node.children.firstOrNull { it.kind in typeKinds }?.let(::type) ?: opaque(node)
        BASIC_TYPE -> TypeKey.primitive(text(node).trim())
        USER_TYPE -> {
            val arguments = node.child(TYPE_ARGUMENT_LIST)?.children(TYPE_PROJECTION).orEmpty().map(::type)
            val base = node.child(USER_TYPE)
            if (base == null) TypeKey.Ref(name(node), arguments) else TypeKey.Qualified(type(base), name(node), arguments)
        }
        OPTIONAL_TYPE -> TypeKey.Option(node.children.firstOrNull { it.kind in typeKinds }?.let(::type) ?: opaque(node))
        TUPLE_TYPE -> TypeKey.Tuple(node.children(TYPE_REFERENCE).map(::type))
        FUNCTION_TYPE -> TypeKey.Function(
            node.child(VALUE_PARAMETER_LIST)?.children(VALUE_PARAMETER).orEmpty().map { it.child(TYPE_REFERENCE)?.let(::type) ?: opaque(it) },
            node.child(TYPE_REFERENCE)?.let(::type) ?: opaque(node))
        VARRAY_TYPE -> TypeKey.VArray(
            node.child(TYPE_ARGUMENT_LIST)?.children(TYPE_PROJECTION)?.singleOrNull()?.let(::type) ?: opaque(node),
            TypeKey.Constant("INTEGER", node.child(CjTokens.INTEGER_LITERAL)?.let(::text).orEmpty()))
        else -> opaque(node)
    }

    private fun opaque(node: CjdSyntaxNode): TypeKey.Opaque {
        diagnostic(node, CjdDiagnosticKind.UNSUPPORTED_TYPE, "Opaque type: ${node.kind}")
        return TypeKey.Opaque(node.kind.toString(), text(node))
    }

    companion object {
        private val declarationKinds: Map<IElementType, CjdDeclarationKind> = mapOf(
            FUNC to CjdDeclarationKind.FUNCTION, PRIMARY_CONSTRUCTOR to CjdDeclarationKind.FUNCTION,
            SECONDARY_CONSTRUCTOR to CjdDeclarationKind.FUNCTION, FIELD to CjdDeclarationKind.VARIABLE, VARIABLE to CjdDeclarationKind.VARIABLE,
            PROPERTY to CjdDeclarationKind.PROPERTY, CLASS to CjdDeclarationKind.CLASS, STRUCT to CjdDeclarationKind.STRUCT,
            INTERFACE to CjdDeclarationKind.INTERFACE, ENUM to CjdDeclarationKind.ENUM, TYPEALIAS to CjdDeclarationKind.TYPE_ALIAS,
            EXTEND to CjdDeclarationKind.EXTEND, ENUM_CONSTRUCTOR to CjdDeclarationKind.FUNCTION,
            MACRO to CjdDeclarationKind.MACRO, MAIN_FUNC to CjdDeclarationKind.MAIN, FINALIZER to CjdDeclarationKind.FINALIZER)
        private val bodyKinds = setOf(CLASS_BODY, INTERFACE_BODY, ENUM_BODY)
        private val patternKinds = setOf(BINDING_PATTERN, VAR_OR_ENUM_PATTERN, TUPLE_PATTERN, ENUM_PATTERN, WILDCARD_PATTERN, TYPE_PATTERN)
        private val typeKinds = setOf(TYPE_REFERENCE, TYPE_PROJECTION, PARENTHESIZED_TYPE, BASIC_TYPE, USER_TYPE, OPTIONAL_TYPE, TUPLE_TYPE, FUNCTION_TYPE, VARRAY_TYPE, THIS_TYPE)
    }
}
