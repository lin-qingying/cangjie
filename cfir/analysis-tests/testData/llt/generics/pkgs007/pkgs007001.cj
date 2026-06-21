

// Test condition: package with several generic functions along with normal items. But not all items are imported.

internal import pkgs007001a.NormalFunc
internal import pkgs007001a.Bar

main(): Int64 {
    var a: Int32 = 3
    var b = NormalFunc(a)
    var c: Int32 = 4
    var d = <!UNRESOLVED_REFERENCE!>Foo<!><Int32, Bool>(c, true)
    var e: Int32 = 5
    var f = Bar<Int32>(e)

    var h = <!UNRESOLVED_REFERENCE!>one<!>
    var i = <!UNRESOLVED_REFERENCE!>two<!>
    var j = <!UNRESOLVED_REFERENCE!>three<!>

    if (b == 3 && f == 5) {
        return 0
    } else {
        return 1
    }
}
