// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %import-cangjie-stdx %cmp_opt %n.cj -o %n.%suffix
// EXEC: %set_stdx_path %run %run_opt %n.%suffix 2>&1 | compare %f
// ASSERT: scan call f

internal import std.collection.*
internal import stdx.serialization.serialization.*

interface I<T> <: Serializable<T> {
    func f(): Unit
}

extend<K, V> HashMap<K, V> <: I<HashMap<K, V>> where K <: Serializable<K> & Hashable & Equatable<K>,
    V <: Serializable<V> {
    public func f(): Unit {
        println("call f")
    }
}

main() {
    var a = HashMap<Int64, String>()
    a.f()
    0
}
