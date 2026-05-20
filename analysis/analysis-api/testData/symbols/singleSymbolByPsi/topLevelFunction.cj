// FILE: topLevelFunction.cj
package sample.symbols.singlepsi

func <caret>selected(): Int64 {
    return 1
}

func other(): Int64 {
    return selected()
}
