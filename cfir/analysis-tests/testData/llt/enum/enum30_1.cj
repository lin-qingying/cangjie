// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum Foo {
    A | B | C

    public func f1(v: Foo) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>v<!> = B
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>A<!> = B
    }
}

main(): Int64 {
    0
}
