// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

package pkgs

public class A{
    public var a: Int64
    public var b: Int64
    public init(x: Int64){
        a = x
        b = x
    }
    <!parse_expected_decl!>publ<!> (index > 1) {
            3
        } else {
            this.a
        }
    }
<!parse_expected_decl!>ic<!> <!parse_expected_character!>operator<!> func [](index: Int64): Int64{
        if (index > 1) {
            3
        } else {
            this.a
        }
    }
    public <!parse_illegal_modifier_in_scope!>operator<!> func []( index: Int64, value!: Int64): Unit{
        this.b = value
    }
<!parse_expected_decl!>}<!>
