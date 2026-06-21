
// DIAGNOSTICS: -warnings


main() {
    let data = "123"
    let arr: VArray<UInt8, $1> = [1]
    let c = arr[<!TYPE_MISMATCH!>data[1]<!>]
}
