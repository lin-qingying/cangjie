// invoke 操作符 —— enum 上的 invoke 和构造器优先级

enum Color {
    Red | Green | Blue

    public operator func ()(brightness: Float64): Float64 {
        return brightness
    }
}

func testEnumInvoke(): Unit {
    let c = Red
    let _ = Red(0.5)
    let _ = c(0.5)
}
