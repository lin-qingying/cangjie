package helper_s1_preceding

// 未引用的 unused 也必须成为每个 helper 的前序形参；已有默认值不能随形参副本传播。
public func chain(seed: Int64, unused: Int64,
    first!: Int64 = seed,
    second!: Int64 = seed + first,
    third!: Int64 = if (unused > seed) { second } else { first }): Int64 {
    third
}
