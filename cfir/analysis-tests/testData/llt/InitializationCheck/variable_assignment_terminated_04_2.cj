

func f(c: Bool) {
    let a: Int64
    if (c) {
        try {
            a = Some(1) ?? throw Exception()
            a = 2
            println(a)
        } catch (_) {
        }
    } else {
        a = 2
    }
    return <!USED_BEFORE_INITIALIZATION!>a<!>
}

main() {
    f(true) - 1
}
