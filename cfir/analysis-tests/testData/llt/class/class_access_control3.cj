// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    public open func abc() {}
}

class B <: A {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>abc<!>() {}
}

open class D {
    public open func f() {}
}

public open class E <: D {
    public open override func f() {}
}

class F <: E {
    private override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}
