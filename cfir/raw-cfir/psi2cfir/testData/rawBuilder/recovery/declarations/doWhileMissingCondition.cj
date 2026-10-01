// RUN_PIPELINE_TILL: FRONTEND
func doWhileWithoutCondition(): Unit {
    do {
        let a = 1
        let _ = a
    } while
}
