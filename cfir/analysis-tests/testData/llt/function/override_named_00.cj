// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    protected open func f(a!: Int32): Int32 {
        a + 1
    }
}

class B <: A {
    protected override func f(a!: Int32): Int32 {
        a + 2
    }
}

main() {
    B().f(a: 3)
}
