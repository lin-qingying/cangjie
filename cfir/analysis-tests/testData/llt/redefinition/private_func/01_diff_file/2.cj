

// LLT_COMPANION_SOURCES

private func foo(): Unit {
    println("foo in 2.cj")
}

internal func hoo() {
   println("hoo in 2.cj")
   foo()
}
