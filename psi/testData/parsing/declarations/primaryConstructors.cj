// 主构造函数：struct / class 的 primary constructor（let / var 形参直接生成属性）。
// 官方依据：Cangjie 文档《构造函数》；cjc 1.0.5 / 1.1.3 零诊断。
struct PctorRect {
    public PctorRect(let width: Int64, let height: Int64) {}

    public func area(): Int64 {
        return this.width * this.height
    }
}

class PctorCounter {
    public PctorCounter(var count: Int64) {
        this.count = count + this.offset()
    }

    private func offset(): Int64 {
        return 1
    }

    public func bump(): Unit {
        count++
    }

    public func get(): Int64 {
        return count
    }
}

class PctorMixed {
    private let id: Int64

    public PctorMixed(id: Int64, var name: String) {
        this.id = id
    }
}
