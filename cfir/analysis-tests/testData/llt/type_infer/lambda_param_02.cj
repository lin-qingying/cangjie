


// basic cases

let f2 = {x => let v: Int = x}
let f2_2 = {x => let v: Any = x}
let f3 = {
    x => if (x) {
        1
    } else {
        2
    }
}
let f3_2 = {
    x => if (!x) {
        1
    } else {
        2
    }
}

main() {
    println(f2(2))
    println(f3(true))
}
