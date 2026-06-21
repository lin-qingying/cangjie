


internal import ops.*

main(): Int64 {
    let input = Tensor<Int32>(0)
    let input2: Int32 = 1
    let value = MaximumOp<Int32>(input, input2)
    return 0
}
