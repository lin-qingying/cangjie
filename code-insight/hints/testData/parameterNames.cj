package sample.hints

func greet(value: Int64, times: Int64): Int64 {
    return value * times
}

func use(): Int64 {
    return greet(<#value: #>1, <#times: #>2)
}
