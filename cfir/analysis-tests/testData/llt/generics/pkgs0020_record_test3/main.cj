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

public struct Data2 {
    public let width: Int32
    public let length: Int32
    public init(width1: Int32, length1: Int32) {
        width = length1
        length = width1
    }
    public func area(): Int32 {
        return width * length
    }
}

// FILE: main.cj



internal import pkgs.*

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res: Int32
    var res2: Int32
    var a: Int32 = 3
    var b: Int32 = 4
    let data: Data<Int32> = Data<Int32>(a, b)
    let data2: Data2 = Data2(a, b)
    res = data.area()
    res2 = data2.area()
    if (res == 3 && res2 == 12) {
        return 0
    }
    return 1
}
