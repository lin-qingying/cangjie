// RUN_PIPELINE_TILL: FRONTEND
func nestedWithGuards(values: Array<Int64>): Int64 {
    var total = 0
    for (v in values) {
        for (w in values) {
            if (w == 0) {
                continue
            }
            total = total + v * w
        }
    }
    return total
}

func loopWithLet(values: Array<?Int64>): Int64 {
    var count = 0
    for (v in values) {
        if (let Some(x) <- v) {
            count = count + x
        }
    }
    return count
}
