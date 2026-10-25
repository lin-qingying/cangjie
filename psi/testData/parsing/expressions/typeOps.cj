// 类型运算表达式：is / as / is? / as? 与 Any 互转。
// 官方依据：Cangjie 文档《类型转换》；cjc 1.0.5 / 1.1.3 零诊断。
open class Base {
    public init() {}
}

class Derived <: Base {
    public init() {
        super()
    }
}

func typeOps(x: Any, b: Base): Bool {
    let isBase = x is Base
    let isDerived = x is Derived
    let asBase = x as Base
    let isMaybe = x is? Base
    let asMaybe = x as? Base
    let derivedIsBase = b is Base
    let _ = asBase
    let _ = asMaybe
    return isBase && isDerived && isMaybe && derivedIsBase
}

func narrow(x: Any): Int64 {
    if (x is Int64) {
        let n = (x as Int64).getOrThrow()
        return n
    }
    return 0
}
