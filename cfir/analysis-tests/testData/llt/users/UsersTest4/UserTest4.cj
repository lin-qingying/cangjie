

var i8Number1: Int8 = 127
var i16Number1: Int16 = 0

func funcTest3(): Unit {
    i8Number1 = <!ASSIGNMENT_TYPE_MISMATCH!>Int16(i16Number1)<!> // err
    i8Number1 = <!ASSIGNMENT_TYPE_MISMATCH!>i16Number1<!>
    i8Number1 = <!ASSIGNMENT_TYPE_MISMATCH!>Int16(i16Number1) + i16Number1 + Int16(i16Number1)<!> // err
}
