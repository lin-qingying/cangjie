// RUN_PIPELINE_TILL: FRONTEND
func bump(): Int64 {
    var x = 1
    x++
    x--
    return x
}
