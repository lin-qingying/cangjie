// RUN_PIPELINE_TILL: FRONTEND
func iterateWithGuards(scoreList: Array<Int64>): Unit {
    for (item in scoreList where item > 50) {
        let high = item
    }

    for (_ in scoreList) {
        let seen = 1
    }
}
