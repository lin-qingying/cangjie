

enum E<T> {
    | e(E<T>)
    | e
}

func test<T>(i_: T) {
    var a: E<T> = e(e<T>)
    var i = e<T>
    var res = match (a) {
        case e(v) =>
            print("matched");
            i
        case <!UNREACHABLE_PATTERN!>e<!> =>
            print("fail");
            i
    }
    match (res) {
        case e => 1
        case <!UNREACHABLE_PATTERN!>e(_)<!> => 0
    }
}

main(): Int64 {
    test(1)
}
