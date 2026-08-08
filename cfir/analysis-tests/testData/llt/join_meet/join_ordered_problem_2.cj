


interface I1 {}

interface I2 {}

class C <: I1 & I2 {}

let f1 = {_: I1 => ()}
let f2 = {_: I2 => ()}
let f3 = {_: C => ()}

main() {
    let f = match (1) {
        case 1 => f2
        case <!UNREACHABLE_PATTERN!>2<!> => f1
        case <!UNREACHABLE_PATTERN!>3<!> => f2
        case <!UNREACHABLE_PATTERN!>4<!> => f1
        case <!UNREACHABLE_PATTERN!>5<!> => f3
        case <!UNREACHABLE_PATTERN!>6<!> => f1
        case <!UNREACHABLE_PATTERN!>7<!> => f2
        case <!UNREACHABLE_PATTERN!>8<!> => f1
        case <!UNREACHABLE_PATTERN!>_<!> => f2
    }
    f(C())
}
