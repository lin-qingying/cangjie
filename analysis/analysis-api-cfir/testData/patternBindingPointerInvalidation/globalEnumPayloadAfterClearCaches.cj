package test.patterns.pointers

enum Parcel<T> { | Packet(T, (Bool, Int64)) }

let Packet(se<caret_serial>rial, (fl<caret_flag>ag, ta<caret_tail>il)) = Parcel<Int64>.Packet(1, (true, 2))

func readSerial(): Int64 { return se<caret_serialUse>rial }
func readFlag(): Bool { return fl<caret_flagUse>ag }
func readTail(): Int64 { return ta<caret_tailUse>il }

main() {
    println(readSerial())
    println(readFlag())
    println(readTail())
}
