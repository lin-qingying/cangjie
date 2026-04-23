// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

enum E<T> {
    A

    static func test(a: T) {
        return a
    }
}

main() {
    let v = E.test(123)
    let x = v + E.test(123)
    if (x != 246) {
        return 1
    }
    return 0
}
