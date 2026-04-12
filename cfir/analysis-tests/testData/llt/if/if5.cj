// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {}

class B {}

func foo1(a: A): Unit {}

func foo2(a: B): Unit {}

main() {
    let a = if (true) {
        foo1
    } else {
        foo2
    }
    if (a is Any) {
        return 0
    }
    return 1
}
