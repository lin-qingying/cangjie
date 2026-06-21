


// solve by generic constraint, and type var created on-the-fly

let f12 = {x => "this is ${x}"}
let f15 = {x => "this is ${Some(x)}"}
let f16 = {g => "this is ${g(0)}"}

main() {
    println(f12(12))
    println(f15(15))
    println(f16({x => x + 16}))
}
