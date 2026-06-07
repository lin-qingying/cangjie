// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    mut prop x: Int64
}

struct A <: I {
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public mut override prop x: Int64 {
        set(_) {}
    }<!>
}

main() {
    println(A().x)
}
