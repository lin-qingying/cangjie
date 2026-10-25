// this 与 super 限定访问：this 成员、super 成员、this() / super() 构造调用。
// 官方依据：Cangjie 文档《this 与 super》；cjc 1.0.5 / 1.1.3 零诊断。
open class Animal {
    public var age: Int64 = 0

    public init() {}

    public open func describe(): String {
        return "animal"
    }
}

class Dog <: Animal {
    public var name: String = "dog"

    public init() {
        super()
    }

    public func bark(): String {
        return this.name
    }

    public override func describe(): String {
        return super.describe() + " dog"
    }

    public func selfCall(): String {
        return this.describe()
    }
}

func thisSuperQualified(d: Dog): String {
    return d.describe()
}
