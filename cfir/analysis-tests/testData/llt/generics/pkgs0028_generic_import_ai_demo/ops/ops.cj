

package ops

public struct Tensor<T> {
    public Tensor(var a: Int64) {}
}

public func MaximumOp<T>(input1: Tensor<T>, input2: Float32): Tensor<T> {
    return Tensor<T>(0)
}

public func MaximumOp<T>(input1: Tensor<T>, input2: Int32): Tensor<T> {
    return Tensor<T>(0)
}

public func MaximumOp<T>(input1: Float32, input2: Tensor<T>): Tensor<T> {
    return Tensor<T>(0)
}

public func MaximumOp<T>(input1: Tensor<T>, input2: Tensor<T>): Tensor<T> {
    return Tensor<T>(0)
}

public func adjoint_MaximumOp<T>(input1: Tensor<T>, input2: Tensor<T>) {
    let value = MaximumOp<T>(input1, input2)
    return 0
}

public func adjoint_MaximumOp<T>(input1: Tensor<T>, input2: Int32) {
    let input = Tensor<Int32>(0)
    let value = MaximumOp<Int32>(input, input2)
    return 0
}
