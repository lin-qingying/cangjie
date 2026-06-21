

main() {
    let x = UInt8(UInt32(r'A'))
    match (x) {
        case 'A' | 'B' => 0
        case _ => 1
    }
}
