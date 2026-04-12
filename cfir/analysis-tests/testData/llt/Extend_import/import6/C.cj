// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package C

internal import A.Foo

public interface I2 {
    func f(): Unit
}

extend Foo <: I2 {
    public func f(): Unit {
        print("I2.f\n")
    }
}
