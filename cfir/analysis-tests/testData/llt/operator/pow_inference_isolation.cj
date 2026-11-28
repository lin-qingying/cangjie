func f(x: Int32): Float64 {
    10.0
}

func f(x: Int64): Int64 {
    -10
}

// lambda 形参类型通过泛型 T 推断，回调返回 Unit。
func withBase<T>(base: T, action: (T) -> Unit): Unit {
    action(base)
}

class PowResource <: Resource {
    public let value: Float64
    private var closed: Bool = false

    public init(value: Float64) {
        this.value = value
    }

    public func isClosed(): Bool {
        closed
    }

    public func close(): Unit {
        closed = true
    }
}

func checkLambdaInference() {
    withBase(2.0, { base => let _ = base ** f(2i32) })
    withBase(2.0, { base => let _ = base ** f(2i64) })
    withBase(2.0, { base => let _ = base <!INVALID_BINARY_OPERATOR!>**<!> f(2) })
}

func checkResourceInitializers() {
    try (resource = PowResource(2.0 ** f(2i32))) {
        let _ = resource.value
    }
    try (resource = PowResource(2.0 ** f(2i64))) {
        let _ = resource.value
    }
    try (resource = PowResource(2.0 <!INVALID_BINARY_OPERATOR!>**<!> f(2))) {
        let _ = resource.value
    }
}

main() {
    checkLambdaInference()
    checkResourceInitializers()
    0
}
