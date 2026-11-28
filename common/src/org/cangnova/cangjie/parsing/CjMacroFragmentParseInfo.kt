package org.cangnova.cangjie.parsing

import java.util.Collections

/** 文件角色检查与未执行检查必须保持独立。 */
enum class CjmpDeclarationRoleCheck { NOT_CHECKED, ACCEPTED, REJECTED }

/** ParseNodes 对真实 FuncArg 的 inout 更新与首声明前缀，随实际 contents 根一起提交。 */
class CjMacroFragmentParseInfo(
    inoutRootIndices: Set<Int>,
    val inheritedPrefixRootIndex: Int?,
    val hasInvalidRoot: Boolean,
    val inheritedPrefix: CjMacroDeclarationPrefix?,
    val inheritedPrefixRoleCheck: CjmpDeclarationRoleCheck?,
    /** 结果的原解析语境与片段进入时角色；二者不隐含目标 session 的权威性。 */
    val parsingContext: CjmpParsingContext?,
    val entryFileRole: CjmpFileRole?,
) {
    init {
        require((inheritedPrefixRootIndex == null) == (inheritedPrefix == null))
        require((inheritedPrefixRootIndex == null) == (inheritedPrefixRoleCheck == null))
        require((parsingContext == null) == (entryFileRole == null))
    }
    val inoutRootIndices: Set<Int> = Collections.unmodifiableSet(LinkedHashSet(inoutRootIndices))

    override fun equals(other: Any?): Boolean = other is CjMacroFragmentParseInfo &&
        inoutRootIndices == other.inoutRootIndices && inheritedPrefixRootIndex == other.inheritedPrefixRootIndex &&
        hasInvalidRoot == other.hasInvalidRoot && inheritedPrefix == other.inheritedPrefix &&
        inheritedPrefixRoleCheck == other.inheritedPrefixRoleCheck && parsingContext == other.parsingContext &&
        entryFileRole == other.entryFileRole

    override fun hashCode(): Int {
        var result = inoutRootIndices.hashCode()
        result = 31 * result + (inheritedPrefixRootIndex?.hashCode() ?: 0)
        result = 31 * result + hasInvalidRoot.hashCode()
        result = 31 * result + (inheritedPrefix?.hashCode() ?: 0)
        result = 31 * result + (inheritedPrefixRoleCheck?.hashCode() ?: 0)
        result = 31 * result + (parsingContext?.hashCode() ?: 0)
        return 31 * result + (entryFileRole?.hashCode() ?: 0)
    }
}
