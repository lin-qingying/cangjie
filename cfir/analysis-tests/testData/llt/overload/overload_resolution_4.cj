// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    init(_: () -> Int64) {}
}

func f(_: A) {}

func f(_: Array<Bool>) {}

let _ = f(A() {=> 0})

let _ = f(true)

main() {}
