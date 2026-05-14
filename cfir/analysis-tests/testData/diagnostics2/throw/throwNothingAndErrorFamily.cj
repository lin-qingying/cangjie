// RUN_PIPELINE_TILL: FRONTEND
func fail(error: Error): Nothing {
    throw error
}

func legalThrowFromNothing(error: Error): Nothing {
    throw error
}

func illegalThrowFromNothing(): Nothing {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> 42
}
