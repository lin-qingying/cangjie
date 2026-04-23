// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkg.cj

internal import pkg.*

class B <: A & I {}

func f(x: I) {
    match (x) {
        case _: A => 0
        case _: I => 1
    }
}

main() {
    f(B())
}
