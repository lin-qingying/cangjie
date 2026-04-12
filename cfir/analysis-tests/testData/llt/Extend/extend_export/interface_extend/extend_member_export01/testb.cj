// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

import a.*

public interface I {
    public func f1(): Unit {
        println("f1")
    }
    public func f3(): Unit
}

extend A <: I {
    public func f2() {
        println("f2")
    }
    public func f3() {
        println("f3")
    }
}
