// C 互操作：@C foreign、无返回值 foreign 与 unsafe 调用形态。
// 官方依据：Cangjie 文档《C 互操作》；cjc 1.0.5 / 1.1.3 零诊断。
foreign func nativeAbs(x: Int64): Int64
foreign func nativeUnit(): Unit
@C
foreign func nativeC(x: Int64): Int64

func cInterop(): Int64 {
    unsafe {
        let a = nativeAbs(1)
        nativeUnit()
        let b = nativeC(2)
        return a + b
    }
}
