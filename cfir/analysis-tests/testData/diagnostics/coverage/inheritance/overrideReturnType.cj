// RUN_PIPELINE_TILL: FRONTEND
// Override 返回类型协变检查测试
open class Base {
    open func getValue(): Base {
        return Base()
    }
}

class Child <: Base {
    // OK: 返回类型是父类返回类型的子类型
    func getValue(): Child {
        return Child()
    }
}
