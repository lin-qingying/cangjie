// FILE: testa.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package adapter.lsk.pb

internal import protobuf.protobuf.*

public enum ServerEventType <: Enum<ServerEventType> {
    | EVENTTYPE_UNDEFINED

    public operator func ==(value: ServerEventType) {
        match ((this, value)) {
            case (EVENTTYPE_UNDEFINED, EVENTTYPE_UNDEFINED) => true
            case _ => false
        }
    }
}

// FILE: test.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import adapter.lsk.pb.ServerEventType
internal import std.collection.concurrent.*

main() {
    let callbackMap: ConcurrentHashMap<ServerEventType, Int64> = ConcurrentHashMap<ServerEventType, Int64>()
    callbackMap.get(ServerEventType.EVENTTYPE_UNDEFINED)
    0
}
