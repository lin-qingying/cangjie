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

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend Int64 <: I1<Int64> {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend Int64 <: I2<Int64> {}

main() {
    0
}
