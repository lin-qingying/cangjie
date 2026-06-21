

internal import std.collection.*

class A {
    A(var run!: () -> Unit = {=>}) {}
}

func foo() {}

var test = ArrayList<A>([A(<!NEED_NAMED_ARGUMENT!>foo<!>)])

main() {}
