// RUN_PIPELINE_TILL: FRONTEND
enum Token {
    A
}

func throwInt(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> 1
}

func throwEnumValue(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> Token.A
}
