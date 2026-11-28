package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.name.CangJieUnicode
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.source.CjSourceElement

/** 官方 MangleName 计算规范化后 UTF-8 字节数。 */
internal fun cjmpRawMangleName(value: String): String {
    val normalized = CangJieUnicode.normalizeIdentifier(value.removeSurrounding("`"))
    return normalized.toByteArray(Charsets.UTF_8).size.toString() + normalized
}

/** 官方 std::string 以字节序排序编码，而不是按 JVM UTF-16 字符排序。 */
internal val cjmpUtf8Order: Comparator<String> = Comparator { left, right ->
    val a = left.toByteArray(Charsets.UTF_8)
    val b = right.toByteArray(Charsets.UTF_8)
    for (index in 0 until minOf(a.size, b.size)) {
        val difference = (a[index].toInt() and 0xff) - (b[index].toInt() and 0xff)
        if (difference != 0) return@Comparator difference
    }
    a.size - b.size
}

/** 官方 ASTMangler 使用原始类型语法；解析后的类型不能还原括号、别名及限定名。 */
internal object CfirCjmpSourceTypeMangler {
    /** where 左侧也是源码类型引用；与普通类型名共用原始长度和 NFC 内容的编码。 */
    fun typeParameterReference(source: CjSourceElement): String? {
        val reference = snapshot(source.treeStructure, source.lighterASTNode)
        if (reference.type != CjNodeTypes.REFERENCE_EXPRESSION || reference.containsError()) return null
        return encodeReferenceName(reference)
    }

    /** extend 的目标类型与继承列表分别位于直接子节点和 SUPER_TYPE_LIST 中。 */
    fun extendAnnotations(source: CjSourceElement): List<String>? {
        val tree = source.treeStructure
        val ref = Ref<Array<LighterASTNode>>()
        val count = tree.getChildren(source.lighterASTNode, ref)
        val children = ref.get()
        try {
            var target: String? = null
            var inherited: List<String>? = null
            for (index in 0 until count) {
                val child = children[index]
                when (child.tokenType) {
                    CjNodeTypes.TYPE_REFERENCE -> {
                        if (target != null) return null
                        target = encode(snapshot(tree, child)) ?: return null
                    }
                    CjNodeTypes.SUPER_TYPE_LIST -> {
                        if (inherited != null) return null
                        val entries = snapshot(tree, child).children(CjNodeTypes.SUPER_TYPE_ENTRY)
                        if (entries.isEmpty()) return null
                        inherited = entries.map { entry ->
                            entry.child(CjNodeTypes.TYPE_REFERENCE)?.let(::encode) ?: return null
                        }
                    }
                }
            }
            return listOf(target ?: return null) + inherited.orEmpty()
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    /** 编码声明节点的直接类型注解；损坏的类型不产生可参与判重的名称。 */
    fun directAnnotations(source: CjSourceElement): List<String>? {
        val tree = source.treeStructure
        val ref = Ref<Array<LighterASTNode>>()
        val count = tree.getChildren(source.lighterASTNode, ref)
        val children = ref.get()
        try {
            return buildList {
                for (index in 0 until count) {
                    val child = children[index]
                    if (child.tokenType == CjNodeTypes.TYPE_REFERENCE) {
                        add(encode(snapshot(tree, child)) ?: return null)
                    }
                }
            }
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    private data class Syntax(val type: IElementType, val text: String, val children: List<Syntax>) {
        fun child(type: IElementType): Syntax? = children.singleOrNull { it.type == type }
        fun children(type: IElementType): List<Syntax> = children.filter { it.type == type }
        fun containsError(): Boolean = type == TokenType.ERROR_ELEMENT || children.any { it.containsError() }
    }

    private fun snapshot(tree: FlyweightCapableTreeStructure<LighterASTNode>, node: LighterASTNode): Syntax {
        val ref = Ref<Array<LighterASTNode>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        try {
            val nested = buildList {
                for (index in 0 until count) {
                    val child = children[index]
                    if (child.tokenType !in CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET) add(snapshot(tree, child))
                }
            }
            return Syntax(node.tokenType, if (count == 0) tree.toString(node).toString() else "", nested)
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    private val typeElements = setOf(
        CjNodeTypes.BASIC_TYPE, CjNodeTypes.USER_TYPE, CjNodeTypes.FUNCTION_TYPE, CjNodeTypes.OPTIONAL_TYPE,
        CjNodeTypes.TUPLE_TYPE, CjNodeTypes.PARENTHESIZED_TYPE, CjNodeTypes.VARRAY_TYPE, CjNodeTypes.THIS_TYPE,
    )

    private fun encode(node: Syntax): String? {
        if (node.containsError()) return null
        return when (node.type) {
            CjNodeTypes.TYPE_REFERENCE -> node.children.singleOrNull { it.type in typeElements }?.let(::encode)
            CjNodeTypes.BASIC_TYPE -> node.children.singleOrNull()?.text?.let(primitiveTypes::get)
            CjNodeTypes.THIS_TYPE -> "H"
            CjNodeTypes.PARENTHESIZED_TYPE -> node.child(CjNodeTypes.TYPE_REFERENCE)?.let(::encode)?.let { "P_$it" }
            CjNodeTypes.OPTIONAL_TYPE -> encodeOption(node)
            CjNodeTypes.TUPLE_TYPE -> encodeTuple(node)
            CjNodeTypes.FUNCTION_TYPE -> encodeFunction(node)
            CjNodeTypes.USER_TYPE -> encodeUserType(node)
            CjNodeTypes.VARRAY_TYPE -> encodeVArray(node)
            else -> null
        }
    }

    private fun encodeOption(node: Syntax): String? {
        var current = node
        var count = 0
        while (current.type == CjNodeTypes.OPTIONAL_TYPE) {
            if (current.children.count { it.type == CjTokens.QUEST } != 1) return null
            count++
            current = current.children.singleOrNull { it.type in typeElements } ?: return null
        }
        return encode(current)?.let { "O${count}_$it" }
    }

    private fun encodeTuple(node: Syntax): String? {
        val elements = node.children(CjNodeTypes.TYPE_REFERENCE)
        if (elements.size < 2) return null
        return "T${elements.size}_" + (encodeAll(elements) ?: return null)
    }

    private fun encodeFunction(node: Syntax): String? {
        if (node.children.count { it.type == CjTokens.ARROW } != 1) return null
        val parameters = node.child(CjNodeTypes.VALUE_PARAMETER_LIST) ?: return null
        val result = node.child(CjNodeTypes.TYPE_REFERENCE)?.let(::encode) ?: return null
        val types = parameters.children(CjNodeTypes.VALUE_PARAMETER).map { parameter ->
            parameter.child(CjNodeTypes.TYPE_REFERENCE) ?: return null
        }
        return "F" + result + (encodeAll(types) ?: return null)
    }

    private fun encodeUserType(node: Syntax): String? {
        val reference = node.child(CjNodeTypes.REFERENCE_EXPRESSION) ?: return null
        val segment = encodeReferenceName(reference) ?: return null
        val arguments = node.child(CjNodeTypes.TYPE_ARGUMENT_LIST)?.let { list ->
            val types = list.children(CjNodeTypes.TYPE_PROJECTION).map { projection ->
                projection.child(CjNodeTypes.TYPE_REFERENCE) ?: return null
            }
            if (types.isEmpty()) return null
            "<" + (encodeAll(types) ?: return null) + ">"
        }.orEmpty()
        val qualifier = node.child(CjNodeTypes.USER_TYPE)
        return if (qualifier == null) segment + arguments else "Q" + (encodeUserType(qualifier) ?: return null) + "." + segment + arguments
    }

    /** 反引号不属于名称范围；字节长度取自未归一化拼写，内容取自 NFC 名称。 */
    private fun encodeReferenceName(reference: Syntax): String? {
        val raw = reference.children.singleOrNull { it.type == CjTokens.IDENTIFIER }?.text ?: return null
        val spelling = raw.removeSurrounding("`")
        return spelling.toByteArray(Charsets.UTF_8).size.toString() + CangJieUnicode.normalizeIdentifier(spelling)
    }

    private fun encodeVArray(node: Syntax): String? {
        if (node.children.none { it.type == CjTokens.DOLLAR } || node.children.none { it.type == CjTokens.GT }) return null
        val argument = node.child(CjNodeTypes.TYPE_ARGUMENT_LIST)?.child(CjNodeTypes.TYPE_PROJECTION)
            ?.child(CjNodeTypes.TYPE_REFERENCE)?.let(::encode) ?: return null
        val size = node.child(CjTokens.INTEGER_LITERAL)?.text ?: return null
        return "V_${argument}\$$size"
    }

    private fun encodeAll(nodes: List<Syntax>): String? = buildString {
        for (node in nodes) append(encode(node) ?: return null)
    }

    private val primitiveTypes = mapOf(
        "Nothing" to "n", "Unit" to "u", "Rune" to "c", "Bool" to "b",
        "Float16" to "Dh", "Float32" to "f", "Float64" to "d",
        "Int8" to "a", "Int16" to "s", "Int32" to "i", "Int64" to "l", "IntNative" to "q",
        "UInt8" to "h", "UInt16" to "t", "UInt32" to "j", "UInt64" to "m", "UIntNative" to "r",
        "CString" to "CString", "CFunc" to "CFunc", "CPointer" to "CPointer",
    )
}
