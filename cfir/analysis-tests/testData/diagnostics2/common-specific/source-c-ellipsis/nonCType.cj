package s4_c_ellipsis_non_ctype

foreign func cEllipsisFixed3(first: Int32, middle: Int64, last: Int32, ...): Int32

public class CEllipsisObject {}

public interface CEllipsisNonCType {
    func reject(first: Int32, middle: Int64, last: Int32, extra: CEllipsisObject): Int32 {
        return unsafe { cEllipsisFixed3(first, middle, last, <!ARGUMENT_TYPE_MISMATCH!>extra<!>) }
    }
}
