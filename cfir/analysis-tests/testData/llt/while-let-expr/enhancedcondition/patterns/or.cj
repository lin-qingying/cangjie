// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(a: ?Int64, b: ?Int64) {
    while (true && false || let None <- b) {
        return 8
    }
    -1
}

func Go(a: Int64) {
    a > 3
}

main() {
    println(foo(Some(15), Some(3))) // -1
    println(foo(Some(8), None)) // 8
}
