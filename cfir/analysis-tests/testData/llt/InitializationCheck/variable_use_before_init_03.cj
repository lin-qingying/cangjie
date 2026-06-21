

func f1() {
    var a: Int64
    var b: Int64
    if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
    }
    b - a - a
}

func f2() {
    var a: Int64
    var b: Int64
    if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
        a = 1
    }
    b - a - a
}

func f3() {
    var a: Int64
    var b: Int64
    if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
        b = 1
    }
    b - a - a
}

main() {
    f1()
    f2()
    f3()
}
