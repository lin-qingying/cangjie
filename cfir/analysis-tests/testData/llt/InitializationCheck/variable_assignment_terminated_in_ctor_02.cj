// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    <!CLASS_UNINITIALIZED_FIELD!>init<!>(a: Int64, c: Bool) {
        let b = 1
        if (c) {
            return
        }
        x = a
        println(x)
    }
    init() {
        x = 1
    }

    public var x: Int64
}

main() {
}
