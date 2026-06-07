// FILE: pkg/pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

var x = 2

public interface I {
    func f1() {
        x
    }
    func f2(): Int64
    func f3() {
        f2() * 10
    }
    func f4(): Int64
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import pkg.*

var x = 1

struct C {}

extend C <: I {
    public func f2() {
        f1() * 10
    }
    public func f4() {
        f3() * 10
    }
}

main(): Int64 {
    var c = C()
    return c.f1() + c.f2() + c.f3() + c.f4() - 2222
}
