package default_constructor_models

// Raw 与 metadata 共用：泛型默认 init 借用本 owner 的 T、U，并追加在原成员之后。
public class DefaultClass<T, U> {
    public func choose(first: T, middle: U, last: T): U {
        return middle
    }
}

public struct DefaultStruct<T, U> {
    public func choose(first: T, middle: U, last: T): U {
        return middle
    }
}

// static init 保持原槽位，不能替代应生成的默认实例 constructor。
public class StaticClass {
    static let state: Int64
    static init() {
        state = 41
    }
    public func read(): Int64 {
        return state
    }
}

public struct StaticStruct {
    static let state: Int64
    static init() {
        state = 43
    }
    public func read(): Int64 {
        return state
    }
}

// 显式普通 init 保持原位置，不添加默认实例 init。
public class ExplicitClass {
    public init() {}
    public func read(): Int64 {
        return 47
    }
}

// 普通 primary 参数不生成成员参数字段；已有字段和方法保持顺序，primary init 仍追加。
public class PrimaryClass {
    let stored: Int64
    public func read(): Int64 {
        return stored
    }
    public PrimaryClass(value: Int64) {
        stored = value
    }
}

// 真正成员参数按 value、flag 的顺序形成字段，再追加 primary init。
public struct PrimaryStruct {
    public func read(): Int64 {
        if (flag) {
            return value
        }
        return 0
    }
    public PrimaryStruct(let value: Int64, let flag: Bool) {}
}

// Interface 和 enum 维持各自声明角色，不补 class/struct 默认 init。
public interface NoConstructor {}

public enum NoDefaultInit {
    Empty | Carry(Int64)
}
