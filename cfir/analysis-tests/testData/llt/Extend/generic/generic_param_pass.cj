// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct A<T> {
    A(var aa: T) {}
}

extend<V> A<V> {
    func foo() {
        return aa
    }
}

main() {
    return A<Int64>(-1).foo() + 1
}
