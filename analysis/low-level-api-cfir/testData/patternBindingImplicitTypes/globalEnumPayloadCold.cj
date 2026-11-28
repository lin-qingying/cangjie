package test.patterns.pointers

enum Parcel<T> { | Packet(T, (Bool, Int64)) }

let Packet(se<caret_serial>rial, (fl<caret_flag>ag, ta<caret_tail>il)) = Parcel<Int64>.Packet(1, (true, 2))

func readSerial(): Int64 { return serial }
func readFlag(): Bool { return flag }
func readTail(): Int64 { return tail }

main() {
    println(readSerial())
    println(readFlag())
    println(readTail())
}
