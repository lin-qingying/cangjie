// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func foo<T>(a: T) {
    bar<Int64>(1)
    a
}

func bar<T>(a: T): T {
    baz(foo<Int64>)
    a
}

func baz<T>(a: T): T {
    a
}

main(): Int64 {
    let a = bar(1)
    if (a != 1) {
        return 1
    }
    return 0
}
