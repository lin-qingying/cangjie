// RUN_PIPELINE_TILL: FRONTEND
// C 互操作：foreign 函数（带返回值 / Unit 返回 / @C 注解）、unsafe 块内调用；CFunc 泛型（包一层函数类型的 CFunc）、C 类型形参（CPointer / CString / CType）、
// 返回 CString、返回元组、返回 Unit、返回 CString 的函数类型。官方 CFunc 形参按 Sema/FFI/CFFICheck.cpp CheckCFuncParamType 要求是 C 类型（Unit 形参报
// sema_cfunc_cannot_have_unit_args，见 recovery）。
// 官方依据：Cangjie 文档《C 互操作》；cjc 1.0.5 / 1.1.3 零诊断。
foreign func nativeAbs(x: Int64): Int64
foreign func nativeUnit(): Unit
@C
foreign func nativeC(x: Int64): Int64

func cInteropForms(): Int64 {
    unsafe {
        let a = nativeAbs(1)
        nativeUnit()
        let b = nativeC(2)
        return a + b
    }
}

func cFuncTypes(g: CFunc<(Int64) -> Unit>, h: (CPointer<Int64>) -> Unit, i: (CString) -> Unit, j: (CType) -> Unit, k: () -> CString,
    l: () -> (Int64, Int64), m: (Int64) -> Unit, n: (Int64) -> CString): Unit {
    let _ = (g, h, i, j, k, l, m, n)
}
