// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    func a() {}
}

interface I2 <: I1 {
    func b() {
        <!INTERFACE_SUPER_NOT_ALLOWED!>super<!>.a()
    }
}

main(): Int64 {
    return 0
}
