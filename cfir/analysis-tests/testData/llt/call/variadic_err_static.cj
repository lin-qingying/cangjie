


class A<T> {
    init(x: Bool, a: Array<T>) {}
    static func f(x: Array<T>) {
        x.size
    }
}

main() {
    let x = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>A<!><Int64>(true).f() == 0
}
