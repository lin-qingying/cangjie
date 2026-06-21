


const func f(x: ?Int64, p: (Int64) -> Bool) {
    match (x) {
        case Some(x) where p(x) => 0
        case _ => 1
    }
}

const x = f(0, {x => x % 2 == 0})

main() {
    x
}
