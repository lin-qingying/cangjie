package test.patterns.colors

enum Parcel<T> { | Packet(T, (Bool, Int64)) }
let Packet(globalSerial, (globalFlag, globalTail)) = Parcel<Int64>.Packet(1, (true, 2))

enum Choice { | Ready | Payload(Int64, (Bool, Int64)) }

func unwrap(value: Choice): Int64 {
    return match (value) {
        case Pay<caret_payloadPattern>load(serial, (flag, tail)) => if (flag) { serial + tail } else { serial - tail }
        case Re<caret_readyPattern>ady => globalSerial + globalTail
    }
}

func typed(value: Int64): Int64 {
    return match (value) {
        case narrowed: Int64 => narrowed
    }
}

func bare(value: Int64): Int64 {
    return match (value) {
        case cap<caret_bare>tured => captured
    }
}

func ignored(value: Int64): Int64 {
    return match (value) {
        case <caret_wildcard>_: Int64 => 0
    }
}

main() {
    let selected = if (globalFlag) { Payload(1, (true, 2)) } else { Ready }
    println(unwrap(selected) + typed(3) + bare(4) + ignored(5))
}
