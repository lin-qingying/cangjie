// FILE: pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

enum B {
    B1

    public func test(a: Int64) {
        a
    }
}

public enum A<T> {
    A1

    public func test(a: Int64) {
        return B1.test(a)
    }
}

public func test() {
    if (A1<Int64>.test(123) != 123) {
        return 1
    }
    return 0
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import pkg.*

main() {
    // First part test for using instantiated decls that already instantiated in imported pkg.
    if (A1<Int64>.test(123) != 123) {
        return 1
    }
    if (test() != 0) {
        return 4
    }
    // Second part test for current package instantiation.
    if (A1<Int16>.test(123) != 123) {
        return 1
    }
    return 0
}
