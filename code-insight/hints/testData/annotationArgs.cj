package sample.hints

class Ann {
    Ann(x: Int64, y: Int64) {}
}

@Ann(<#x: #>1, <#y: #>2)
func demo() {}
