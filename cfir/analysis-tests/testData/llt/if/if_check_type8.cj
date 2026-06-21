


let f1 = {x: (Any) -> Unit => ()}
let f2 = {x: (Int64) -> Unit => ()}
let fp = {x: Int64 => ()}

main() {
    let f = if (true) {
        f1
    } else {
        f2
    }
    <!NO_MATCHING_OPERATOR_INVOKE!>f<!>(fp)
}
