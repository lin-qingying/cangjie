// VArray：定长数组的类型标注、字面量与构造调用。
// 官方依据：Cangjie 文档《VArray 类型》；cjc 1.0.5 / 1.1.3 零诊断。
func varrayForms(): Unit {
    let l: VArray<Int64, $3> = [1, 2, 3]
    let m = VArray<Int64, $2>({ size => size })
    let m1 = VArray<Int64, $2>(repeat: 0)
    let _ = (l, m, m1)
}

class Holder {
    private var data: VArray<Int64, $4> = [0, 0, 0, 0]

    public init() {}

    public func at(i: Int64): Int64 {
        return data[i]
    }
}
