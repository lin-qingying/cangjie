// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

open class A<T, K> {
    static func test(a: T, b: K) {
        return b
    }
}

class B<K> <: A<Int8, Int32> {}

type C<T> = A<T, Int16>

main() {
    let x = C.test(1, 2)
    if (x != 2) {
        return 1
    }
    return 0
}
