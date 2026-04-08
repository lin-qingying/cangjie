// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class DerivedError <: Error {}

func fail(): Nothing {
    throw DerivedError()
}

func legalThrowFromNothing(): Nothing {
    throw DerivedError()
}

func illegalThrowFromNothing(): Nothing {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must have Exception or Error subtype even in Nothing-returning contexts. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp */throw 42<!>
}
