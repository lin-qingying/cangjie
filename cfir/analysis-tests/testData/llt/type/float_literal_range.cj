
// 浮点字面量的目标位宽范围检查。
//
// 判据对齐官方 `ChkFloatTypeOverflow`（`external/cangjie_compiler/src/Sema/CalcConstExpr.cpp:30-85`）：
// 把字面量值转成目标位宽后比对 inf 位模式与"归零"位模式，不是拿字面量值与上下界比较。
// cjc 1.0.5 探针：`var a: Float32 = 3.4028235e38` 通过而 `3.5e38` 报
// `sema_float_literal_too_large`，因为前者转 float32 后恰好等于 `Float.MAX_VALUE` 而非 inf。
//
// 边界文案取自官方 `GetFloatTypeInfoByKind`（`src/AST/Utils.cpp:179-191`），
// `IDEAL_FLOAT` 与 `FLOAT64` 共用同一行。

func float32Range() {
    var a1: Float32 = 3.4028235e38
    var a2: Float32 = <!FLOAT_LITERAL_TOO_LARGE!>3.5e38<!>
    var a3: Float32 = 1e-39
    var a4: Float32 = <!FLOAT_LITERAL_TOO_SMALL!>1e-46<!>
    // 官方下溢守卫要求高精度值非零（`value != 0 && value == 0`），所以字面量本身写作 0 不报。
    var a5: Float32 = 0.0
    var a6: Float32 = <!FLOAT_LITERAL_TOO_SMALL!>1e-400<!>
    var a7: Float32 = <!FLOAT_LITERAL_TOO_LARGE!>1e39<!>
    // 取负号折叠进字面量后，锚点覆盖整个带符号字面量。
    var a8: Float32 = <!FLOAT_LITERAL_TOO_LARGE!>-1e39<!>
    println("${a1}${a3}${a5}")
}

func float64Range() {
    var b1: Float64 = 1.7976931348623157e308
    var b2: Float64 = <!FLOAT_LITERAL_TOO_LARGE!>1.7976931348623159e308<!>
    var b3: Float64 = <!FLOAT_LITERAL_TOO_SMALL!>1e-400<!>
    // 0.0e-400 的精确量级为零，官方不报下溢。
    var b4: Float64 = 0.0e-400
    var b5: Float64 = -0.0
    println("${b1}${b4}${b5}")
}

func float16Range() {
    // 阈值取自官方自己的 `FloatFormat::Float32ToFloat16` 位运算，不是 IEEE 舍入边界：
    // cjc 探针中 65520 不报而 70000 报。
    var c1: Float16 = 65520.0
    var c2: Float16 = <!FLOAT_LITERAL_TOO_LARGE!>70000.0<!>
    // Float16 最小次正规数是 2^-24（官方 min 文案 5.960464477539063E-8），归零阈值是它的半个 ULP。
    var c3: Float16 = 5.960464477539063E-8
    var c4: Float16 = <!FLOAT_LITERAL_TOO_SMALL!>2.9802322e-8<!>
    var c5: Float16 = 0.0
    println("${c1}${c3}${c5}")
}

func idealFloatRange() {
    // 理想浮点按官方表落到 Float64 边界。
    var d1 = <!FLOAT_LITERAL_TOO_LARGE!>1e400<!>
    var d2 = <!FLOAT_LITERAL_TOO_SMALL!>1e-400<!>
    println("${d1}${d2}")
}

main() {
    float32Range()
    float64Range()
    float16Range()
    idealFloatRange()
}
