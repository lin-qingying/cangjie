// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

open class Base {}

class Sub <: Base {}

class A<T> {
    var v: Int64
    init(x: Sub, y: Sub) {
        v = 1
    } // The closest matching rule chooses this one
    init(x: Base, y: Base) {
        v = 2
    }
}

main() {
    let a = A<Unit>(Sub(), Sub())
    return a.v - 1
}
