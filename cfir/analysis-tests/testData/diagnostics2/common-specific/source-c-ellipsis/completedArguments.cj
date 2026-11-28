package s4_c_ellipsis_source

foreign func cEllipsisFixed3(first: Int32, middle: Int64, last: Int32, ...): Int32

// C ellipsis 不占 formal，默认正文保留真实 SOURCE 声明调用。
public interface CEllipsisSource {
    func zero(first: Int32, middle: Int64, last: Int32): Int32 {
        return unsafe { cEllipsisFixed3(first, middle, last) }
    }

    func one(first: Int32, middle: Int64, last: Int32, extra: Int32): Int32 {
        return unsafe { cEllipsisFixed3(first, middle, last, extra) }
    }

    func heterogeneous(
        first: Int32, middle: Int64, last: Int32,
        small: Int8, flag: Bool, real: Float32, text: CString, pointer: CPointer<Int32>
    ): Int32 {
        return unsafe { cEllipsisFixed3(first, middle, last, small, flag, real, text, pointer) }
    }

    // CType 检查目标下，嵌套整数运算和浮点字面量仍走正常 completion。
    func nestedLiterals(): Int32 {
        return unsafe { cEllipsisFixed3(1, 2 + 3, 4, (5 + 6) * 7, true, 8.5) }
    }
}
