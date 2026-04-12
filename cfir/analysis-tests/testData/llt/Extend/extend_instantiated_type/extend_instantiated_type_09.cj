// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class B<T> {}

class A<T> {}

extend<T> A<B<T>> {
    static func test(a: B<T>) {
        11
    }
}

extend A<B<Int64>> {
    static func foo() {
        return test(B<Int64>())
    }
}

main() {
    let v = A<B<Int64>>.foo()
    if (v != 11) {
        return 1
    }
    return 0
}
