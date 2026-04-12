// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt %n.cj pkg.%middle 2>&1 | compare %f

internal import pkg.*

class C1 <: C {
    func h() {
        f()
        g()
        p = 0
        p
        q = 0
        q
        0
    }
}

main() {
    0
}

// ASSERT: scan undeclared identifier 'f'
// (not CJNATIVE) ASSERT: regex .*error.*: no matching function declaration for function call 'g'
// (CJNATIVE) ASSERT: scan undeclared identifier 'g'
// ASSERT: scan undeclared identifier 'p'
// (not CJNATIVE) ASSERT: regex .*error.*: can not access field 'q'
// (CJNATIVE)ASSERT: scan undeclared identifier 'q'
// ASSERT: regex 6 errors generated, 6 errors printed
