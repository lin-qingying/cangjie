// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    var a = 1
}

class B {}

func test(a: (A) -> Int64) {
    0
}

func test(a: (B) -> Int64) {
    1
}

main() {
    test {r => r.a}
}
