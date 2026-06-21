

enum Color {
    Red | Blue | Green
}

func test(Green: Int32) {
    var a: Int32 = Green
    a
}

main() {
    var t = Green
    var res = match (t) {
        case Red => 2
        case Blue => 4
        case Green => 158
    }

    if (res != 158 || test(123) != 123) {
        return 1
    }

    return 0
}
