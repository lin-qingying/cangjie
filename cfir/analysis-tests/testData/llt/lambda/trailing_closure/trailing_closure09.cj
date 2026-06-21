

class Select<R> {
    public func otherwise(fn: () -> R): R {
        fn()
    }
}

main(): Int64 {
    Select<Unit>().otherwise {
        print("select default\n")
    }
    return 0
}
