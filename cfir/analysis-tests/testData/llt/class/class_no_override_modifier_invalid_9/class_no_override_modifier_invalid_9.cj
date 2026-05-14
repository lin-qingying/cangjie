// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg/pkg.%middle
// EXEC-PIPE-1: %compiler %cmp_opt pkg/pkg.%middle %n.cj --import-path pkg 2>&1 | compare %f

internal import pkg.*

interface B<T> {
    func foo(b: T): T {
        return b
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<T><!> <: A<T> & B<T> {}

main() {
    return 1
}
