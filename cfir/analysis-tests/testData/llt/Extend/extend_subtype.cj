// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    func foo(): Int64
}

class A {}

extend A <: I {
    public func foo(): Int64 {
        return 0
    }
}

main() {
    let a: I = A()
    return a.foo()
}
