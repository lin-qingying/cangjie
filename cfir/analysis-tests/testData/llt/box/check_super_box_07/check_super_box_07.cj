// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %f -o %output
// EXEC: %run %run_opt %output

internal import pkg.*

class B <: A {
    public operator func ==(a: A) {
        true
    }
    public operator func !=(a: A) {
        false
    }
}

main() {
    let a: A = B()
    let v: Any = a
    let cmpRes = match (v) {
        case t: Equatable<A> => t == B()
        case _ => false
    }
    if (cmpRes) {
        return 0
    }
    return 1
}
