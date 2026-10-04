package completion.sections

func outer(param: Int64): Int64 {
    let local = param
    return local
}

func shadowed(): Int64 {
    let value = 1
    return value
}
