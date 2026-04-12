// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs/pkgs.cj -o pkgs/pkgs.%middle
// EXEC: %compiler %cmp_opt pkgs/pkgs.%middle %n.cj --import-path pkgs -o %n.%suffix

internal import pkgs.*

main() {
    var ans: Data<Int32> = data
    if (ans.area() == 3) {
        return 0
    }
    return 1
}
