package org.cangnova.cangjie.chir.cfir2chir

/**
 * CFIR 到 CHIR 转换阶段保留的源级语义操作名。
 *
 * 这些名字只属于转换模块，不能注册到 CHIR 树本体的 operation 集合中；
 * CHIR core 只承载通用 IR 结构，CFIR 专有语义由本模块写入 `ChirOtherExpression.operation` 字符串。
 */
internal enum class Cfir2ChirOperation(
    val canonicalName: String,
) {
    PHI("phi"),
    CFIR_BINARY_OP("cfir.binary_op"),
    CFIR_TYPE_CONVERSION("cfir.type_conversion"),
    CFIR_TYPE_OPERATOR("cfir.type_operator"),
    CFIR_ANNOTATION("cfir.annotation"),
    CFIR_ANNOTATION_CALL("cfir.annotation_call"),
    CFIR_ABSENT_BODY("cfir.absent_body"),
    CFIR_LET_PATTERN("cfir.let_pattern"),
    CFIR_PATTERN_EXTRACT("cfir.pattern_extract"),
    CFIR_MATCH("cfir.match"),
    CFIR_PATTERN_MATCH("cfir.pattern_match"),
    CFIR_ENUM_CONSTRUCTOR("cfir.enum_constructor"),
    CFIR_ARRAY_LITERAL("cfir.array_literal"),
    CFIR_TUPLE_LITERAL("cfir.tuple_literal"),
    CFIR_STRING_INTERPOLATION("cfir.string_interpolation"),
    CFIR_RANGE("cfir.range"),
    CFIR_SUBSCRIPT("cfir.subscript"),
    CFIR_OPTIONAL("cfir.optional"),
    CFIR_OPTIONAL_CHAIN("cfir.optional_chain"),
    CFIR_INOUT_ARGUMENT("cfir.inout_argument"),
    CFIR_QUALIFIED_ACCESS("cfir.qualified_access"),
    CFIR_MEMBER_ACCESS("cfir.member_access"),
    CFIR_MEMBER_ASSIGN("cfir.member_assign"),
    CFIR_CALL_WITH_RECEIVER("cfir.call_with_receiver"),
    CFIR_ANONYMOUS_FUNCTION("cfir.anonymous_function"),
    CFIR_LOCAL_DECLARATION("cfir.local_declaration"),
    CFIR_FOR_IN_ITERATOR("cfir.for_in_iterator"),
    CFIR_FOR_IN_HAS_NEXT("cfir.for_in_has_next"),
    CFIR_FOR_IN_NEXT("cfir.for_in_next"),
    CFIR_TRY("cfir.try"),
    CFIR_CATCH("cfir.catch"),
    CFIR_HANDLE("cfir.handle"),
    CFIR_PERFORM("cfir.perform"),
    CFIR_RESUME("cfir.resume"),
    CFIR_SPAWN("cfir.spawn"),
    CFIR_SYNCHRONIZED("cfir.synchronized"),
    CFIR_UNSAFE("cfir.unsafe"),
    CFIR_QUOTE("cfir.quote"),
    CFIR_SUPER_RECEIVER("cfir.super_receiver"),
    CFIR_THIS_RECEIVER("cfir.this_receiver"),
    CFIR_INACCESSIBLE_RECEIVER("cfir.inaccessible_receiver"),
    CFIR_SMART_CAST("cfir.smart_cast"),
}
