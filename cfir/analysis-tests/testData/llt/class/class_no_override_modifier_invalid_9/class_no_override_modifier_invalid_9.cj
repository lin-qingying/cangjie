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

class D<T> <: A<T> & B<T> {}

main() {
    return 1
}

/* SCAN
error: interface function 'foo' must be implemented in 'D'
  ==> class_no_override_modifier_invalid_9.cj:19:1:
   | 
19 | class D<T> <: A<T> & B<T> {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
