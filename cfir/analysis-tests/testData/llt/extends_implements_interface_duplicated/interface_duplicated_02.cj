// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I0<T> {}

class A<Q> <: I0<Q> & I0<Int64> {}

main() {
    var b: A<Int64>
    return 0
}

/* SCAN
error: class 'A' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_02.cj:14:12:
   | 
14 |     var b: A<Int64>
   |            ^ 
   | 

1 error generated, 1 error printed.
*/
