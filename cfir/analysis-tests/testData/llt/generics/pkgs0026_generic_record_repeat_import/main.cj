// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga/pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga/pkga.cj -o pkga/pkga.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga/pkga.%middle pkgb/pkgb.cj --import-path pkga
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga/pkga.%middle pkgb/pkgb.cj --import-path pkga -o pkgb/pkgb.%middle
// DEPENDENCE: pkgb
// EXEC: %compiler %cmp_opt pkga/pkga.%middle pkgb/pkgb.%middle %n.cj --import-path pkga --import-path pkgb
// EXEC: %compiler %cmp_opt pkga/pkga.%middle pkgb/pkgb.%middle %n.cj --import-path pkga --import-path pkgb -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkga.*
internal import pkgb.*

main(): Int64 {
    let ans: R<Int32> = a
    return Int64(ans.a - 1)
}

