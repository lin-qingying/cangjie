// FILE: pkga/pkga.cj


package pkga

public struct R<T> {
    public var a: Int32 = 0
    public init(a: Int32) {
        this.a = a
    }
}

// FILE: pkgb/pkgb.cj


package pkgb

internal import pkga.*

public var a = R<Int32>(1)

// FILE: main.cj



internal import pkga.*
internal import pkgb.*

main(): Int64 {
    let ans: R<Int32> = a
    return Int64(ans.a - 1)
}
