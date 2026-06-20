// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public let x: Int64 = 1
}

class B <: A {}

extend B {
    public func foo(): Int64 {
        return <!EXTEND_USE_SUPER!>super<!>.x
    }
}
