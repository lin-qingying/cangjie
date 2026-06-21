


internal import std.collection.*

main() {
    let a: Array<Int32> = [1, 2, 3]
    for (i in a where i > 1) {
        print(i.toString())
    }
    let t = ArrayList<Int32>([1, 2, 3])
    for (i in t where i > 1) {
        print(i.toString())
    }
    0
}
