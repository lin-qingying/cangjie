
enum E {
    Foo(Int32, Int32)
}
main() {
    var i: Int32 = 0
    while (let Foo(0, x) <- Foo(i, i)) {
        println("see x=${x}")
        i++
    }
    println("done")
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
