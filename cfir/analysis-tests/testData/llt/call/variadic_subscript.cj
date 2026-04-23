// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    operator func [](x: Array<Int8>) {
        x.size
    }
    operator func [](x: Array<Int8>, value!: Int64) {
        println(x.size + value)
    }
}

main() {
    let a = A()
    a[1] = a[1, 2, 3, 4]
}
