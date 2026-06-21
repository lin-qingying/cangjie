


main() {
    var arr = [1, 2, 3, 4, 5]
    var range1 = 1..10 : 3
    var arr1 = arr[range1]
    var arr2 = arr[1..10 : <!TYPE_MISMATCH!>2.0<!>]
    let v: Collection<Int32> = <!TYPE_MISMATCH!>1..100<!>
    return 0
}
