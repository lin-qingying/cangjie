// RUN_PIPELINE_TILL: FRONTEND
class ValidOperatorArity {
    operator func !(): Bool {
        return true
    }

    operator func +(other: ValidOperatorArity): ValidOperatorArity {
        return other
    }

    operator func ()(left: Int64, right: Int64): Int64 {
        let _ = right
        return left
    }

    operator func [](index: Int64, value!: Int64): Unit {
        let _ = index
        let _ = value
        return
    }
}

class InvalidOperatorArity {
    <!INVALID_OPERATOR_PARAMETER_COUNT!>operator<!> func !(value: Bool): Bool {
        return value
    }

    <!INVALID_OPERATOR_PARAMETER_COUNT!>operator<!> func +(left: Int64, right: Int64): Int64 {
        let _ = right
        return left
    }

    <!INVALID_OPERATOR_PARAMETER_COUNT!>operator<!> func <<(count: Int64, extra: Int64): Int64 {
        let _ = extra
        return count
    }
}
