// RUN_PIPELINE_TILL: FRONTEND
func useOption(o: ?Int64): Int64 {
    let a = Some(1)
    let b: Option<Int64> = None
    let c = o.getOrThrow()
    let d = o ?? 2
    let _ = b
    return a.getOrThrow() + c + d
}
