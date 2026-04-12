// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output

class A<T> {
    func test<R>(a: T, b: R) {
        return 123
    }
}

extend<T> A<T> {
    func foo(a: T, b: T) {
        return test<T>(a, b)
    }
}

main() {
    let a = A<Int64>()
    if (a.foo(1, 2) != 123) {
        return 1
    }
    return 0
}
