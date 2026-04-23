// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface T {
    func f() {}
}

interface T1 <: T {
    func f() {}
}

abstract class B <: T {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}

open class C <: T {
    public open override func f() {}
}

public class D <: C {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}
