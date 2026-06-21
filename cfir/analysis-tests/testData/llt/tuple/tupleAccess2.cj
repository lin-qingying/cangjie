

main(): Int64 {
    var res1 = (3.14, "PIE")[0] // ok
    var res2 = (3.14, "PIE")[1] // ok
    var res3 = (3.14, ("PIE", 1))[0] // ok
    var res4 = (3.14, ("PIE", 2))[1][0] // core dump
    if (res1 == 3.14 && res2 == "PIE" && res3 == 3.14 && res4 == "PIE") {
        return 0
    }
    return 1
}
