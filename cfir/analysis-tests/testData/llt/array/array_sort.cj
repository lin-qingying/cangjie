

internal import std.sort.*

main() {
    var arr: Array<Int64> = [3, 45, 1, 65, 1, 2]
    let res = [1, 1, 2, 3, 45, 65]
    arr.sortBy(comparator: {a, b => a.compare(b)}, stable: true)
    var i = 0
    for (e in arr) {
        if (e != res[i]) {
            return 1
        }
        i++
    }
    if (i != arr.size || i != 6) {
        return 2
    }
    return 0
}
