// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I1<T> <: I3<T> {}

interface I2<K> <: I1<K> {}

interface I3<Q> <: I2<Q> & I2<Int32> {}

main(): Int64 {
    var i3: I3<Int32>
    return 0;
}

/* SCAN
error: inheritance cycle detected: 'I1->I3->I2->I1'
 ==> interface_duplicated_13.cj:9:1:
  | 
9 | interface I1<T> <: I3<T> {}
  | ^ 
  | 

error: interface 'I2' inherits or implements duplicate interface 'I2'
  ==> interface_duplicated_13.cj:13:28:
   | 
13 | interface I3<Q> <: I2<Q> & I2<Int32> {}
   |                            ^ 
   | 

error: interface 'I3' inherits or implements duplicate interface 'I2'
  ==> interface_duplicated_13.cj:16:13:
   | 
16 |     var i3: I3<Int32>
   |             ^ 
   | 

3 errors generated, 3 errors printed.
*/
