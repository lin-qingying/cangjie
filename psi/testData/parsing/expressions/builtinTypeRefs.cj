// 内置类型引用：全部整型 / 浮点 / Unit / Rune / Nothing 作为形参与返回类型。
// 官方依据：Cangjie 文档《基本数据类型》；cjc 1.0.5 / 1.1.3 零诊断。
func i8(a: Int8): Int8 {
    return a
}

func i16(a: Int16): Int16 {
    return a
}

func i32(a: Int32): Int32 {
    return a
}

func i64(a: Int64): Int64 {
    return a
}

func iNative(a: IntNative): IntNative {
    return a
}

func u8(a: UInt8): UInt8 {
    return a
}

func u16(a: UInt16): UInt16 {
    return a
}

func u32(a: UInt32): UInt32 {
    return a
}

func u64(a: UInt64): UInt64 {
    return a
}

func uNative(a: UIntNative): UIntNative {
    return a
}

func f16(a: Float16): Float16 {
    return a
}

func f32(a: Float32): Float32 {
    return a
}

func f64(a: Float64): Float64 {
    return a
}

func boolFn(a: Bool): Bool {
    return a
}

func runeFn(a: Rune): Rune {
    return a
}

func unitFn(): Unit {}

func nothingFn(): Nothing {
    throw Exception()
}
