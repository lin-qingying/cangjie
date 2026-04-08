// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
enum Token {
    A
}

func throwInt(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: scalar values cannot be thrown unless they belong to the Exception/Error family. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp */throw 1<!>
}

func throwEnumValue(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: enum values are not throwable unless they match dedicated resource semantics. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp */throw Token.A<!>
}
