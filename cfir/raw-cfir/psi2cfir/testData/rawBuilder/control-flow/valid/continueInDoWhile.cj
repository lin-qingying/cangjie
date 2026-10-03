// RUN_PIPELINE_TILL: FRONTEND
func f(): Int64 {
    var i = 0
    do {
        i = i + 1
        if (i < 3) {
            continue
        }
    } while (i < 5)
    return i
}
