package sample.rename

// twice appears in a comment
func twice(value: Int64): Int64 {
    return value * 2
}

func caller(): Int64 {
    let note = "twice in a string"
    return twice(2)
}
