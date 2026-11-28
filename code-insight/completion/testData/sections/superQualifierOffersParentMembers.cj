package completion.sections

open class Base {
    public open func inherited(): Int64 {
        1
    }
}

class Derived <: Base {
    public override func inherited(): Int64 {
        return super.completionTarget
    }
}
