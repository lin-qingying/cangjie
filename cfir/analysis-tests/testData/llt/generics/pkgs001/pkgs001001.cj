

// Test condition: package with a single generic function is imported

internal import pkgs001001a.foo

main(): Int64 {
    let res = foo<Int32>(3)
    if (res == 3) {
        return 0
    } else {
        return 1
    }
}
