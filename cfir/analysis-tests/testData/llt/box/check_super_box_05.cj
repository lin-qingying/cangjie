// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

interface I0 {}

open class A {}

extend A <: I0 {}

class B <: A {}

interface I1 {}

extend B <: I1 {}

main() {
    let ins0: Any = B()
    let ins1: I1 = B()
    let v1 = ins0 is I0
    let v2 = ins1 is I0
    if (!v1) {
        return 1
    } else if (!v2) {
        return 2
    }
    return 0
}
