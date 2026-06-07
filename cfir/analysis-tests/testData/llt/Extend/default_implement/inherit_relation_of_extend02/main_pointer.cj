// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


public interface I1<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
}

public interface I2<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
    func containsValue(value: V): Bool {
        false
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend<V> CPointer<V> <: I1<V> {}<!>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend<V> CPointer<V> <: I2<V> {}<!>

main() {
    0
}
