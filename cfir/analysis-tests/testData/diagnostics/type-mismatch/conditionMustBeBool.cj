
func badIfCondition(): Int64 {
    if (<!TYPE_MISMATCH!>1<!>) {
        return 1
    } else {
        return 0
    }
}

func badWhileCondition(): Unit {
    var i = 0
    while (<!TYPE_MISMATCH!>i<!>) {
        i = i + 1
        break
    }
}

func badDoWhileCondition(): Unit {
    var i = 0
    do {
        i = i + 1
    } while (<!TYPE_MISMATCH!>i<!>)
}

