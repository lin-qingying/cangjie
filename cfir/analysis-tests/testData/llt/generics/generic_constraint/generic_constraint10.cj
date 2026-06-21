


interface A<T> {
    static func test(a: String): T
}

extend Int32 <: A<Int32> {
    public static func test(a: String): Int32 {
        return 123
    }
}

func getNumber<T>(data: String): T where T <: A<T> {
    test(T.test(data))
}

func test<T>(a: T): T where T <: A<T> {
    a
}

main() {
    var v: Int32 = getNumber("321")
    if (v != 123) {
        return 1
    }
    return 0
}
