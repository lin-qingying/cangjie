


// generic container

import std.collection.*

let f11 = {
    x =>
    let arr = ArrayList<Int>()
    for (v in x) {
        arr.add(v + v)
    }
    arr
}
let f17 = {
    x => x |> map {x => x + 1} |> collectArray
}
let f26 = {
    l =>
    let i = l.iterator()
    var max = 0
    while (let Some(v) <- i.next()) {
        if (v > max) {
            max = v
        }
    }
    max
}

main() {
    println(f11([11, 12, 13]))
    println(f17([17, 18, 19]))
    println(f26([1, 26, 2, 3]))
}
