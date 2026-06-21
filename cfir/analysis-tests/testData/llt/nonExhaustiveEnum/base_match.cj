

enum A {
    | B | C(Int64) | ...

    func foo() {
        1
    }
}

var a = B

main() {
    println(match (a) {
        case B => 0
        case C(_) => 1
        case _ => 2
    })
    println(match (C(4)) {
        case B => 0
        case C(a) where a > 3 => 1
        case _ => 2
    })
    println(match (C(2)) {
        case B => 0
        case C(a) where a > 3 => 1
        case _ => 2
    })
}
