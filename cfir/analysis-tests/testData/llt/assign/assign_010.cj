

main() {
    let myLetUInt8: UInt8 = 0
    if (true) {
        print("${myLetUInt8}")
    } else {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>myLetUInt8<!> = 12
    }
}
