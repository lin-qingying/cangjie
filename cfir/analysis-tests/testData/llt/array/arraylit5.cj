

class A {}

class B {}

main() {
    var arr1 = [1, A(), true]
    var arr2 = [A(), B()]
    let x: Object = arr2[0]
    if (arr1[0] is Int64 && arr1[1] is A && arr1[2] is Bool) {
        return 0
    }
    return 1
}
