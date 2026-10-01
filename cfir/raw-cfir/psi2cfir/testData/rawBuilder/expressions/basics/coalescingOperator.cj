// RUN_PIPELINE_TILL: FRONTEND
func useCoalesce(a: ?Int64): Int64 {
    return a ?? 1
}
