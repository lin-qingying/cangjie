class Mixed {
    func target(value: Int64): Int64 {
        value
    }

    static func <!STATIC_FUNCTION_OVERLOAD_CONFLICTS!>target<!>(value: Int64): Int64 {
        value
    }
}

main(): Int64 {
    return 0
}
