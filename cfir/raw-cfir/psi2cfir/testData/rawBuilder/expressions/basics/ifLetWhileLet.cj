// RUN_PIPELINE_TILL: FRONTEND
func firstOrZero(o: ?Int64): Int64 {
    if (let Some(v) <- o) {
        return v
    }
    return 0
}

func drain(o: ?Int64): Int64 {
    var w = o
    var count = 0
    while (let Some(x) <- w) {
        let _ = x
        count = count + 1
        w = None
    }
    return count
}
