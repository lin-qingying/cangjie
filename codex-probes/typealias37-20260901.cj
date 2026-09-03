import std.collection.*
interface List1<T>{}
type bb = List1<T>
type x = List<T>

main(): Int64 {
    var a: x = [1]
    return a.size - 1
}
