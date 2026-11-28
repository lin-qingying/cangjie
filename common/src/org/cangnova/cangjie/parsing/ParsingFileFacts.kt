package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.CjSourceKind

/** 共享 parser 确认的文件角色；SyntaxOnly 允许保留尚未验证的双角色事实。 */
enum class CjmpFileRole(val isCommon: Boolean, val isSpecific: Boolean) {
    NONE(false, false),
    COMMON(true, false),
    SPECIFIC(false, true),
    BOTH(true, true),
}

/** 一次文件解析发布的不可变文法事实，不从后续 CFIR status 或源码字符串重建。 */
data class ParsingFileFacts(
    val sourceKind: CjSourceKind,
    val cjmpRole: CjmpFileRole,
)
