

func f(x: Bool) {
    println(x)
    let <!REDECLARATION!>x<!> = 0
    println(x)
}

main() {
    f(false)
}
