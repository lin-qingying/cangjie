// 局部函数：函数体内声明的函数可捕获外层参数与 this。
// 官方依据：Cangjie 文档《函数定义》；cjc 1.0.5 / 1.1.3 零诊断。
func localFunctionFormsPlain(seed: Int64): Int64 {
    func localGreet(name: String): String {
        return "hello ${name}"
    }
    return localGreet("x").size + seed
}

class LocalFunctionFormsHolder {
    private var value: Int64 = 0

    public init(x: Int64) {
        func scale(a: Int64): Int64 {
            return a * 2 + this.value
        }
        this.value = scale(x)
    }

    public func get(): Int64 {
        func twice(a: Int64): Int64 {
            return a * 2
        }
        return twice(value)
    }
}
