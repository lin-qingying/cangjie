// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

open class A<K, V> {}

extend<K, V> A<K, V> where K <: Hashable {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>hashCode<!>(): UInt64 {
        return 0
    }
}

extend<K, V> A<K, V> where V <: Equatable<V> {
    public operator func ==(right: A<K, V>): Bool {
        return true
    }
}

class Coo<K, V> <: A<K, V> where K <: Hashable & Equatable<K> {
    public func hashCode(): UInt64 {
        return 0
    }
    public operator func ==(right: Coo<K, V>): Bool {
        return true
    }
}
