

// LLT_COMPANION_SOURCES

private func <!CONFLICTING_OVERLOADS!>foo<!>(): Unit {
    println("foo in 2.cj")
}

internal func hoo() {
   println("hoo in 2.cj")
   <!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
}
