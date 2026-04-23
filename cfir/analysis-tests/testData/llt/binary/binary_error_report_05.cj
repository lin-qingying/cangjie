// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

// (CJNATIVE) EXEC: %compiler %enableO2 %cmp_opt %n.cj -o %output 2>&1 |compare %f

main(): Int64 {
    var b = Boo()
    b.m = 1
    return b.m
}

class Boo {
    public mut prop m: Int64 {
        get() {
            return m
        }
        set(input) {
            var a = 0;
            <!CONST_EVAL_DIVIDE_BY_ZERO!>m /= a<!>
        }
    }
}
