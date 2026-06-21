


func test1(a: Int32): Int32 {
    a
}

func test2(a: <!UNRESOLVED_REFERENCE!>T<!>): <!UNRESOLVED_REFERENCE!>T<!> {
    a
}

func test3<T>(a: T): T {
    a
}

main() {
    var a = <!NO_VALUE_FOR_PARAMETER!>test1()<!>
    var b = <!GENERIC_ARGUMENT_NO_MATCH!>test1<Int32>()<!>
    var c = test1(1)
    var d = <!NO_VALUE_FOR_PARAMETER!>test2()<!>
    var e = <!GENERIC_ARGUMENT_NO_MATCH!>test2<Int32>()<!>
    var f = test2(1)
    var g = <!GENERIC_ARGUMENT_NO_MATCH!>test2<Int32>(1)<!>
    var h = test3(1)
    var i = <!NO_VALUE_FOR_PARAMETER!>test3<Int32>()<!>
    var j = <!NO_VALUE_FOR_PARAMETER!>test3()<!>
    var k = <!GENERIC_ARGUMENT_NO_MATCH!>test3<Int32, Int32>(1)<!>
    var l = test3<Int32>(1)
    return 0
}
