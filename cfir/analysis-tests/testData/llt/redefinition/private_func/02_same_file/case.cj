

private func foo(): Unit {
    println("foo in 1")
}

private func <!CONFLICTING_OVERLOADS!>foo<!>(): Unit {
    println("foo in 2")
}
