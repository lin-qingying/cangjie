// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Father {}

class Child <: Father {}

interface J {
    func foo(a: Int16): Child
}

public class B <: J {
    public func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(a: Int16): Father {
        return Child()
    }
}
