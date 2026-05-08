// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    public open func leftNamed(right: Int64, left!: Int64): Int64 {
        return left + right
    }

    public open func positional(value: Int64): Int64 {
        return value
    }

    public open func renamed(target!: Int64): Int64 {
        return target
    }
}

class Child <: Parent {
    public override func <!PARAM_NAMED_MISMATCHED!>leftNamed<!>(right: Int64, left: Int64): Int64 {
        return left + right
    }

    public override func <!PARAM_NAMED_MISMATCHED!>positional<!>(value!: Int64): Int64 {
        return value
    }

    public override func <!PARAM_NAMED_MISMATCHED!>renamed<!>(result!: Int64): Int64 {
        return result
    }
}
