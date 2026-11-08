// 形态照 mariadb `src/protocol/server_result.cj`：
// `Array<?Array<Byte>>(columnCount, repeat: [])` 的空数组字面量必须按 `Option<Array<Byte>>`
// 自动装箱后的 `Array<Byte>` 推断元素类型；CFIR 曾报 `ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED`。
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
