func getHeight(height: Int64): Int64 {
    return height * 2
}

class C {}

class Data<T> {
    public let width: T

    public init(x: C) {
        width = C()
    }
}

func run(): Int32 {
    let data: Data<(Int64) -> Int64> = Data<(Int64) -> Int64>(getHeight)
    var res = data.width(5)
    if (res == 10) {
        return 0
    }
    return 1
}

main(): Int32 {
    return run()
}
