package test.patterns.pointers

enum Parcel<T> { | Packet(T, (Bool, Int64)) | Em<caret_emptyDeclaration>pty }

func unwrap(value: Parcel<Int64>): Int64 {
    return match (value) {
        case Packet(se<caret_serial>rial, (fl<caret_flag>ag, ta<caret_tail>il)) => if (fl<caret_flagUse>ag) { se<caret_serialUse>rial + ta<caret_tailUse>il } else { serial - tail }
        case Em<caret_emptyPattern>pty => 0
    }
}

main() { println(unwrap(Parcel<Int64>.Packet(1, (true, 2)))) }
