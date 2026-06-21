

var a = VArray<Int64, $4_4>(repeat: 0) // 长度实际为 4，期望为 44
var b = VArray<Int64, $0b11>(repeat: 0) // 长度实际为 0，期望为 3

main() {
    println(a.size)
    println(b.size)
}
