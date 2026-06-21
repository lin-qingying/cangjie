

main(): Int64 {
    return Int64(Data().foo())
}

abstract class Base {
    public func foo(): Int32
}

class Data <: Base {
    public func foo(): Int32 {
        return 0
    }
}
