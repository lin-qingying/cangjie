// RUN_PIPELINE_TILL: FRONTEND
func f(o: ?(?Int64)): Int64 {
    if (let Some(inner) <- o) {
        if (let Some(v) <- inner) {
            return v
        }
    }
    return 0
}
