// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {
    public mut prop a: Int64 {
        <!CANNOT_HAVE_PARAMETER!>g<!>et(b) {
            1
        }
        set(v) {}
    }
}

main(): Int64 {
    0
}
