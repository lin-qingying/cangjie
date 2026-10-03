// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：foreign 函数形参写具名（`x!:`）（官方 Sema/FFI/CFFICheck.cpp CheckCFuncParam：sema_cfunc_cannot_have_named_args "CFunc cannot have named arguments"，锚 `!`）。
// 官方 cjc 1.0.5 / 1.1.3：sema_cfunc_cannot_have_named_args。
foreign func native(x!: Int64): Int64

func foreignNamedParam(): Int64 {
    unsafe {
        return native(x: 1)
    }
}
