struct Setter {
    Setter(var value!: Int64) {}
    mut operator func [](index: Int64, value!: Int64) { this.value = value }
}

struct Getter {
    Getter(var value!: Int64) {}
    mut operator func [](index: Int64): Int64 { value++; value }
}

func setterReceivers(): Unit {
    let fixed = Setter(value: 0)
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>fixed<!>[0] = 1
    var mutable = Setter(value: 0)
    mutable[0] = 1
    var optional = Some(Setter(value: 0))
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>optional?<!>[0] = 1
}

func getterReceivers(): Unit {
    let fixed = Getter(value: 0)
    let first = <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>fixed<!>[0]
    var mutable = Getter(value: 0)
    let second = mutable[0]
    var optional = Some(Getter(value: 0))
    let third = <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>optional?<!>[0]
}
