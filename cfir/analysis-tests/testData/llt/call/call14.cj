// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class Base {}

class Sub <: Base {}

func test<X, Y>(a: X, b: Y) {
    return 1
}

func test<I>(a: Base, b: I) where I <: Sub {
    return 2
}

main(): Int64 {
    var I = test<Sub>(Base(), Sub())
    if (I != 2) {
        return 1
    }
    return 0
}
