// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    func foo(): Int64 {
        0
    }
}

interface I2 <: I1 {
    func foo(): Int64 {
        1
    }
}

class C1 {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend C1 <: I2 & I1 {}<!>

main() {
    return -1
}
