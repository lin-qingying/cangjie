


var a: Int64 = 5
var b: Int32 = 3
var c = [a, 5]
var d: Int32 = <!TYPE_MISMATCH!>c[1]<!>
var f: Array<Int64> = [<!TYPE_MISMATCH!>[]<!>]
var g: Array<Int64> = [<!TYPE_MISMATCH!>3.1<!>]
var h: Array<Array<Int64>> = [c, [<!TYPE_MISMATCH!>b<!>]]
let i = [[10, 10, 10], <!INCONSISTENT_ARRAY_LITERAL_ELEMENT_TYPE!>[20, 20, r'a']<!>]
var j = <!ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED!>[<!>]
var k: Int64 = <!ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED!>[<!>].size
