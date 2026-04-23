G.ITF.03 类型定义时避免同时声明实现父接口和子接口

【级别】建议

【描述】

同时实现父接口和子接口时，父接口属于冗余信息，对开发者甄别信息造成困扰。避免声明重复的父接口可以让声明保持简洁。

```cangjie
interface Base {
    func f1(): Unit
}

interface Sub <: Base {
    func f2(): Unit
}

// 符合
class A <: Sub {
    public func f1(): Unit {
        // CODE
    }

    public func f2(): Unit {
        // CODE
    }
}

// 不符合
class B <: Sub & Base {
    public func f1(): Unit {
        // CODE
    }

    public func f2(): Unit {
        // CODE
    }
}
```