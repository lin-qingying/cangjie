


func bar() {
    while (false) {
        if (true) {
            1
        } else {
            1.0
        }
    }

    do {
        if (true) {
            1
        } else {
            1.0
        }
    } while (false)

    for (i in 1..100) {
        if (true) {
            1
        } else {
            1.0
        }
    }
}

func baz() {
    if (true) {
        if (true) {
            1
        } else {
            1.0
        }
    }
}

func qux() {
    try {
        1
    } catch (_) {
        2
    } finally {
        if (true) {
            1
        } else {
            1.0
        } // Discarded
    }
}

main() {
    bar()
    baz()
    qux()
    return 0
}
