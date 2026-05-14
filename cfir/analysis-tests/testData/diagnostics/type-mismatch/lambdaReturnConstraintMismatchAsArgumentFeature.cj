// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LambdaReturnTypeMismatchAsArgumentTypeMismatch

func applyFunc(f: (Int64) -> Int64, x: Int64): Int64 {
    return f(x)
}

main(): Unit {
    let _ = applyFunc(<!TYPE_MISMATCH!>{ _ => true }<!>, 10)
}
