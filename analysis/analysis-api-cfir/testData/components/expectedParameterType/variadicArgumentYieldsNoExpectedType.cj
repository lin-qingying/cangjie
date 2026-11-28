package completion.expectedparametertype

// 最后一个非命名形参是 `Array<Int64>`：该位既可以传数组本身，也可以传 0..n 个 `Int64`。
func sum(arr: Array<Int64>): Int64 {
    0
}

func variadicArgumentYieldsNoExpectedType() {
    sum(1, 2, 3)
}
