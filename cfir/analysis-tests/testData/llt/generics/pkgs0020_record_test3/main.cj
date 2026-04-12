// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs/pkgs.cj -o pkgs/pkgs.%middle
// EXEC: %compiler %cmp_opt pkgs/pkgs.%middle %n.cj --import-path pkgs -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkgs.*

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res: Int32
    var res2: Int32
    var a: Int32 = 3
    var b: Int32 = 4
    let data: Data<Int32> = Data<Int32>(a, b)
    let data2: Data2 = Data2(a, b)
    res = data.area()
    res2 = data2.area()
    if (res == 3 && res2 == 12) {
        return 0
    }
    return 1
}
