// 抽象类：abstract class / sealed abstract 与具体成员、无体抽象成员。
// 说明：class 体内的无体成员（无论是否写 abstract 修饰符）在官方是**隐式 abstract**，
// 不是语法错误；`abstract` 修饰符本身在 1.0.5 被拒绝（1.1.3 仅 CJMP 接受），
// 属独立的修饰符合法性问题，见 bodylessWithModifier.cj。
// 声明文件形态见 psi/testData/cjd/abstractMembers.cj.d。
// 官方依据：Cangjie 文档《抽象类》；cjc 1.0.5 实测本文件零诊断。
abstract class Shape {
    public init() {}

    public open func describe(): String {
        return "shape"
    }
}

sealed abstract class Root {
    public init() {}
}

class Left <: Root {}

class Right <: Root {}

class Square <: Shape {
    private let side: Int64

    init(side: Int64) {
        super()
        this.side = side
    }

    public func describe(): String {
        return "square"
    }
}

abstract class Animal1 {
    public func makeSound(): String

    protected func makeSound2(): String
}

abstract class Container<T> {
    public func isEmpty(): Bool {
        return size() == 0
    }

    public func size(): Int64 {
        return 0
    }
}
