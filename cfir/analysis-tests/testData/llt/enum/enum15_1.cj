


enum Color {
    Red | Blue | Green
}

class Green {}

var Red: Int64 = 2

func test(Green: Int32) {
    var a: Int32 = Green
    a
}

main() {
    var t0 = <!REF_NOT_BE_TYPE!>G<!>reen
    var t1 = Red
    var t2 = Blue

    return 0
}
