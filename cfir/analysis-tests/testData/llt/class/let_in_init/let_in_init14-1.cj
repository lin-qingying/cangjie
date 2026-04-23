// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Test1 {
    public Test1(let a: Int32, var b: Int32) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.a<!> = a
        this.b = b
    }
}

class Test2 {
    public Test2(let a: Int32, var b: Int32) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = a
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = b
    }
}

main(): Int64 {
    return 0
}
