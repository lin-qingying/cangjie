func runTwice(value: Int64, block: (Int64) -> Unit) {
    block(value)
    block(value + 1)
}

func jumpAcrossTrailingLambdaBoundary(flag: Bool): Unit {
    while (flag) {
        runTwice(0) { value =>
            <!INVALID_LOOP_CONTROL!>break<!>
        }
        runTwice(1) { value =>
            <!INVALID_LOOP_CONTROL!>continue<!>
        }
        break
    }
}
