/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.macro

/**
 * CangJieLexer 与 CJC 的 TokenKind 使用不同编号和部分不同名称；宏协议必须发送 CJC 编号。
 *
 * 名称顺序与 CJC `include/cangjie/Lex/Tokens.inc` 一致。IntelliJ lexer 的名字经显式映射后，
 * 才能写入 MacroMsg 的 Token.kind。普通空格不属于宏 token；含换行的空白折为 CJC 的 NL。
 */
object MacroTokenKind {
    private val cjcTokenNames = """
        DOT COMMA LPAREN RPAREN LSQUARE RSQUARE LCURL RCURL EXP MUL MOD DIV ADD SUB INCR DECR
        AND OR COALESCING PIPELINE COMPOSITION NOT BITAND BITOR BITXOR BITNOT LSHIFT RSHIFT
        COLON SEMI ASSIGN ADD_ASSIGN SUB_ASSIGN MUL_ASSIGN EXP_ASSIGN DIV_ASSIGN MOD_ASSIGN
        AND_ASSIGN OR_ASSIGN BITAND_ASSIGN BITOR_ASSIGN BITXOR_ASSIGN LSHIFT_ASSIGN RSHIFT_ASSIGN
        ARROW BACKARROW DOUBLE_ARROW RANGEOP CLOSEDRANGEOP ELLIPSIS HASH AT QUEST LT GT LE GE IS AS
        NOTEQ EQUAL WILDCARD INT8 INT16 INT32 INT64 INTNATIVE UINT8 UINT16 UINT32 UINT64 UINTNATIVE
        FLOAT16 FLOAT32 FLOAT64 RUNE BOOLEAN NOTHING UNIT STRUCT ENUM VARRAY THISTYPE PACKAGE IMPORT
        CLASS INTERFACE FUNC MACRO QUOTE DOLLAR LET VAR CONST TYPE INIT THIS SUPER IF ELSE CASE TRY CATCH
        FINALLY FOR DO WHILE THROW RETURN CONTINUE BREAK IN NOT_IN MATCH WHERE EXTEND WITH PROP STATIC
        PUBLIC PRIVATE INTERNAL PROTECTED OVERRIDE REDEF ABSTRACT SEALED OPEN FOREIGN INOUT MUT UNSAFE
        OPERATOR SPAWN SYNCHRONIZED UPPERBOUND MAIN IDENTIFIER PACKAGE_IDENTIFIER INTEGER_LITERAL
        RUNE_BYTE_LITERAL FLOAT_LITERAL COMMENT NL END SENTINEL RUNE_LITERAL STRING_LITERAL JSTRING_LITERAL
        MULTILINE_STRING MULTILINE_RAW_STRING BOOL_LITERAL UNIT_LITERAL DOLLAR_IDENTIFIER ANNOTATION AT_EXCL
        ILLEGAL
    """.trimIndent().split(Regex("\\s+"))

    private val cjcKindByName = cjcTokenNames.withIndex().associate { (index, name) -> name to index.toUByte() }

    private val aliases = mapOf(
        "ANDAND" to "AND",
        "ANDANDEQ" to "AND_ASSIGN",
        "ANDEQ" to "BITAND_ASSIGN",
        "ATEXCL" to "AT_EXCL",
        "BLOCK_COMMENT" to "COMMENT",
        "BOOL_KEYWORD" to "BOOLEAN",
        "CHARACTER_BYTE_LITERAL" to "RUNE_BYTE_LITERAL",
        "DANGLING_NEWLINE" to "NL",
        "DIVEQ" to "DIV_ASSIGN",
        "EOL_COMMENT" to "COMMENT",
        "EOL_OR_SEMICOLON" to "NL",
        "EOF" to "END",
        "EQ" to "ASSIGN",
        "EQEQ" to "EQUAL",
        "EXCL" to "NOT",
        "EXCLEQ" to "NOTEQ",
        "FIELD_IDENTIFIER" to "DOLLAR_IDENTIFIER",
        "FALSE_KEYWORD" to "BOOL_LITERAL",
        "GTEQ" to "GE",
        "GTGT" to "RSHIFT",
        "GTGTEQ" to "RSHIFT_ASSIGN",
        "LEFT_ARROW" to "BACKARROW",
        "LBRACE" to "LCURL",
        "LBRACKET" to "LSQUARE",
        "LPAR" to "LPAREN",
        "LTEQ" to "LE",
        "LTLT" to "LSHIFT",
        "LTLTEQ" to "LSHIFT_ASSIGN",
        "MINUS" to "SUB",
        "MINUSEQ" to "SUB_ASSIGN",
        "MINUSMINUS" to "DECR",
        "MULMUL" to "EXP",
        "MULMULEQ" to "EXP_ASSIGN",
        "MULTEQ" to "MUL_ASSIGN",
        "OPERATION_AND" to "BITAND",
        "OPERATION_COMPARE_GT" to "GT",
        "OPERATION_COMPARE_GTEQ" to "GE",
        "OPERATION_COMPARE_LT" to "LT",
        "OPERATION_COMPARE_LTEQ" to "LE",
        "OPERATION_DIV" to "DIV",
        "OPERATION_EQUALS" to "EQUAL",
        "OPERATION_EXPONENTIATION" to "EXP",
        "OPERATION_GET" to "IDENTIFIER",
        "OPERATION_INVOKE" to "IDENTIFIER",
        "OPERATION_LEFT_SHIFT" to "LSHIFT",
        "OPERATION_MINUS" to "SUB",
        "OPERATION_NOT" to "NOT",
        "OPERATION_NOT_EQUALS" to "NOTEQ",
        "OPERATION_OR" to "OR",
        "OPERATION_PLUS" to "ADD",
        "OPERATION_REM" to "MOD",
        "OPERATION_RIGHT_SHIFT" to "RSHIFT",
        "OPERATION_TIMES" to "MUL",
        "OPERATION_XOR" to "BITXOR",
        "OREQ" to "BITOR_ASSIGN",
        "OROR" to "OR",
        "OROREQ" to "OR_ASSIGN",
        "PERC" to "MOD",
        "PERCEQ" to "MOD_ASSIGN",
        "PLUS" to "ADD",
        "PLUSEQ" to "ADD_ASSIGN",
        "PLUSPLUS" to "INCR",
        "RANGE" to "RANGEOP",
        "RANGEEQ" to "CLOSEDRANGEOP",
        "RBRACE" to "RCURL",
        "RBRACKET" to "RSQUARE",
        "RPAR" to "RPAREN",
        "RUNE_KEYWORD" to "RUNE",
        "SEMICOLON" to "SEMI",
        "SHEBANG_COMMENT" to "COMMENT",
        "THIS_KEYWORD_UPPER" to "THISTYPE",
        "TILDE" to "BITNOT",
        "TRUE_KEYWORD" to "BOOL_LITERAL",
        "UNDERLINE" to "WILDCARD",
        "XOR" to "BITXOR",
        "XOREQ" to "BITXOR_ASSIGN",
    )

    private val keywordTextAliases = mapOf(
        "Bool" to "BOOLEAN",
        "This" to "THISTYPE",
        "false" to "BOOL_LITERAL",
        "true" to "BOOL_LITERAL",
    )

    /**
     * 将仓颉 IntelliJ lexer token 编码为 CJC 宏协议 token；普通空白返回 null。
     * 未支持的 lexer token 明确失败，避免以错误 kind 静默损坏 AST。
     */
    fun fromLexerToken(kindName: String?, text: String): EncodedMacroToken? {
        requireNotNull(kindName) { "Macro token is missing its CangJieLexer kind: `$text`." }
        if (kindName == "WHITE_SPACE") {
            return if ('\n' in text || '\r' in text) {
                EncodedMacroToken(cjcKindByName.getValue("NL"), "\n")
            } else {
                null
            }
        }
        if (kindName == "BAD_CHARACTER" && text == "#") {
            return EncodedMacroToken(cjcKindByName.getValue("HASH"), text)
        }

        val directName = aliases[kindName] ?: kindName
        val cjcName = sequenceOf(
            directName,
            directName.removeSuffix("_KEYWORD"),
            keywordTextAliases[kindName],
            kindName.uppercase(),
        ).filterNotNull().firstOrNull(cjcKindByName::containsKey)
        val kind = cjcKindByName[cjcName]
            ?: error("CangJieLexer token `$kindName` has no CJC TokenKind mapping (text=`$text`).")
        return EncodedMacroToken(kind, text)
    }

    /** 已映射到 CJC TokenKind 的 token。 */
    data class EncodedMacroToken(
        /** CJC `TokenKind` 的协议编号。 */
        val kind: UByte,
        /** CJC token 的原始或规范化文本。 */
        val value: String,
    )
}
