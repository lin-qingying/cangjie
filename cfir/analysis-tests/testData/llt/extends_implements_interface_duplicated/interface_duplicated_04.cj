// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

class A<Q> <: I1<Q> {
    public func doit(): Int64 {
        return 5
    }
}

main() {
    var j = A<Rune>()
    return 0
}

/* SCAN
error: class 'A' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_04.cj:20:13:
   | 
20 |     var j = A<Rune>()
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
