// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LambdaReturnTypeMismatchAsArgumentTypeMismatch

func applyFunc(f: (Int64) -> Int64, x: Int64): Int64 {
    return f(x)
}

main() {
    let result = applyFunc(<!ARGUMENT_TYPE_MISMATCH!>{ value => true }<!>, 10)
}
