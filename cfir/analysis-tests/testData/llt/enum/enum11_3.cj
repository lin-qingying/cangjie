


func getTime<V>(x: V, z: V): V {
    return x
}

func getTime<V>(x: V): V {
    return x
}

enum Time<T> {
    Day(T)
}

func run(): Int64 {
    var time1 = <!AMBIGUOUS_ARG_TYPE!>T<!>ime.Day(getTime<Int32>)
    var time = Time.Day(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>getTime<!>)
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
