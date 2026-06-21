

var a: Array<Int64> = [1, 5]
var b: Array<Array<Int64>> = [a, [1]]
var c: Int32 = <!TYPE_MISMATCH!>a[0]<!>
var d: Int64 = <!TYPE_MISMATCH!>b[1]<!>
