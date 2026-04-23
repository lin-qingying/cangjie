struct Tensor<T> {
    public var no: LazyOp = LazyOp()
}

open class LazyOp{
    public func execute () { return this }
}

class OP<T> <: LazyOp {
    public func CallAsFunction(x : Tensor<T>) : Tensor<T>
    {
        return x
    }
}

func testop<T>(input : Tensor<T>) : Tensor<T> {
    var op = OP<T>();
    var res = op.CallAsFunction(input)
    return res
}

main() : Int64 {
    return 0
}
