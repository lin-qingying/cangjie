

main(): Int64 {
    let (x, y) = (true, true)
    var a = {=> x}
    var b = {=> y}
    var c = {
        =>
        x;
        y
    }
    return 0
}
