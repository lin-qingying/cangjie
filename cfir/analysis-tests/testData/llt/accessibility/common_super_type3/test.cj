// DEPENDENCE: base.cj derived.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt base.cj -o libpkg.base.a
// EXEC: %compiler %cmp_opt %compile_lib_opt derived.cj -o libpkg.derived.a
// EXEC-PIPE-0: %compiler %cmp_opt %compile_lib_opt %diag_json %f -o libpkg.derived.sub.a 2>&1 | compare %f


package pkg.derived.sub

import pkg.derived.Derived

public class SubDerived <: Derived {
    public func print(): Unit {
        println(value)
    }
}

// ASSERT: scan-not sema_ambiguous_use

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
