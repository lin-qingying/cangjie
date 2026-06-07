// FILE: pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public open class A {
    func f() {
        0
    }
}

public open class B <: A {
    func ff() {
        2
    } // f2, override f1
}

extend B {
    func fff() {
        2
    }
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

extend B {
    func f() {
        3
    }
    func ff() {
        3
    } // f3. not protected override function for f2/f1
    func fff() {
        3
    }
}

main() {
    let v = B().ff() - B().fff()
    return v
}
