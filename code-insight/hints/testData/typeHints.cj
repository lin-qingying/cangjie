package sample.hints

func inferred(a: Int64)<#: Int64#> {
    let answer<#: Int64#> = a
    let typed: Int64 = 1
    let text<#: String#> = "hello"
    return answer
}

func unitBody() {
    let flag<#: Bool#> = true
}
