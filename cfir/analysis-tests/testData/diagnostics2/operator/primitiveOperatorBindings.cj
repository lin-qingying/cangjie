// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_VERSION: 1.1.3

package primitive_operator_bindings

// 官方 BuiltInOperatorUtil：全部具体数值类型的算术、一元负号和六种比较。

func numericInt8(a: Int8, b: Int8): (Int8, Int8, Int8, Int8, Int8, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericInt16(a: Int16, b: Int16): (Int16, Int16, Int16, Int16, Int16, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericInt32(a: Int32, b: Int32): (Int32, Int32, Int32, Int32, Int32, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericInt64(a: Int64, b: Int64): (Int64, Int64, Int64, Int64, Int64, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericIntNative(a: IntNative, b: IntNative): (IntNative, IntNative, IntNative, IntNative, IntNative, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericUInt8(a: UInt8, b: UInt8): (UInt8, UInt8, UInt8, UInt8, UInt8, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericUInt16(a: UInt16, b: UInt16): (UInt16, UInt16, UInt16, UInt16, UInt16, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericUInt32(a: UInt32, b: UInt32): (UInt32, UInt32, UInt32, UInt32, UInt32, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericUInt64(a: UInt64, b: UInt64): (UInt64, UInt64, UInt64, UInt64, UInt64, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericUIntNative(a: UIntNative, b: UIntNative): (UIntNative, UIntNative, UIntNative, UIntNative, UIntNative, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericFloat16(a: Float16, b: Float16): (Float16, Float16, Float16, Float16, Float16, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericFloat32(a: Float32, b: Float32): (Float32, Float32, Float32, Float32, Float32, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func numericFloat64(a: Float64, b: Float64): (Float64, Float64, Float64, Float64, Float64, Bool, Bool, Bool, Bool, Bool, Bool) {
    (a + b, a - b, a * b, a / b, -a, a == b, a != b, a < b, a <= b, a > b, a >= b)
}



// 整数余数、三种位运算和按位求反，不把逻辑非混为整数比较。

func bitsInt8(a: Int8, b: Int8): (Int8, Int8, Int8, Int8, Int8) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsInt16(a: Int16, b: Int16): (Int16, Int16, Int16, Int16, Int16) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsInt32(a: Int32, b: Int32): (Int32, Int32, Int32, Int32, Int32) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsInt64(a: Int64, b: Int64): (Int64, Int64, Int64, Int64, Int64) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsIntNative(a: IntNative, b: IntNative): (IntNative, IntNative, IntNative, IntNative, IntNative) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsUInt8(a: UInt8, b: UInt8): (UInt8, UInt8, UInt8, UInt8, UInt8) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsUInt16(a: UInt16, b: UInt16): (UInt16, UInt16, UInt16, UInt16, UInt16) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsUInt32(a: UInt32, b: UInt32): (UInt32, UInt32, UInt32, UInt32, UInt32) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsUInt64(a: UInt64, b: UInt64): (UInt64, UInt64, UInt64, UInt64, UInt64) {
    (a % b, a & b, a | b, a ^ b, !a)
}

func bitsUIntNative(a: UIntNative, b: UIntNative): (UIntNative, UIntNative, UIntNative, UIntNative, UIntNative) {
    (a % b, a & b, a | b, a ^ b, !a)
}



func runeComparisons(a: Rune, b: Rune): (Bool, Bool, Bool, Bool, Bool, Bool) {
    (a == b, a != b, a < b, a <= b, a > b, a >= b)
}

func boolOperators(a: Bool, b: Bool): (Bool, Bool, Bool, Bool, Bool) {
    (a == b, a != b, !a, a && b, a || b)
}

func nestedBool(a: Bool, b: Bool, c: Bool): Bool {
    (a && !b) || (b && c)
}

func unitComparisons(a: Unit, b: Unit): (Bool, Bool) {
    (a == b, a != b)
}

// 左侧和右侧整数类型不同；结果保持左侧类型。
func heterogeneousShifts(i8: Int8, u16: UInt16, i32: Int32, u64: UInt64, u8: UInt8, i16: Int16): (Int8, UInt16, Int32, UInt64) {
    (i8 << u64, u16 >> i8, i32 << u8, u64 >> i16)
}

// 官方 SEMA_EXP_TYPES 的三个签名，不能套用同操作数类型的算术规则。
func powerSignatures(i: Int64, u: UInt64, f: Float64, exponent: Int64): (Int64, Float64, Float64) {
    (i ** u, f ** exponent, f ** f)
}

// Ideal 字面量必须在声明上下文和选定运算签名中定型。
func idealInt8(): Int8 { (1 + 2) * 3 }
func idealUInt32(): UInt32 { (1 + 2) - 1 }
func idealFloat32(): Float32 { (1.25 + 2.5) / 2.0 }
func idealPower(): Int64 { 2 ** 3 }
func idealInferred(): Int64 {
    let value = 1 + 2
    value
}

func nestedChain(a: Int64, b: Int64): Int64 { ((a + b) * (a - b)) / b }

// 无显式 lambda 参数类型；泛型实参/返回上下文约束其 operator 接收者。
func applyOperator<T>(f: (T) -> T, seed: T): T { f(seed) }

func pclaInt32(seed: Int32): Int32 {
    applyOperator({ value => (value + 1) * 2 }, seed)
}

func pclaFloat32(seed: Float32): Float32 {
    applyOperator({ value => (value + 1.0) / (value - 2.0) }, seed)
}

func pclaNested(seed: Int16): Int16 {
    applyOperator({ outer => applyOperator({ inner => inner + outer }, seed) }, seed)
}

// 正常用户 operator 必须保留真实声明目标，不能转成无 symbol 的 primitive binding。
class OperatorBox {
    public init() {}
    public operator func +(other: OperatorBox): OperatorBox { other }
    public operator func -(): OperatorBox { this }
    public operator func !(): Bool { true }
}

func userPlus(a: OperatorBox, b: OperatorBox): OperatorBox { a + b }
func userUnaryMinus(a: OperatorBox): OperatorBox { -a }
func userNot(a: OperatorBox): Bool { !a }
