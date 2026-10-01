// RUN_PIPELINE_TILL: FRONTEND
func f(b: Bool): (Int64, String) {
    if (b) {
        return (1, "a")
    }
    (2, "b")
}
