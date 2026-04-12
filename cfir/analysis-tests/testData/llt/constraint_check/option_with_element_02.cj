// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

// Test for type inference of E<(Option<Int64>, Int64)> with literal (Int64, Int64)
enum E<T> {
    EE(T)
}

func test(): E<(Option<Int64>, Int64)> {
    return EE((1, 1))
}

main(): Unit {
    test()
}
