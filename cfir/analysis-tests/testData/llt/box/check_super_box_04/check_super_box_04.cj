// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %f -o %output
// EXEC: %run %run_opt %output

// (CJNATIVE and Linux) EXEC: %compiler %cmp_opt --output-type=dylib pkg.cj -o pkg.%middle
// (CJNATIVE and Linux) EXEC: %compiler %cmp_opt pkg.%middle %f -o %output
// (CJNATIVE and Linux) EXEC: %run %run_opt %output

internal import pkg.*

interface I1 {}

extend C <: I1 {}

main() {
    let ins0: Any = C()
    let v1 = ins0 is B && ins0 is A<Int32> && ins0 is I<Int32> && ins0 is I<Rune>

    let ins1: I<Rune> = C()
    let v2 = ins1 is B && ins1 is A<Int32> && ins1 is I<Int32> && ins1 is I<Rune>

    let ins2: I1 = C()
    let v3 = ins2 is B && ins2 is A<Int32> && ins2 is I<Int32> && ins2 is I<Rune>

    let v4 = v is B && v is A<Int32> && v is I<Int32>

    if (!v1) {
        return 1
    } else if (!v2) {
        return 2
    } else if (!v3) {
        return 3
    } else if (!v4) {
        return 4
    }
    return 0
}
