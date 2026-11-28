package s4_function_abi

// foreign 与访问修饰符冲突；省略 @C 是官方允许的形式。
foreign func s4ForeignFixed(
    first: Int32, middle: Int64, last: Int32
): Int32

foreign func s4ForeignVariadic(
    first: Int32, middle: Int64, last: Int32, ...
): Int32

@C
public func cOrdered(first: Int32, middle: Int64, last: Int32): Int32 {
    return first
}

public func cjOrdered(first: Int32, middle: Int64, last: Int32): Int32 {
    return first
}

public type OrderedC = CFunc<(Int32, Int64, Int32) -> Int32>

// 默认正文保留 foreign 引用的真实 SOURCE 路径。
public interface FunctionAbiSource {
    func fixed(first: Int32, middle: Int64, last: Int32): Int32 {
        let result = unsafe { s4ForeignFixed(first, middle, last) }
        return result
    }

    func variadic(
        first: Int32, middle: Int64, last: Int32, extra: Int32
    ): Int32 {
        let result = unsafe {
            s4ForeignVariadic(first, middle, last, extra)
        }
        return result
    }

    func callback(): OrderedC {
        return cOrdered
    }

    func callbackUse(
        value: CFunc<(Int32, Int64, Int32) -> Int32>
    ): CFunc<(Int32, Int64, Int32) -> Int32> {
        return value
    }

    func ordinary(first: Int32, middle: Int64, last: Int32): Int32 {
        return cjOrdered(first, middle, last)
    }
}
