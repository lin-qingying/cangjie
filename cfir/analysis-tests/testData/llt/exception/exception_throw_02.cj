


public func assertThrown<T>(fn: () -> Int64, msg!: String = ""): Int64 where T <: Exception {
    try {
        return fn()
    } catch (e: T) {
        if (!(e is T)) {
            throw e
        }
    }
    return 0
}

main(): Int64 {
    let a = assertThrown<Exception>({=> 11})
    if (a != 11) {
        return 1
    }
    return 0
}
