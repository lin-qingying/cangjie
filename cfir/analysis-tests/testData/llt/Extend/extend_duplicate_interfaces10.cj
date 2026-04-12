// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix  2>&1 | compare %f

main(): Int64 {
    var a = B<String, Int32>() // ok
    var b = B<String, String>() // error
    return 0
}

interface I<T> {}

class B<T, V> {}

extend<T, V> B<T, V> <: I<T> & I<V> {}

/* SCAN
error: class 'B' inherits or implements duplicate interface 'I'
  ==> extend_duplicate_interfaces10.cj:11:13:
   | 
11 |     var b = B<String, String>() // error
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
