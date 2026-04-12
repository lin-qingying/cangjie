// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

import a.Foo

private interface I1 {
    public func f1(): Unit
}

internal interface I2 {
    public func f1(a: Int64): Unit
    public func f2(): Unit
}

extend Foo <: I1 & I2 {
    public func f1() {}
    public func f1(a: Int64) {}
    public func f2(): Unit {}
}
