func invokeNow(value: Int64, block: (Int64) -> Unit) {
    block(value)
}

func applyTwice(block: (Int64) -> Unit, value: Int64) {
    block(value)
    block(value + 1)
}

func makePrinter(offset: Int64): (Int64) -> Unit {
    return { value =>
        value + offset
    }
}

func useHigherOrderFunctions() {
    let local: (Int64) -> Unit = { value =>
        value + 1
    }
    local(1)

    applyTwice({ value =>
        value + 2
    }, 2)

    invokeNow(3) { value =>
        value + 3
    }

    makePrinter(4)(5)
}
