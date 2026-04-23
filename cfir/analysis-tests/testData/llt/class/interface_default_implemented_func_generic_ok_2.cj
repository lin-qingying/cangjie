// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1<T> {
    func foo1(a: T): Int64 {
        return 0
    }
}

struct R1<T> {}

extend<T> R1<T> <: I1<T> { // ok
    public func foo1(a: T): Int64 {
        return 0
    }
}

main() {
    return 0
}
