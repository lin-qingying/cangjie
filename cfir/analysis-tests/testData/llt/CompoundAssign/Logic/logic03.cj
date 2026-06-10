// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f(a: Bool) {
    println("f")
    false
}

class A {
    mut prop a: Bool {
        get() {
            false
        }
        set(v) {
            ()
        }
    }
}

main() {
    var x = A()
    x.a &&= f(true)
}
