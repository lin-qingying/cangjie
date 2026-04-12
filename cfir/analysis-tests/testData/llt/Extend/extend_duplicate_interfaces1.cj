// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface Foo {}

class A {}

extend A <: Foo {}

extend A <: Foo {}

main(): Int64 {
    return 1
}

/* SCAN
error: interface 'Interface-Foo' has been implemented by 'Class-A', please remove it
  ==> extend_duplicate_interfaces1.cj:15:13:
   | 
15 | extend A <: Foo {}
   |             ^^^ 
   | 

1 error generated, 1 error printed.
*/
