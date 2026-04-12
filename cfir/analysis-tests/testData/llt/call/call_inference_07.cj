// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

class B {
    let v: A<Int64>
    init(a: A<Int64>) {
        v = a
    }
    // Must defined before A, and A must have more than one operator function '[]'
    func test(): Int64 {
        v[1]
    }
}

public struct A<T> {
    let v: T
    init(a: T) {
        v = a
    }

    public operator func [](index: Int64) {
        v
    }
    public operator func [](index: Range<Int64>) {
        A(index.start)
    }
}

main() {
    let b = B(A(1))
    if (b.test() != 1) {
        return 1
    }
    return 0
}
