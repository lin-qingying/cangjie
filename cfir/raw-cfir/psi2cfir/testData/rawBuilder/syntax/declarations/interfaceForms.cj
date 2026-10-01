// RUN_PIPELINE_TILL: FRONTEND
// interface：无实现成员、默认实现、prop（抽象 / static 带 get）、operator 函数、多个父接口、泛型 interface；实现类里以
// 非 mut 的只读 prop 实现接口的抽象 prop（接口 prop 是不可变的，不能用 mut prop 重新声明）。
// 官方依据：Cangjie 文档《接口》；cjc 1.0.5 / 1.1.3 零诊断（接口成员隐式 public，不写 public）。
interface Named {
    func name(): String
}

interface Sized {
    func size(): Int64
}

interface Both {
    func combined(): Int64 {
        return 1
    }

    func required(): Int64

    prop label: String {
        get() {
            return "label"
        }
    }

    prop abstractProp: Int64

    static prop staticProp: Int64 {
        get() {
            return 1
        }
    }

    public operator func ==(other: Both): Bool {
        return true
    }
}

interface Multi <: Named & Sized {
    func describe(): String {
        return "multi"
    }
}

interface GenericBox<T> {
    func box(): T
}

class Impl <: Named & Sized {
    public init() {
    }

    public func name(): String {
        return "impl"
    }

    public func size(): Int64 {
        return 1
    }
}

class BothImpl <: Both {
    private var store: Int64 = 0

    public init() {
    }

    public func required(): Int64 {
        return 2
    }

    public prop abstractProp: Int64 {
        get() {
            return store
        }
    }
}
