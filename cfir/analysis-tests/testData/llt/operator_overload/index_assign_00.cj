

class Input {}

main() {
    let n: Int64 = 1
    let cond = false
    var input = Input()
    <!CANNOT_ASSIGN_TO_SUBSCRIPT!>input[0]<!> = 1
}
