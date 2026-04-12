// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p2

internal import p1.*

interface I1 {
    func f(): Unit
}

public interface I2 {
    func g(): Unit
}

extend Foo <: I1 & I2 { // not public
    public func f(): Unit {}
    public func g(): Unit {}
}

extend Bar <: I2 { // public
    public func g(): Unit {}
}
