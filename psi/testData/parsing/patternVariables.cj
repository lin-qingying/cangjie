// 模式变量与属性声明：let / var / const / prop 及其模式形式。
// 官方依据：Cangjie 文档《变量》《属性》；cjc 1.0.5 / 1.1.3 零诊断。
let globalConstant = 1
var globalVariable = 2

class Holder {
    private let immutable: Int64 = 0
    private var mutable: Int64 = 1
    public static const LIMIT: Int64 = 100
    public mut prop computed: Int64 {
        get() {
            return immutable
        }
        set(value) {}
    }

    func use(): Int64 {
        let (a, b) = (1, 2)
        var counter = 0
        counter += 1
        return a + b + counter + immutable + mutable + LIMIT + computed
    }
}

