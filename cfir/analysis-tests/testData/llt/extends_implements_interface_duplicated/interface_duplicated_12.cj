// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I<T> {}

class A <: I<Bool> & I<Bool> {}

main(): Int64 {
    return 0;
}

/* SCAN
error: class 'A' inherits or implements duplicate interface 'I'
  ==> interface_duplicated_12.cj:11:1:
   | 
11 | class A <: I<Bool> & I<Bool> {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
