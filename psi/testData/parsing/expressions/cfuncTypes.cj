// CFunc 类型：C 函数指针类型作为形参与调用形态。
// 官方依据：Cangjie 文档《C 互操作》；cjc 1.0.5 / 1.1.3 零诊断。
func cFuncTypes(g: CFunc<(Int64) -> Unit>, h: (CPointer<Int64>) -> Unit, i: (CString) -> Unit): Unit {
    let _ = g
    let _ = h
    let _ = i
}

class Callbacks {
    private var handler: ?CFunc<(Int64) -> Int32> = None

    public init() {}

    public func setHandler(cb: CFunc<(Int64) -> Int32>): Unit {
        handler = cb
    }

    public func hasHandler(): Bool {
        return handler is? CFunc<(Int64) -> Int32>
    }
}
