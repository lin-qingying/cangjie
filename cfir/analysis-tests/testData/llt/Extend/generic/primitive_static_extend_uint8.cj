

extend UInt8 {
    static func fromLE(a: Int64): UInt8 {
        return 0u8
    }
}

extend UIntNative {
    static func natLE(a: Int64): UIntNative {
        return 0uN
    }
}

main() {
    let a = UInt8.fromLE(1)
    let b = UIntNative.natLE(1)
    return 0
}
