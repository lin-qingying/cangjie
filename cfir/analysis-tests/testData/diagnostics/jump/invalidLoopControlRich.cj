func runBlock(block: (Int64) -> Unit) {
    block(0)
}

func breakOutsideLoop(): Unit {
    <!INVALID_LOOP_CONTROL!>break<!>
}

func continueOutsideLoop(): Unit {
    <!INVALID_LOOP_CONTROL!>continue<!>
}

func validLoopControl(flag: Bool): Unit {
    while (flag) {
        if (flag) {
            break
        }
        continue
    }
}

func jumpAcrossLambdaBoundary(flag: Bool): Unit {
    while (flag) {
        runBlock({ value =>
            <!INVALID_LOOP_CONTROL!>break<!>
        })
        runBlock({ value =>
            <!INVALID_LOOP_CONTROL!>continue<!>
        })
        break
    }
}
