// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1 pkg2
internal import pkg1.*
internal import pkg2.*

interface I2 {
    func foo(): Int64
}

extend<T> A<T> <: I2 {
    public func foo() { 2 }
}

main() {
    A<Rune>()
    return f() - 1
}
