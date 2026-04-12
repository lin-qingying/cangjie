// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface Foo {}

open class A <: Foo {}

class B <: A {}

extend B <: Foo {}

main(): Int64 {
    return 1
}

/* SCAN
error: interface 'Interface-Foo' has been implemented by 'Class-B', please remove it
  ==> extend_duplicate_interfaces3.cj:15:13:
   | 
15 | extend B <: Foo {}
   |             ^^^ 
   | 

1 error generated, 1 error printed.
*/
