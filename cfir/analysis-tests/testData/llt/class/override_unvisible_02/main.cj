// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// ERRCHECK: %compiler %cmp_opt pkg.%middle %n.cj -o %output

import pkg.*

extend B {
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>ff<!>() {
        3
    } // f3. conflict 
}

main() {
    let v = B().ff()
    if (v != 3) {
        return 1
    }
    return 0
}
