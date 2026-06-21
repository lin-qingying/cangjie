


open class Base {
    public var b: Int32 = 6
    public init(x: Int32) {
        this.b = 5
    }
}

class <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>Test<!><T> <: Base {
    public var a: Int32 = 3
}
