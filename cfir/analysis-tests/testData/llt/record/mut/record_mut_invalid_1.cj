// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class C1 {
    public <!parse_illegal_modifier_in_scope!>mut<!> func goo() {

    }
}

struct R1 {
    public mut func goo() {

    }

    public mut <!parse_conflict_modifier!>static<!> func foo() {

    }

    public mut operator func +(x: R1, y: R1) {
        return x
    }
}

main() {
    return 1
}
