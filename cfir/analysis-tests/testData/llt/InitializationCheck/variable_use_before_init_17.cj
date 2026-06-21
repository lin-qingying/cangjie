

func f() {
    var x: Int64
    while (true) {
        <!USED_BEFORE_INITIALIZATION!>x<!>++
    }
}

main() {}
