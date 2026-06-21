

main() {
    let a: Array<UInt8> = Array<UInt8>(1, repeat: 0)
    a[0] *= match (0) {
        case x => 1
    }
}
