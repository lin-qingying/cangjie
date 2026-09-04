// FILE: testa.cj


package protobuf.protobuf

public interface Int32Enum <: Hashable {
    func hashCode(): Int64 {
        Int64(1)
    }
}

public interface Enum<E> <: Int32Enum & Equatable<E> where E <: Enum<E> {
    operator func ==(e: E): Bool
    operator func !=(e: E) {
        !(this == e)
    }
}

// FILE: testb.cj


package adapter.lsk.pb

internal import protobuf.protobuf.*

public enum ServerEventType <: Enum<ServerEventType> {
    | EVENTTYPE_UNDEFINED

    public operator func ==(value: ServerEventType) {
        match ((this, value)) {
            case (EVENTTYPE_UNDEFINED, EVENTTYPE_UNDEFINED) => true
            case <!UNREACHABLE_PATTERN!>_<!> => false
        }
    }
}

// FILE: test.cj



internal import adapter.lsk.pb.ServerEventType
internal import std.collection.concurrent.*

main() {
    let callbackMap: ConcurrentHashMap<ServerEventType, Int64> = ConcurrentHashMap<ServerEventType, Int64>()
    callbackMap.get(ServerEventType.EVENTTYPE_UNDEFINED)
    0
}
