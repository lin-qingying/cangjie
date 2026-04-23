// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    func go(): Int64 {
        return 0
    }
}

extend A {
    func foo(): Int64 {
        go()
    }
}

main(): Int64 {
    let a = A()
    return a.foo()
}
