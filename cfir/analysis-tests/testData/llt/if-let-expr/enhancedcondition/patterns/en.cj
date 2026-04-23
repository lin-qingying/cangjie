// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum A {
    B | C | D
}

main() {
    let a = B
    println(foo(a))
    println(foo(D))
}

func foo(a: A) {
    if (let B | C <- a) {
        1
    } else {
        -1
    }
}
