


// pattern matching and least common upperbound

let f9 = {
    x => match (x) {
        case 1 => 1
        case 2 => 2
        case _ => 0
    }
}
let f10 = {
    x => match (x) {
        case Some(v) => v + 1
        case None => 0
    }
}
let f10_1 = {a => a ?? 0}
let f10_2 = {a, b => a + (b ?? 0)}
let f13 = {
    x => match (1) {
        case 1 => x
        case _ => 1
    }
}

main() {
    println(f9(9))
    println(f10(Some(10)))
    println(f13(13))
    println(f10_1(Some(101)))
    println(f10_2(102, None))
}
