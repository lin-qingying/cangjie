package completion.expectedparametertype

// 变长的判据是「最后一个**非命名**形参是 Array」——后面还跟着命名形参并不影响它。
func collect(arr: Array<Int64>, extra!: Int64): Int64 {
    extra
}

func arrayParameterBeforeNamedParameterIsVariadic() {
    collect(1, 2, 3, extra: 4)
}
