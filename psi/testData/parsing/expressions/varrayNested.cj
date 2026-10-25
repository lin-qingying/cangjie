// VArray 嵌套类型：定长数组的嵌套定长数组类型与多维初始化。
// 官方依据：Cangjie 文档《VArray 类型》；cjc 1.0.5 / 1.1.3 零诊断。
func varrayNested(): VArray<VArray<Int64, $2>, $2> {
    let m: VArray<VArray<Int64, $2>, $2> = [[1, 2], [3, 4]]
    let _ = m
    return m
}

class Matrix {
    private var cells: VArray<VArray<Int64, $2>, $2> = [[0, 0], [0, 0]]

    public init() {}

    public func at(i: Int64, j: Int64): Int64 {
        return cells[i][j]
    }
}
