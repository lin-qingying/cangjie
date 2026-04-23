// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I2 {
    func foo(): Unit
}

open class C2 {}

extend C2 <: I2 {
    public func foo() {}
}

class D2 <: C2 {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {}
}

main() {}
