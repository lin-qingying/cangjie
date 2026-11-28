// RUN_PIPELINE_TILL: FRONTEND
func f(arr: Array<Int64>): Int64 {
    var total = 0
    for (v in arr where v > 0) {
        total = total + v
    }
    return total
}
