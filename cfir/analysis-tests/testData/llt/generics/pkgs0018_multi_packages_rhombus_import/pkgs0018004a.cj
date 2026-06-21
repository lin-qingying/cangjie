


package pkgs0018004a

internal import pkgs0018002a.run1
internal import pkgs0018003a.run2

main(): Int64 {
    let ans1 = run1()
    let ans2 = run1()
    let ans3 = run2()
    let ans4 = run2()

    if (ans1 == 1 && ans2 == 1 && ans3 == 2 && ans4 == 2) {
        return 0
    }
    return 1
}
