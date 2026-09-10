import std.collection.*
public func contextualEntries() {
    let entries = HashMap<Int32, Rune>([(1, r'a'), (2, r'b')])
}
public func finalizedEntries() {
    let entries = [(1, r'a'), (2, r'b')]
    let values = HashMap<Int32, Rune>(<!ARGUMENT_TYPE_MISMATCH!>entries<!>)
}
public func explicitEntries() {
    let entries = [(1i32, r'a'), (2i32, r'b')]
    let values = HashMap<Int32, Rune>(entries)
}
