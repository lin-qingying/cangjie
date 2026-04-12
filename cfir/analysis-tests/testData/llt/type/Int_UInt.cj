// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-0: %run %run_opt %output %run_args

func foo(a: Int): UInt {
    return UInt64(a)
}

main() {
    var a = 10
    var b: Int64 = 5
    var c: Int = 5
    var d: UInt64 = 5
    var e: UInt = 5
    return Int64(foo(a) * 2 - d - e) - b - c
}
