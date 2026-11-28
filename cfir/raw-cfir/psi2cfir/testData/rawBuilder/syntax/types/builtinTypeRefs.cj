// RUN_PIPELINE_TILL: FRONTEND
// 内置类型关键字作为类型引用：全部整数 / 浮点类型、Bool / Rune / String / Unit / Nothing / This / VArray、CString / CPointer；
// 出现在函数形参、返回值、变量标注等位置。
// 官方依据：Cangjie 文档《关键字》《基本数据类型》《C 互操作》；cjc 1.0.5 / 1.1.3 零诊断。
func i8(a: Int8): Int8 { return a }
func i16(a: Int16): Int16 { return a }
func i32(a: Int32): Int32 { return a }
func i64(a: Int64): Int64 { return a }
func iNative(a: IntNative): IntNative { return a }
func u8(a: UInt8): UInt8 { return a }
func u16(a: UInt16): UInt16 { return a }
func u32(a: UInt32): UInt32 { return a }
func u64(a: UInt64): UInt64 { return a }
func uNative(a: UIntNative): UIntNative { return a }
func f16(a: Float16): Float16 { return a }
func f32(a: Float32): Float32 { return a }
func f64(a: Float64): Float64 { return a }
func b(a: Bool): Bool { return a }
func r(a: Rune): Rune { return a }
func s(a: String): String { return a }
func u(a: Unit): Unit { }
func n(a: Nothing): Nothing { throw Exception("x") }
func varray(a: VArray<Int8, $4>): VArray<Int8, $4> { return a }
func cstring(a: CString): CString { return a }
func cpointer(a: CPointer<Int64>): CPointer<Int64> { return a }

class ThisHolder {
    var v: Int64 = 0

    init() {
    }

    func selfRef(): This {
        return this
    }
}

struct UnitHolder {
    var u: Unit = ()
}
