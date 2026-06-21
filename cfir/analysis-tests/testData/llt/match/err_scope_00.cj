

main() {
    match (0) {
        case x =>
            let <!REDECLARATION!>x<!> = 1
            println(x)
    }
}
