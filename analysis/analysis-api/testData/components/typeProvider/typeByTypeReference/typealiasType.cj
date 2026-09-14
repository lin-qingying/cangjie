// FILE: typealiasType.cj
package sample.typeprovider.refs

typealias Meter = Int64

func measure(): <caret>Meter {
    return 1
}
