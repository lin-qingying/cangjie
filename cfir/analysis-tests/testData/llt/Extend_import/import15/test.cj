// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// DEPENDENCE: a.cj
// EXEC-PIPE-1: %compiler %cmp_opt a.cj %f -o %output 2>&1 | compare %f

package test

main(): Int64 {
    let a: <!sema_undeclared_type_name!>F<!>oos = <!sema_undeclared_identifier!>F<!>oos()

    a.foos.toArray()
    a.foos.contains(Foo("1"))
    return 0
}
