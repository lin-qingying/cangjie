// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class NotException {}

func badThrow(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must have Exception or Error subtype. source: manual_source_zh_cn_handle; external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp; SynThrowExpr checks both Exception and Error */throw NotException()<!>
}
