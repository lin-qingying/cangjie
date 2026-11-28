// RUN_PIPELINE_TILL: FRONTEND
func classify(x: Int64): Int64 {
    let y = if (x > 10) {
        2
    } else if (x > 5) {
        1
    } else {
        0
    }
    return y
}
