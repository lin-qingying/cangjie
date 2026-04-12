// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj %enableO2 -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %f %enableO2 -o %output
// EXEC: %run %run_opt %output

internal import pkg.*

main() {
    let a = getA()
    a.setObj(a)
    if (a.run() != 222) {
        return 1
    }
    return 0
}
