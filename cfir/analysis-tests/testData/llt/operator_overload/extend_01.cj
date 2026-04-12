// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-2: %run %run_opt %output %run_args

class A {}

extend Int64 {
    public operator func *(a: A): A {
        return a
    }
}

func s(b: Int32) {
    1
}

func s(b: A) {
    2
}

main() {
    let a = A()
    s(8 * a)
}
