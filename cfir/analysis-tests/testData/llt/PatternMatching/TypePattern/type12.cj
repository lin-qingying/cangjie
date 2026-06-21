

main() {
    match ((1, 1)) {
        case (x: Int64, <!REDECLARATION!>x<!>: Int64) => ()
        case (_, _) => ()
    }
}
