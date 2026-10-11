// RUN_PIPELINE_TILL: FRONTEND
// extend 内置类型（Int64 / String）添加普通成员函数；extend 结构添加 operator / mut prop / static func；extend 泛型类（类型形参出现在被扩展类型里）。
// 注意：内置类型上不能定义与内建同名的 operator（官方 sema_operator_overload_built_in_binary_operator，见 recovery）；泛型 extend 的类型形参必须在
// 被扩展类型里出现（官方 sema_extend_generic_must_be_used，见 recovery）。
// 官方依据：Cangjie 文档《扩展概述》；cjc 1.0.5 / 1.1.3 零诊断。
struct P {
    var x: Int64 = 0

    public init(x: Int64) {
        this.x = x
    }

    public func d(): Int64 {
        return x
    }
}

extend P {
    public operator func + (o: P): P { return o }

    public mut prop doubled: Int64 {
        get() { return x * 2 }
        set(v) { x = v }
    }

    public static func zero(): P { return P(0) }
}

extend Int64 {
    public func twice(): Int64 { return this * 2 }
}

extend String {
    public func shout(): String { return this + "!" }
}

class G<T> {
    public init() {}
}

extend<T> G<T> {
    public func g(): Int64 { return 0 }
}

func extendBuiltinForms(p: P, n: Int64, s: String): Int64 {
    return p.d() + n.twice() + s.shout().size
}
