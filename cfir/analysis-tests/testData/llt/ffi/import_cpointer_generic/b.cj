// FILE: a.cj


package test.subpkg

public class XDArry<T> where T <: CType & ToString {
    let data_: CPointer<T>
    public prop data: CPointer<T> {
        get() {
            this.data_
        }
    }
    public init() {
        this.data_ = CPointer<T>()
    }
}

// FILE: b.cj



import test.subpkg.*

public func funcA(x: XDArry<Float64>): Unit {
    return
}

main(): Int64 {
    return 0
}
