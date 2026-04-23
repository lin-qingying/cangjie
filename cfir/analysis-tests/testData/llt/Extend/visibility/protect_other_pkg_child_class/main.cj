// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg

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
