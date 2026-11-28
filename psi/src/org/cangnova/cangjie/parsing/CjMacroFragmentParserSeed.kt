package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.parsing.AbstractCangJieParsing.ParsingContext

/** fresh macro parser 保留原文件语义，语言模块为空，文法只由原调用的显式输入决定。 */
fun createMacroFragmentParserSeed(
    fileFacts: ParsingFileFacts,
    cjmpContext: CjmpParsingContext,
    entry: CjMacroFragmentContext,
    settings: CjParserSettings = CjParserSettings(),
): CjParserSeed = CjParserSeed(
    sourceKind = fileFacts.sourceKind,
    languageModuleName = "",
    cjmpContext = cjmpContext,
    fileRole = fileFacts.cjmpRole,
    quotedSyntaxDepth = 0,
    context = ParsingContext.ANNOTATION_ONLY.forMacroFragment(entry),
    macroFragment = entry,
    settings = settings,
)

internal fun ParsingContext.forMacroFragment(entry: CjMacroFragmentContext): ParsingContext {
    val scope = when (entry) {
        is CjMacroFragmentContext.Nodes -> (entry.scope as? MacroParsingScope.Declaration)?.kind
        is CjMacroFragmentContext.ParameterSlice, is CjMacroFragmentContext.Annotation -> null
    }
    val nominalKind = when (scope) {
        CjScopeKind.CLASS_BODY -> CjMemberParsingContext.Kind.CLASS
        CjScopeKind.STRUCT_BODY -> CjMemberParsingContext.Kind.STRUCT
        CjScopeKind.INTERFACE_BODY -> CjMemberParsingContext.Kind.INTERFACE
        CjScopeKind.ENUM_BODY, CjScopeKind.ENUM_CONSTRUCTOR -> CjMemberParsingContext.Kind.ENUM
        CjScopeKind.EXTEND_BODY -> CjMemberParsingContext.Kind.EXTEND
        else -> null
    }
    return copy(
        disableMacroParsing = true,
        enableCustomAnnotation = true,
        ignoreScopeCheck = true,
        memberScope = nominalKind?.let { CjMemberParsingContext(it, entry.outerDeclIdent) },
        // 官方 Parser.SetPrimaryDecl(string) 的 overload 固定使用 isRawId=false。
        primaryDeclaration = entry.outerDeclIdent?.let { CjPrimaryParsingName(it, false) },
        statementScope = scope ?: CjScopeKind.UNKNOWN_SCOPE,
    )
}
