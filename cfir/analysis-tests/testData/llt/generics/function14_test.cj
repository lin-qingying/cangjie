


func test1(a: Int32): Int32 {
    a
}

func test2(a: <!UNDECLARED_TYPE_NAME!>T<!>): <!UNDECLARED_TYPE_NAME!>T<!> {
    a
}

func test3<T>(a: T): T {
    a
}

main() {
    var a = test1<!NO_VALUE_FOR_PARAMETER!>()<!>
    var b = <!GENERIC_ARGUMENT_NO_MATCH!>test1<Int32><!NO_VALUE_FOR_PARAMETER!>()<!><!>
    var c = test1(1)
    var d = test2<!NO_VALUE_FOR_PARAMETER!>()<!>
    var e = <!GENERIC_ARGUMENT_NO_MATCH!>test2<Int32><!NO_VALUE_FOR_PARAMETER!>()<!><!>
    var f = test2(1)
    var g = <!GENERIC_ARGUMENT_NO_MATCH!>test2<Int32>(1)<!>
    var h = test3(1)
    var i = test3<Int32><!NO_VALUE_FOR_PARAMETER!>()<!>
    var j = test3<!NO_VALUE_FOR_PARAMETER!>()<!>
    var k = <!GENERIC_ARGUMENT_NO_MATCH!>test3<Int32, Int32>(1)<!>
    var l = test3<Int32>(1)
    return 0
}
