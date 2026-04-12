// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

class A<T> {
    func foo(a: Int64, b: Int64) {
        for (i in a..=b) {
            println(i)
        }
    }
}

main() {
    A<Int64>().foo(3, 7)
}
