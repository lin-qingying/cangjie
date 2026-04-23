// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package test

public interface I {
    func bar(a: Int64): Unit {
        println("bar")
    }

    func bar(): Unit {
        println("bar empty")
    }
}

extend Float64 <: I {
    public func bar(): Unit {
        println("Float64 bar empty")
    }
}

main() {
    let f: Float64 = 3.2
    f.bar()
    f.bar(123)
}
