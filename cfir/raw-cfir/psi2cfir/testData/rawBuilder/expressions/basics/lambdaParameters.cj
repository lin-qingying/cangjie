// RUN_PIPELINE_TILL: FRONTEND
func useLambdas(): Int64 {
    let f: (Int64, Int64) -> Int64 = { a, b => a * b }
    let g = { a: Int64, b: Int64 => a + b }
    let h = { => 1 }
    return f(1, 2) + g(3, 4) + h()
}
