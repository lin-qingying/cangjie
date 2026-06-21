

func getTime<V>(x: V): V {
    return x
}

func getTime<V, W>(x: V, z: W): V {
    return x
}

func getTime(x: Int32): Int32 {
    return x
}

enum Time<T> {
    Day(T)
}

func run(): Int64 {
    var time = Time.Day(getTime<Int32>)
    var res: Int32 = match (time) {
        case Day(x) => x(3)
    }
    if (res == 3) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}
