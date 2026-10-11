// RUN_PIPELINE_TILL: FRONTEND
func useForTuple(arr: Array<(Int64, String)>): Unit {
    for ((a, b) in arr) {
        let _ = a
        let _ = b
    }
}
