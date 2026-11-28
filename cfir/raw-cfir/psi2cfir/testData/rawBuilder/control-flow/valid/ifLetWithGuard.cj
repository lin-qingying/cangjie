// RUN_PIPELINE_TILL: FRONTEND
func firstOrZero(o: ?Int64, flag: Bool): Int64 {
    if (flag) {
        if (let Some(v) <- o) {
            return v
        }
    }
    return 0
}
