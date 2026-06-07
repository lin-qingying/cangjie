// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    prop a: Int32 {
        get() {
            return 0
        }
    }
}

interface I2 {
    prop a: Int32 {
        get() {
            return 3
        }
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C<!> <: I1 & I2 {}

main() {
    return 0
}
