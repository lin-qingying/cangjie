package p0_c5_implicit_super

public specific open class Base {
    public specific init(x!: Int64) {}
}

public class Child <: Base {
    public init() {}
}

public func use(): Child { Child() }
