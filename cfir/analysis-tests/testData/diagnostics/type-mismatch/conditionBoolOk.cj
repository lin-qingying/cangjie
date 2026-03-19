// RUN_PIPELINE_TILL: FRONTEND
func ifConditionOk(x: Int64): Int64 {
    if (x > 0) {
        return x
    } else {
        return 0
    }
}

func whileConditionOk(): Unit {
    var i = 0
    while (i < 3) {
        i = i + 1
    }
}

func doWhileConditionOk(): Unit {
    var i = 0
    do {
        i = i + 1
    } while (i < 3)
}

