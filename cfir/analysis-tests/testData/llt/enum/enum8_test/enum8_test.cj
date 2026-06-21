


internal import pkg.*

enum A {
    a
}

main() {
    let b: Unit1
    a
    b = Unit1.u1_3
    a
    let c: Unit1
    a
    c = Unit1.u1_5
    a

    let ret1 = match (b) {
        case u1_3 => 1
        case _ => 11
    }
    let ret2 = match (c) {
        case u1_5 => 2
        case _ => 22
    }
    if (ret1 == 1 && ret2 == 2) {
        return 0
    }
    return 1
}
