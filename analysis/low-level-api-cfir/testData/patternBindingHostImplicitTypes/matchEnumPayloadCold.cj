package test.patterns.pointers

enum Parcel<T> { | Packet(T, (Bool, Int64)) | Empty }

func unwrap(value: Parcel<Int64>): Int64 {
    return match (value) {
        case Packet(se<caret_serial>rial, (fl<caret_flag>ag, ta<caret_tail>il)) => if (flag) { serial + tail } else { serial - tail }
        case Empty => 0
    }
}

main() { println(unwrap(Parcel<Int64>.Packet(1, (true, 2)))) }
