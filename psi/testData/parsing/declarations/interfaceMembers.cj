// 接口成员：默认实现、mut 函数、static 属性与运算符声明。
// 官方依据：Cangjie 文档《接口》；cjc 1.0.5 / 1.1.3 零诊断。
interface WithConst {
    static prop limit: Int64 {
        get() {
            return 1
        }
    }
}

interface Mutable {
    mut func go(): Unit

    func name(): String {
        return "mutable"
    }
}

interface Comparable2<T> {
    operator func <(other: T): Bool

    func label(): String {
        return "comparable"
    }
}
