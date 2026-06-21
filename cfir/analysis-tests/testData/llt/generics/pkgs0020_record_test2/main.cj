// FILE: pkgs/pkgs.cj


package pkgs

public struct Data<T> {
    public let width: T
    public let length: T
    public init(width1: T, length1: T) {
        width = width1
        length = length1
    }
    public func area(): T {
        return width
    }
}

// FILE: main.cj



internal import pkgs.*

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res: Int32
    var a: Int32 = 3
    var b: Int32 = 4
    let data: Data<Int32> = Data<Int32>(3, 4)
    res = data.area()
    if (res == 3) {
        return 0
    }
    return 1
}
