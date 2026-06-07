// FILE: pkg/pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

var g: Int64 = 0

public open class C {}

extend C {
    protected func f() {
        10
    }
    protected mut prop p: Int64 {
        get() {
            g
        }
        set(v) {
            g++
        }
    }
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

class C1 <: C {
    func g() {
        p = 0
        f() + p
    }
}

main() {
    let a = C1()
    if (a.g() != 11) {
        return 1
    }
    return 0
}
