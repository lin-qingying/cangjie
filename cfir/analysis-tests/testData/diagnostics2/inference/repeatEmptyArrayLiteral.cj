// 形态照 mariadb `src/protocol/server_result.cj`：
// `Array<?Array<Byte>>(columnCount, repeat: [])` 的 `repeat:` 实参是空数组字面量，
// 期望类型是元素类型 `?Array<Byte>`（Option 自动装箱）。官方 cjc 接受该写法，
// CFIR 若只按字面量自身推断就会报 ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED。
class BinaryRow {
    protected let row: Array<?Array<Byte>>

    protected init(columnCount: Int64) {
        this.row = Array<?Array<Byte>>(columnCount, repeat: [])
    }

    public func rows(): Array<?Array<Byte>> {
        return row
    }
}

func emptyRows(columnCount: Int64): Array<?Array<Byte>> {
    return Array<?Array<Byte>>(columnCount, repeat: [])
}

func emptyRowsWithElementType(columnCount: Int64): Array<Array<Byte>> {
    return Array<Array<Byte>>(columnCount, repeat: [])
}
