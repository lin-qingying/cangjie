// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package f_collection

public import std.collection.{HashMap, TreeMap}

public interface ValueEqualMap<K, V> <: ValueContainsMap<K, V> where K <: Equatable<K>, V <: Equal<V> {
    func containsValue(value: V): Bool {
        return false
    }
}

public interface ValueContainsMap<K, V> where K <: Equatable<K> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        return false
    }
}

public class LinkedHashMap<K, V> {}

extend<K, V> LinkedHashMap<K, V> <: ValueEqualMap<K, V> where K <: Hashable & Equatable<K>, V <: Equal<V> {}

extend<K, V> LinkedHashMap<K, V> <: ValueContainsMap<K, V> where K <: Equatable<K>, V <: Equal<V> {}
