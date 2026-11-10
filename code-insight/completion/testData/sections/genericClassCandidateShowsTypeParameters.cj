package completion.sections

class Box<T> {
    let value: T
    init(value: T) {
        this.value = value
    }
}

// 泛型类有两种写法：`Box` 与 `Box<Int64>`。不显示可用的类型参数，用户只能凭记忆写。
func site(): Int64 {
    completionTarget
}
