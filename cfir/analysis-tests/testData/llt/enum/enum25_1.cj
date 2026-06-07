// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum B {
    a(Int64,Int64,Int64) | b

    public <!parse_illegal_modifier_in_scope!>mut<!> prop b : Int64 {
       get() {1}
       set(v){}
    }

    public func a(){}
    public func a(x: Int64, y: Int64, z: Int64){}
}

enum B {
    aa

    public <!parse_illegal_modifier_in_scope!>mut<!> prop a <!parse_expected_character!>{<!>
       get() {1}
       set(v){}
    }

    public func a(){}
}

main(): Int64 {
    return 0
}
