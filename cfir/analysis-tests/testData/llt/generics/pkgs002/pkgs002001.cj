

// Test condition: package with several generic functions is imported

internal import pkgs002001a.Bar

main(): Int64 {
    var a: Int32 = 3
    var b = Bar<Int32>(a)
    if (b == 3) {
        return 0
    } else {
        return 1
    }
}
