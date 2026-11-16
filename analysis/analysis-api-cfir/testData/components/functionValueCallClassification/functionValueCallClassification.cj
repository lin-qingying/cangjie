func transform(value: Int64): Int64 {
    return value
}

func functionValueCallClassification(): Int64 {
    let callback: (Int64) -> Int64 = transform
    let fromValue = callback(1)
    let direct = transform(2)
    return fromValue
}
