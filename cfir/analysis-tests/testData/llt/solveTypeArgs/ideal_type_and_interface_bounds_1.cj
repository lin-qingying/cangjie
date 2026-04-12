// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC-11: %run %run_opt %output %run_args

func test<T>(a: T): T where T <: ToString {
    a
}

main(): Int64 {
    return test(11)
}
