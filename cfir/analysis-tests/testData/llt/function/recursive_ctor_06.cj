// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    let b = B()
}

class B <: A {
    <!RECURSIVE_CONSTRUCTOR_CALL!>B<!>() {
        super() // the super should be in the dependency path
    }
}

main() {
    let _ = A()
}
