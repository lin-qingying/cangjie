// RUN_PIPELINE_TILL: FRONTEND
func apply(f: CFunc<(value: Int64) -> Int32>, x: Int64): Int32 {
    return unsafe { f(x) }
}
