package s4_c_ellipsis_unit_extra

foreign func cEllipsisFixed3(first: Int32, middle: Int64, last: Int32, ...): Int32

public interface CEllipsisUnitExtra {
    func reject(first: Int32, middle: Int64, last: Int32): Int32 {
        return unsafe { cEllipsisFixed3(first, middle, last, <!UNIT_CANNOT_BE_CFUNC_ARGUMENT!>(<!>)) }
    }
}
