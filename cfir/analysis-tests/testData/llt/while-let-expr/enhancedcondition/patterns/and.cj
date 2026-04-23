// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(a: ?Int64) {
    while (let Some(a) <- a && a > 10) {
        return a + 1
    }
    return -1
}

main() {
    println(foo(Some(18))) // 19
    println(foo(Some(3))) // -1
    println(foo(None))
}
