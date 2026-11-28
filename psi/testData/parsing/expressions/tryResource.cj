// try-with-resources：单资源、多资源声明与资源体。
// 官方依据：Cangjie 文档《异常处理》；cjc 1.0.5 / 1.1.3 零诊断。
class Res <: Resource {
    public init() {}

    public func isClosed(): Bool {
        return false
    }

    public func close(): Unit {}
}

class MyError <: Exception {
    public init() {
        super()
    }
}

func tryResourceForms(): Unit {
    try (a = Res()) {
        let x = 1
        let _ = x
    }
    try (b = Res(), c = Res()) {
        let _ = b
        let _ = c
    }
}
