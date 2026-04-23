// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Base3 {
    func foo(): Int64
}

interface Data <: Base3 {
    func foo(): Int64 {
        return 1
    }
    func foo(a: Int64): Int64 {
        return a
    }
}

class Test <: Data {}

main(): Int64 {
    var obj = Test()
    if (obj.foo() == 1 && obj.foo(0) == 0) {
        return 0
    }

    return 1
}
