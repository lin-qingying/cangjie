package s4_c_ellipsis_deferred_unit

foreign func cEllipsisFixed3(first: Int32, middle: Int64, last: Int32, ...): Int32

public func preserve<T>(value: T): T {
    return value
}

// C01：extra 的 Unit 来自嵌套泛型调用完成，不是直接 Unit literal。
public interface CEllipsisDeferredUnit {
    func reject(first: Int32, middle: Int64, last: Int32): Int32 {
        return unsafe { cEllipsisFixed3(first, middle, last, <!UNIT_CANNOT_BE_CFUNC_ARGUMENT!>p<!>reserve(())) }
    }
}
