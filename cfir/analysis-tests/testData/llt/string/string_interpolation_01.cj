// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1: %compiler %cmp_opt %n.cj
// TIMEOUT: 10

main(): Int64 {
    var (a, b, c, d, e) = foo()
    println("after:${a},${b},${c},${d},${e}")
    return 0
}
