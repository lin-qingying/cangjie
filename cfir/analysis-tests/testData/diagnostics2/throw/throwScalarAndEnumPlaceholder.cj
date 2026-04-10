// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
enum Token {
    A
}

func throwInt(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must have Exception or Error subtype; scalar values are not throwable. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp; SynThrowExpr checks both Exception and Error */throw 1<!>
}

func throwEnumValue(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must have Exception or Error subtype; enum values are not throwable. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp; SynThrowExpr checks both Exception and Error */throw Token.A<!>
}
