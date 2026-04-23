// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1 {
    func foo() {}
}

open class C1 {}

extend C1 <: I1 {}

class D1 <: C1 {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {}
}

main() {}
