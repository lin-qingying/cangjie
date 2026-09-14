// FILE: functionType.cj
package sample.typeprovider.refs

func makeHandler(): <caret>(Int64) -> Int64 {
    return { value: Int64 => value }
}
