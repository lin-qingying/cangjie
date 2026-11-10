package e5probe

class RowHolder {
    private var row: Array<?Array<Byte>>

    public init(columnCount: Int64) {
        this.row = Array<?Array<Byte>>(columnCount, repeat: [])
    }

    public func empty(columnCount: Int64): Array<?Array<Byte>> {
        return Array<?Array<Byte>>(columnCount, repeat: [])
    }
}

main() {
    let h = RowHolder(3)
    let _ = h.empty(3)
}
