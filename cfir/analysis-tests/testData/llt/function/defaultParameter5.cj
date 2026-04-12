// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output

func foo3(x!: Int64 = 1, y!: Rune = r'a', z!: Float64 = 0.0): Int32 {
    return 1
}

func foo3(x!: Int32 = 1, y!: Rune, z!: Float64 = 0.0): Int32 {
    return 2
}

main(): Int32 {
    let v: Int32 = 1
    foo3(x: v, y: r'c')
    return foo3(x: 1) - 1
}
