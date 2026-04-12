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
