// 枚举与泛型约束：enum 构造器、泛型形参、where 子句与上界。
// 官方依据：Cangjie 文档《枚举》《泛型》；cjc 1.0.5 / 1.1.3 零诊断。
enum Option<T> {
    Some(T) | None
}

enum Color {
    Red | Green | Blue
}

interface Display {
    func render(): String
}

interface Container<T> {
    func get(): T
}

class Box<T> <: Container<T> & Display where T <: Display {
    private var value: T

    init(value: T) {
        this.value = value
    }

    public override func get(): T {
        return value
    }

    public func render(): String {
        return "box"
    }
}

func pick<T>(option: Option<T>, fallback: T): T {
    match (option) {
        case Some(value) => value
        case None => fallback
    }
}
