package lazy_variable_initializer

let explicitTop: Int64 = 1
let implicitTop = 2
func retainedBody(): Int64 { return 5 }

class Holder {
    let explicitField: Int64 = 3
    let implicitField = 4
}
