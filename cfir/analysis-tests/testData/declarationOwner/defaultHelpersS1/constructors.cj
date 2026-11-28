package helper_s1_constructors

public class GenericOwner<T, U> {
    public let value: T
    public let other: U

    // 构造器借用类的泛型；默认 helper 必须 STATIC，不能产生 T$0/U$1 或 this。
    public init(first: T, second: U, copy!: T = first, another!: U = second) {
        this.value = copy
        this.other = another
    }

    public func member(seed: T, result!: T = seed): T { result }
}

public class PlainOwner {
    public init(seed: Int64, unused: Int64, result!: Int64 = seed) {}
    public func member(seed: Int64, result!: Int64 = seed): Int64 { result }
    public static func factory(seed: Int64, result!: Int64 = seed): Int64 { result }
}
