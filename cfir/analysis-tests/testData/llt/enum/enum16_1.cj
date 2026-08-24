


class A2 {}

enum A {
    A1(Int32) | A2
}

enum AA {
    A1 | A3 | A4 | A5
}

func A1() {}

let A3 = 1

main(): Int64 {
    var a = A1
    var A2 = <!REF_NOT_BE_TYPE!>A<!>2
    var b = match (A2) {
        case <!PATTERN_NOT_MATCH!>A1(x)<!> => 0
        case _ => -1
    }
    let A4 = 1
    var a1 = AA.A1
    var c = match (a1) {
        case A3 => 0
        case _ => -1
    }
    var aa = AA.A1
    var d = match (aa) {
        case A4 => 0
        case _ => -1
    }
    let A5 = AA.A1
    var e = match (aa) {
        case A5 => 0
        case _ => -1
    }

    return 0
}
