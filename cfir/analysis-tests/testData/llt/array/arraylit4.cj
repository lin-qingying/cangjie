

main() {
    var arr = [0, 1, 2]
    var arr2 = Array(4, {i: Int64 => i})
    var arr3 = [arr, arr2]
    var i = 0
    while (i < arr.size) {
        if (arr[i] != arr2[i]) {
            return 2
        }
        i++
    }
    i = 0
    for (e in arr) {
        if (e != arr2[i]) {
            return 3
        }
        i++
    }
    if (i != 3) {
        return 4
    }
    if (arr2.size != 4) {
        return 5
    }
    var arr1 = Array<Int64>(1, repeat: 1)
    var cpy = arr1.clone()
    cpy[0] = 2
    if (arr1[0] == cpy[0]) {
        return 6
    }
    var sum = 0
    for (e in arr3) {
        for (v in e) {
            sum += v
        }
    }
    if (sum != 9) {
        return sum
    }
    return 0
}
