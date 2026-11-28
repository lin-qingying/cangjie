// RUN_PIPELINE_TILL: FRONTEND
// extend：为结构 / 类 / 枚举添加成员函数、operator 函数、prop；泛型 extend 带类型形参、where 约束、接口父类型；同一类型多个 extend 块。
// 官方依据：Cangjie 文档《扩展概述》；cjc 1.0.5 / 1.1.3 零诊断（非泛型 extend 上写 where 是官方 parse 错误，见 recovery/extendWhereNonGeneric.cj）。
interface Show {
    func show(): String {
        return "show"
    }
}

interface Hash {
    func hash(): Int64 {
        return 0
    }
}

struct Vec {
    var x: Int64 = 0
    var y: Int64 = 0
}

extend Vec {
    public func len(): Int64 {
        return x + y
    }

    public prop sum: Int64 {
        get() {
            return len()
        }
    }

    public operator func +(other: Vec): Vec {
        return other
    }

    public operator func *(k: Int64): Vec {
        return this
    }
}

extend Vec {
    public func scale(k: Int64): Vec {
        return this
    }
}

struct Holder<T> {
    var value: T

    init(value: T) {
        this.value = value
    }
}

extend<T> Holder<T> where T <: Show {
    public func render(): String {
        return value.show()
    }
}

extend<T> Holder<T> <: Hash where T <: Hash {
    public func hashCode(): Int64 {
        return value.hash()
    }
}

extend<T> Holder<T> {
    public func unwrap(): T {
        return value
    }
}

enum Flag {
    | On
    | Off
}

extend Flag {
    public func flipped(): Flag {
        return Off
    }
}

open class Node {
    public init() {
    }

    public func name(): String {
        return "node"
    }
}

extend Node {
    public func tag(): String {
        return "tag"
    }
}
