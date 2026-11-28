package s4_c_ellipsis_missing_fixed

foreign func cEllipsisFixed3(first: Int32, middle: Int64, last: Int32, ...): Int32

public interface CEllipsisMissingFixed {
    func reject(first: Int32, middle: Int64): Int32 {
        return unsafe { cEllipsisFixed3<!WRONG_NUMBER_OF_ARGUMENTS!>(first, middle)<!> }
    }
}
