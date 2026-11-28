package sample.hints

func demo() {
    let f<#: (Int64) -> Int64#> = { i: Int64 =>
        i + 1<#: Int64#>
    }
}
