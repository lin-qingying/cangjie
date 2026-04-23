// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface T1 {
    func f() {}
}

interface T2 <: T1 {
    func f() {}
}

open class D <: T2 {
    public open override func f() {}
}

class E <: D {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}
