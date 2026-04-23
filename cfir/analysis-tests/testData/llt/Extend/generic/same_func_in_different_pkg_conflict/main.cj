// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1 pkg2
import pkg1.*
import pkg2.*

interface I1 {
    func foo(): Int64
}

extend<T> A<T> <: I1 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>() {
        1
    }
}

main() {
    return A<Int64>().<!AMBIGUOUS_USE!>foo<!>() - 1
}
