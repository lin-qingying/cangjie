// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg2

internal import pkg1.*

interface I1 {
    func foo(): Int64
}

extend<T> A<T> <: I1 {
    public func foo() {
        1
    }
}

public func f() {
    A<Int64>().foo()
}
