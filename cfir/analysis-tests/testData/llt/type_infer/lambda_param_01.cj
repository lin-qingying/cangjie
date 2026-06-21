


// arithmetic operators

let f1 = {x => x + 1}
let f1_2 = {x, y => x && y}
let f1_3 = {x => 1 > 0 && x}
let f1_4 = {x, y => x || y}
let f1_5 = {x => x ** x}
let f1_6 = {x => x ** 10}
let f5 = {x => 1 + x}
let f6 = {x, y => y * 1 + 2 / x}

main() {
    println(f1(1))
    println(f1_2(1 > 0, 2 > 1))
    println(f1_3(1 > 0))
    println(f1_4(1 > 0, 2 > 1))
    println(f1_5(1.5))
    println(f1_6(1.6))
    println(f5(5))
    println(f6(6, 6))
}
