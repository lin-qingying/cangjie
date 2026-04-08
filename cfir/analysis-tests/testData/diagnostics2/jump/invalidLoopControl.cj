// RUN_PIPELINE_TILL: FRONTEND
func breakOutsideLoop(): Unit {
    <!INVALID_LOOP_CONTROL!>break<!>
}

func continueOutsideLoop(): Unit {
    <!INVALID_LOOP_CONTROL!>continue<!>
}

func nestedInvalid(): Unit {
    while (true) {
        func localBreak(): Unit {
            <!INVALID_LOOP_CONTROL!>break<!>
        }
        let localContinue = { =>
            <!INVALID_LOOP_CONTROL!>continue<!>
        }
        localBreak()
        localContinue()
        break
    }
}
