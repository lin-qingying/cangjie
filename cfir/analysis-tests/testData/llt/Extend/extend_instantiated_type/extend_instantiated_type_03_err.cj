


class X {}

class Y {}

class A<T, K> where K <: ToString {}

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend<T> A<Int64, T> <: ToString {
    func test() {
        println("A int64")
    }
}

extend A<Int64, Int64> {
    public func toString() { // invisible for extend ToString
        "A<Int64, Int64>"
    }
}

main() {
    let a1 = A<Int64, Rune>()
    let t: ToString = a1
    if (t.toString() != "A<Int64, Int64>") {
        return 1
    }
    return 0
}
