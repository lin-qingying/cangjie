

interface I1<T1> {
    static func test(a: T1): T1 {
        return a
    }
}

interface I2<T2> <: I1<Bool> & I1<Int64> {
    static func test(a: T2): T2 {
        return a
    }
}

func test<K>(): Int64 where K <: I2<String> {
    K.test(0)
}

main(): Int64 {
    test<I2<String>>()
}
