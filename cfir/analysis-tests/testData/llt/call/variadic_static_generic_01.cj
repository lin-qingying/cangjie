// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {
    static func test(a: Array<T>, b!: Bool = false) {
        a
    }
}

class B<T> where T <: Comparable<T> & ToString {
    static func test(a: Array<T>, b!: Bool = false) {
        a
    }
}

main() {
    var a = [1, 2]
    a = A.test()
    a = B.test()
}
