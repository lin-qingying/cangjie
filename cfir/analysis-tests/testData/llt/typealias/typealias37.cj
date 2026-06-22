


import std.collection.*
interface List1<T>{

}
type bb = <!NOT_A_TYPE!>List1<!><<!UNDECLARED_TYPE_NAME!>T<!>>
type x = <!NOT_A_TYPE!>List<!><<!UNDECLARED_TYPE_NAME!>T<!>>

main(): Int64 {
    var a: x = [1]
    return a.size - 1
}
