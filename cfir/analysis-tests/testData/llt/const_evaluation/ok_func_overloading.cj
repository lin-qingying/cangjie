


func f() {
    0
}

func f(x: Int64) {
    x
}

const func f(x: Bool) {
    x
}

main() {
    if (f(true)) {
        f()
    } else {
        f(1)
    }
}
