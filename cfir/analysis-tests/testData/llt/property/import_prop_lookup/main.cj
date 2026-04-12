// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %n.cj -o %output
// EXEC: %run %run_opt %output

internal import pkg.*

class dd <: cc {
    init() {
        super("dd")
    }

    public func SayHi() {
        return hi // ok
    }
}

main(): Int64 {
    let str = dd().SayHi()
    if (str != "dd") {
        return 1
    }
    return 0
}
