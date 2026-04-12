// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs

public struct A<K> {
    public var value: K
    public init(val: K) {
        value = val
    }
}

public struct B<V> {
    public var value: A<V>
    public init(val: V) {
        value = A<V>(val)
    }
}

public class C {
    private var value: B<Int64> = B<Int64>(2)
}

public class D<T> {
    public var value: B<Int64> = B<Int64>(1)
}
