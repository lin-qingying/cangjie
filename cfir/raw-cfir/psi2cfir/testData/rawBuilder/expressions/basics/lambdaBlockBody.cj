// RUN_PIPELINE_TILL: FRONTEND
func useLambda(): Int64 {
    let f = {
        =>
        let a = 1
        a + 1
    }
    return f()
}
