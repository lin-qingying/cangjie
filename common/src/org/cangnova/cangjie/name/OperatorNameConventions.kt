/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.name

/**
 * 仓颉操作符在编译器内部使用的约定名称集合。
 *
 * 解析、调用解析和诊断渲染通过这些稳定 [Name] 在源码符号与语义入口之间做映射。
 */
object OperatorNameConventions {
    /**
     * `contains` 约定函数名，用于成员包含性或区间包含语义。
     */
    @JvmField
    val CONTAINS = Name.identifier("contains")

    /**
     * 函数调用操作符 `()` 对应的内部名称。
     */
    @JvmField
    val INVOKE = Name.identifier("*operator_invoke")

    /**
     * 下标读取操作符 `[]` 对应的内部名称。
     */
    @JvmField
    val GET = Name.identifier("*operator_get")

    /**
     * 下标写入操作符 `[]=` 对应的内部名称。
     */
    @JvmField
    val SET = Name.identifier("*operator_set")

    /**
     * 逻辑非操作符 `!` 对应的内部名称。
     */
    @JvmField
    val NOT = Name.identifier("*operator_not")

    /**
     * 不等比较操作符 `!=` 对应的内部名称。
     */
    @JvmField
    val NOT_EQUALS = Name.identifier("*operator_not_equals")

    /**
     * 幂运算操作符 `**` 对应的内部名称。
     */
    @JvmField
    val EXPONENTIATION = Name.identifier("*operator_exponentiation")

    /**
     * 相等比较操作符 `==` 对应的内部名称。
     */
    @JvmField
    val EQUALS = Name.identifier("*operator_equals")

    /**
     * 乘法操作符 `*` 对应的内部名称。
     */
    @JvmField
    val TIMES = Name.identifier("*operator_times")

    /**
     * 除法操作符 `/` 对应的内部名称。
     */
    @JvmField
    val DIV = Name.identifier("*operator_div")

    /**
     * 取余操作符 `%` 对应的内部名称。
     */
    @JvmField
    val REM = Name.identifier("*operator_rem")

    /**
     * 减法操作符 `-` 对应的内部名称。
     */
    @JvmField
    val MINUS = Name.identifier("*operator_minus")

    /**
     * 加法操作符 `+` 对应的内部名称。
     */
    @JvmField
    val PLUS = Name.identifier("*operator_plus")

    /**
     * 左移操作符 `<<` 对应的内部名称。
     */
    @JvmField
    val LEFT_SHIFT = Name.identifier("*operator_left_shift")

    /**
     * 右移操作符 `>>` 对应的内部名称。
     */
    @JvmField
    val RIGHT_SHIFT = Name.identifier("*operator_right_shift")

    /**
     * 大于比较操作符 `>` 对应的内部名称。
     */
    @JvmField
    val COMPARE_GT = Name.identifier("*operator_compare_gt")

    /**
     * 小于等于比较操作符 `<=` 对应的内部名称。
     */
    @JvmField
    val COMPARE_LTEQ = Name.identifier("*operator_compare_lteq")

    /**
     * 小于比较操作符 `<` 对应的内部名称。
     */
    @JvmField
    val COMPARE_LT = Name.identifier("*operator_compare_lt")

    /**
     * 大于等于比较操作符 `>=` 对应的内部名称。
     */
    @JvmField
    val COMPARE_GTEQ = Name.identifier("*operator_compare_gteq")

    /**
     * 按位与操作符 `&` 对应的内部名称。
     */
    @JvmField
    val AND = Name.identifier("*operator_and")

    /**
     * 按位异或操作符 `^` 对应的内部名称。
     */
    @JvmField
    val XOR = Name.identifier("*operator_xor")

    /**
     * 按位或操作符 `|` 对应的内部名称。
     */
    @JvmField
    val OR = Name.identifier("*operator_or")

    /**
     * 乘法赋值操作符 `*=` 对应的内部名称。
     */
    @JvmField
    val TIMES_ASSIGN = Name.identifier("*operator_timesAssign")

    /**
     * 除法赋值操作符 `/=` 对应的内部名称。
     */
    @JvmField
    val DIV_ASSIGN = Name.identifier("*operator_divAssign")

    /**
     * 幂运算赋值操作符 `**=` 对应的内部名称。
     */
    @JvmField
    val EXPONENTIATION_ASSIGN = Name.identifier("*operator_exponentiationAssign")

    /**
     * 逻辑或赋值操作符 `||=` 对应的内部名称。
     */
    @JvmField
    val OROREQ_ASSIGN = Name.identifier("*operator_or2Assign")

    /**
     * 逻辑与赋值操作符 `&&=` 对应的内部名称。
     */
    @JvmField
    val ANDANDEQ_ASSIGN = Name.identifier("*operator_and2Assign")

    /**
     * 按位或赋值操作符 `|=` 对应的内部名称。
     */
    @JvmField
    val OREQ_ASSIGN = Name.identifier("*operator_orAssign")

    /**
     * 按位与赋值操作符 `&=` 对应的内部名称。
     */
    @JvmField
    val ANDEQ_ASSIGN = Name.identifier("*operator_andAssign")

    /**
     * 按位异或赋值操作符 `^=` 对应的内部名称。
     */
    @JvmField
    val XOREQ_ASSIGN = Name.identifier("*operator_xorAssign")

    /**
     * 右移赋值操作符 `>>=` 对应的内部名称。
     */
    @JvmField
    val GTGTEQ_ASSIGN = Name.identifier("*operator_rightShiftAssign")

    /**
     * 左移赋值操作符 `<<=` 对应的内部名称。
     */
    @JvmField
    val LTLTEQ_ASSIGN = Name.identifier("*operator_leftShiftAssign")

    /**
     * 管道操作符 `|>` 对应的内部名称。
     */
    @JvmField
    val PIPELINE = Name.identifier("*operator_pipeline")

    /**
     * 组合操作符 `~>` 对应的内部名称。
     */
    @JvmField
    val COMPOSITION = Name.identifier("*operator_composition")

    /**
     * 取余赋值操作符 `%=` 对应的内部名称。
     */
    @JvmField
    val REM_ASSIGN = Name.identifier("*operator_remAssign")

    /**
     * 加法赋值操作符 `+=` 对应的内部名称。
     */
    @JvmField
    val PLUS_ASSIGN = Name.identifier("*operator_plusAssign")

    /**
     * 减法赋值操作符 `-=` 对应的内部名称。
     */
    @JvmField
    val MINUS_ASSIGN = Name.identifier("*operator_minusAssign")

    /**
     * 短路逻辑与操作符 `&&` 对应的内部名称。
     */
    @JvmField
    val ANDAND = Name.identifier("*operator_and2")

    /**
     * 短路逻辑或操作符 `||` 对应的内部名称。
     */
    @JvmField
    val OROR = Name.identifier("*operator_or2")

    /**
     * 自增操作符 `++` 对应的内部名称。
     */
    @JvmField
    val INC = Name.identifier("*operator_inc")

    /**
     * 自减操作符 `--` 对应的内部名称。
     */
    @JvmField
    val DEC = Name.identifier("*operator_dec")

    /**
     * 一元负号操作符 `-x` 对应的内部名称。
     */
    @JvmField
    val UNARY_MINUS = Name.identifier("*operator_unaryMinus")

    /**
     * 一元正号操作符 `+x` 对应的内部名称。
     */
    @JvmField
    val UNARY_PLUS = Name.identifier("*operator_unaryPlus")

    /**
     * 迭代协议使用的 `iterator` 约定函数名。
     */
    @JvmField
    val ITERATOR = Name.identifier("iterator")

    /**
     * 从内部操作符名称到源码 token 文本的稳定映射。
     */
    @JvmField
    val TOKENS_BY_OPERATOR_NAME: Map<Name, String> = mapOf(
        INVOKE to "()",
        GET to "[]",
        SET to "[]",
        NOT to "!",
        NOT_EQUALS to "!=",
        EXPONENTIATION to "**",
        EQUALS to "==",
        TIMES to "*",
        DIV to "/",
        REM to "%",
        MINUS to "-",
        PLUS to "+",
        LEFT_SHIFT to "<<",
        RIGHT_SHIFT to ">>",
        COMPARE_GT to ">",
        COMPARE_LTEQ to "<=",
        COMPARE_LT to "<",
        COMPARE_GTEQ to ">=",
        AND to "&",
        XOR to "^",
        OR to "|",
        TIMES_ASSIGN to "*=",
        DIV_ASSIGN to "/=",
        EXPONENTIATION_ASSIGN to "**=",
        OROREQ_ASSIGN to "||=",
        ANDANDEQ_ASSIGN to "&&=",
        OREQ_ASSIGN to "|=",
        ANDEQ_ASSIGN to "&=",
        XOREQ_ASSIGN to "^=",
        GTGTEQ_ASSIGN to ">>=",
        LTLTEQ_ASSIGN to "<<=",
        PIPELINE to "|>",
        COMPOSITION to "~>",
        REM_ASSIGN to "%=",
        PLUS_ASSIGN to "+=",
        MINUS_ASSIGN to "-=",
        ANDAND to "&&",
        OROR to "||",
        INC to "++",
        DEC to "--",
        UNARY_MINUS to "-",
        UNARY_PLUS to "+",
    )

    /**
     * 将内部操作符名称还原为源码操作符文本；非操作符名称返回自身字符串。
     */
    fun Name.asOperatorString(): String {
        return when (this) {
            INVOKE -> "()"
            GET, SET -> "[]"
            NOT -> "!"
            NOT_EQUALS -> "!="
            EXPONENTIATION -> "**"
            EQUALS -> "=="
            TIMES -> "*"
            DIV -> "/"
            REM -> "%"
            MINUS -> "-"
            PLUS -> "+"
            LEFT_SHIFT -> "<<"
            RIGHT_SHIFT -> ">>"
            COMPARE_GT -> ">"
            COMPARE_LTEQ -> "<="
            COMPARE_LT -> "<"
            COMPARE_GTEQ -> ">="
            AND -> "&"
            XOR -> "^"
            OR -> "|"
            TIMES_ASSIGN -> "*="
            DIV_ASSIGN -> "/="
            EXPONENTIATION_ASSIGN -> "**="
            REM_ASSIGN -> "%="
            PLUS_ASSIGN -> "+="
            MINUS_ASSIGN -> "-="
            ANDAND -> "&&"
            OROR -> "||"
            PIPELINE -> "|>"
            COMPOSITION -> "~>"
            else -> this.asString()
        }
    }

    /**
     * 将源码操作符文本转换为编译器内部操作符名称；未知文本按普通标识符处理。
     */
    fun String.asOperatorName(): Name {
        return when (this) {
            "()" -> INVOKE
            "[]" -> GET
            "!" -> NOT
            "!=" -> NOT_EQUALS
            "**" -> EXPONENTIATION
            "==" -> EQUALS
            "*" -> TIMES
            "/" -> DIV
            "%" -> REM
            "-" -> MINUS
            "+" -> PLUS
            "<<" -> LEFT_SHIFT
            ">>" -> RIGHT_SHIFT
            ">" -> COMPARE_GT
            "<=" -> COMPARE_LTEQ
            "<" -> COMPARE_LT
            ">=" -> COMPARE_GTEQ
            "&" -> AND
            "^" -> XOR
            "|" -> OR
            "*=" -> TIMES_ASSIGN
            "/=" -> DIV_ASSIGN
            "**=" -> EXPONENTIATION_ASSIGN
            "%=" -> REM_ASSIGN
            "+=" -> PLUS_ASSIGN
            "-=" -> MINUS_ASSIGN
            "&&" -> ANDAND
            "||" -> OROR
            "|>" -> PIPELINE
            "~>" -> COMPOSITION
            "++" -> INC
            "--" -> DEC
            else -> Name.identifier(this)
        }
    }
}
