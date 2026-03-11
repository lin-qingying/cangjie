func controlFlowTest(x: Int64): Int64 {
    if (x > 0) {
        return x
    } else {
        return -x
    }
}

func loopTest(): Unit {
    var i = 0
    while (i < 10) {
        i = i + 1
    }
}
