// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

interface I2<Q> <: I1<Q> & I0<Int64> {}

interface I3<P> <: I2<Int32> {}

main() {
    var i2: I2<Int64>
    var i1: I1<Rune>
    return 0
}

/* SCAN
error: interface 'I1' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_07.cj:19:13:
   | 
19 |     var i1: I1<Rune>
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
