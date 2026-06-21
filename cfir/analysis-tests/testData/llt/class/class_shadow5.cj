

open class Base {
    private let val: Int32 = 0
    private func f(): Int32 {
        return 0
    }
}

class Data <: Base {
    public static let val: Int32 = 1
    public static let f: Int32 = 2
}

main(): Int64 {
    var d = Data.val
    var f = Data.f
    if (d != 1 || f != 2) {
        return 1
    }
    return 0
}
