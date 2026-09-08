class Box<T> {
    let value: T
    init(value: T) { this.value = value }
}

extend<T> Box<T> <: ToString where T <: ToString {
    public func toString(): String { value.toString() }
}

class Other {}
let stringifyOption = { x => "${Some(x)}" }
let stringifyBox = { x => "${Box(x)}" }

main() {
    println(stringifyOption(1))
    println(stringifyBox("text"))
    stringifyOption(<!ARGUMENT_TYPE_MISMATCH!>Other()<!>)
    stringifyBox(<!ARGUMENT_TYPE_MISMATCH!>Other()<!>)
    0
}
