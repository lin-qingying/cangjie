// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A<T> {
    let x: T
    init(x: T) {
        this.x = x
    }
}

class B <: A<Int64> {
    let y = <!USED_BEFORE_INITIALIZATION!>x<!>
    init(x: Int64) {
        super(x)
    }
}

main() {
    println(B(10).y)
}
