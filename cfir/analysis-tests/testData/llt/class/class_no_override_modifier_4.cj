// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1 {
    func foo(): Int64
}

interface I2 {
    func foo(): Int64 {
        return 1
    }
}

open class C1 <: I1 & I2 {}

main(): Int64 {
    var res = C1().foo()
    if (res == 1) {
        return 0
    }
    return 1
}
