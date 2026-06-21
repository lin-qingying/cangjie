

let (c0, c1) = (f0(), f1())

func f0(): Int64 {
    <!USED_BEFORE_INITIALIZATION!>c1<!>
}

func f1(): Int64 {
    c0
}

main() {
    println("${c0} ${c1}")
}
