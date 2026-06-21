// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    func foo(): Int64 {
        1
    }
}

interface I2 {
    func foo(): Int64 {
        2
    }
}

class C1 {}

extend C1 <: I1 {
    func <!WEAK_VISIBILITY!>foo<!>(): Int64 {
        3
    }
}

extend <!WEAK_VISIBILITY!>C1<!> <: I2 {}

main() {
    return -1
}
