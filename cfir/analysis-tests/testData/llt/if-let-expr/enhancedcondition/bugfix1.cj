
enum E {
    Foo(Int32, Int32)
}
main() {
    var i: Int32 = 0
    while (let Foo(0, x) <- Foo(i, i)) {
        println("see x=${x}")
        i++
    }
    println("done")
}


