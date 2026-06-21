

func f() {
    var b: Int32
    do {
        if (true) {
            continue
        }
        b = 1
    } while (<!USED_BEFORE_INITIALIZATION!>b<!> == 0)
    <!USED_BEFORE_INITIALIZATION!>b<!>
}

main() {
    println(f())
}
