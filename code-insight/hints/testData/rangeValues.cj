package sample.hints

func halfOpen(): Int64 {
    var total = 0
    for (i in 0<# ≤ #>..<# < #>10) {
        total += i
    }
    return total
}

func closedEnd(): Int64 {
    var count = 0
    for (j in 0<# ≤ #>..=<# ≤ #>5) {
        count += j
    }
    return count
}

func tail(values: Array<Int64>): Array<Int64> {
    return values[1<# ≤ #>..<# < #>3]
}
