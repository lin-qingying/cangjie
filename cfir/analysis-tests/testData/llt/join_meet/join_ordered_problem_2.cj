


interface I1 {}

interface I2 {}

class C <: I1 & I2 {}

let f1 = {_: I1 => ()}
let f2 = {_: I2 => ()}
let f3 = {_: C => ()}

main() {
    let f = match (1) {
        case 1 => f2
        case 2 => f1
        case 3 => f2
        case 4 => f1
        case 5 => f3
        case 6 => f1
        case 7 => f2
        case 8 => f1
        case _ => f2
    }
    f(C())
}
