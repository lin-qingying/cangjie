

public class Test<T> {
    public var a: Int64 = 11
    private Test(x: T) {
        println("before")
    }
    public init(x: Int64) {
        Test<Bool>(true)
    }
}

main(): Int64 {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>Test<Int64><!>(1)
    return 0
}
