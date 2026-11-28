// RUN_PIPELINE_TILL: FRONTEND
// 函数修饰符：顶层 public / private / internal、const func、foreign func 声明；类成员上的 static / open / override /
// redef（redef 只能修饰 static 成员，且顺序是 `public redef static func`）、protected；operator 函数只能定义在 class / struct /
// enum / interface / extend 中（见 operatorOverloadForms.cj）。
// 官方依据：Cangjie 文档《函数定义》「函数修饰符」；cjc 1.0.5 / 1.1.3 零诊断。
public func publicFunc(): Int64 {
    return 1
}

private func privateFunc(): Int64 {
    return 2
}

internal func internalFunc(): Int64 {
    return 3
}

public const func constFunc(): Int64 {
    return 4
}

foreign func foreignFunc(p: CPointer<Int64>): Int64

open class Base {
    public init() {
    }

    public open func openFunc(): Int64 {
        return 1
    }

    public static func staticFunc(): Int64 {
        return 2
    }

    protected func protectedFunc(): Int64 {
        return 3
    }

    private func privateFunc(): Int64 {
        return 4
    }

    internal func internalFunc(): Int64 {
        return 5
    }
}

class Derived <: Base {
    public init() {
        super()
    }

    public override func openFunc(): Int64 {
        return 6
    }

    public redef static func staticFunc(): Int64 {
        return 7
    }
}
