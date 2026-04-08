// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    open func leftNamed(left!: Int64, right: Int64): Int64 {
        return left + right
    }

    open func positional(value: Int64): Int64 {
        return value
    }

    open func renamed(target!: Int64): Int64 {
        return target
    }
}

class Child <: Parent {
    override func <!PARAM_NAMED_MISMATCHED!>leftNamed<!>(left: Int64, right: Int64): Int64 {
        return left + right
    }

    override func <!PARAM_NAMED_MISMATCHED!>positional<!>(value!: Int64): Int64 {
        return value
    }

    override func <!PARAM_NAMED_MISMATCHED!>renamed<!>(result!: Int64): Int64 {
        return result
    }
}
