// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

extend<T> A<T> {}

extend A<Int64> {
    func test() {
        println("A int64")
    }
}

main() {
    let a1 = A<Int64>()
    let a2 = A<Rune>()
    a1.test()
    a2.<!NOT_MEMBER_OF!>test<!>()
}
