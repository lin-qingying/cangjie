// RUN_PIPELINE_TILL: FRONTEND
// 类型转换与类型检查：T(e) 数值转换、as、is；数值类型之间的转换形态。
// 官方依据：Cangjie 文档《类型转换》；cjc 1.0.5 / 1.1.3 零诊断。
func numericConversions(a: Int64, f: Float64): Unit {
    let i8 = Int8(a)
    let i16 = Int16(a)
    let i32 = Int32(a)
    let u8 = UInt8(a)
    let u16 = UInt16(a)
    let u32 = UInt32(a)
    let u64 = UInt64(a)
    let f16 = Float16(f)
    let f32 = Float32(f)
    let f64 = Float64(f)
    let back = Int64(f)
    let _ = (i8, i16, i32, u8, u16, u32, u64, f16, f32, f64, back)
}

func typeChecks(x: Any, y: Int64): Unit {
    let isInt = x is Int64
    let isStr = x is String
    let asInt = x as Int64
    let asStr = x as String
    let _ = (isInt, isStr, asInt, asStr, y)
}
