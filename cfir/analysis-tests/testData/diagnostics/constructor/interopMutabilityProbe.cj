// RUN_PIPELINE_TILL: FRONTEND
//
// P3 批次 2（C 互操作/可变性/比较族）回归语料。官方语义以 cjc 1.0.5 探针取证：
//   - sema_invalid_coalescing: `??` 左操作数必须是 Option（BinaryExpr.cpp:1105-1118）
//   - sema_invalid_tuple_field_ctype: 元组成员不能是 @C struct（TypeCheckType.cpp:299-310）
//   - sema_immutable_access_mutable_func: let 接收者访问 mut 成员（既有 checker 承载）
//   - 裸 CPointer() 泛型无法推导：CFIR 以 UNABLE_TO_INFER_GENERIC_FUNC 承载
//     （官方 sema_pointer_unknow_generic_type 的语义等价改名，保留项目诊断名）
//   - 正常路径零诊断：Option ?? 字面量、普通元组类型、CPointer<Int64>()、pure() 调用
@C
struct CPoint {
    var x: Int64 = 0
    var y: Int64 = 0
}

struct M {
    public mut func bump(): Int64 { 0 }
    public func read(): Int64 { 0 }
}

func probeCoalescing(i: Int64, o: ?Int64): Unit {
    let _ = <!INVALID_COALESCING!>i<!> ?? 1
    let _ = o ?? 1
}

func probeTupleCTypeField(): Unit {
    let bad: <!INVALID_TUPLE_FIELD_CTYPE!>(CPoint, Int64)<!> = (CPoint(), 1)
    let ok: (Int64, Int64) = (1, 2)
    println(<!ARGUMENT_TYPE_MISMATCH!>bad<!>)
    println(<!ARGUMENT_TYPE_MISMATCH!>ok<!>)
}

func probePointer(): Unit {
    let bare = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer<!>()
    let typed = CPointer<Int64>()
    println(<!ARGUMENT_TYPE_MISMATCH!>bare<!>)
    println(<!ARGUMENT_TYPE_MISMATCH!>typed<!>)
}

func probeMutAccess(m: M): Unit {
    let frozen = m
    let _ = <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>frozen<!>.bump()
    let _ = frozen.read()
}
