


import std.collection.*
interface List1<T>{

}
type bb = <!这个实际有错误的!>List1<!><<!这里也有!>T<!>>
type x = <!这个实际有错误的!>List<!><<!这里也有!>T<!>>

main(): Int64 {
    var a: x = [1]
    return a.size - 1
}

