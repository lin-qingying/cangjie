// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p2

internal import p1.*

extend Foo {
    public func f() {}
}

func g() {
    let a = Foo()
    a.f() // ok
}
