// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

class A<T> {
    func test(a: T) {
        1
    }
}

extend<T> A<T> {
    func test(a: ?T) {
        if (let Some(t) <- a) {
            return test(t)
        }
        return 2
    }
}

main() {
    let a: A<Int64> = A<Int64>()
    return a.test(Some(1)) - 1
}
