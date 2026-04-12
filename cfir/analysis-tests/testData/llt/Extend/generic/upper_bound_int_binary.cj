// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C<T, V> where T <: Int8, V <: Int8 {
    func f(a: T) {
        return a + a
    }
    func g(a: T) {
        return a + 1
    }
    func g(a: T, b: V) {
        return a + b
    }
    func g(a: T, b: Int8, c: V) {
        return 1 + a + 1 + b + 1 + c + 1
    }
}

main() {
    var c = C<Int8, Int8>()
    if (c.f(1) != 2) {
        return 1
    }
    if (c.g(1) != 2) {
        return 2
    }
    if (c.g(1, 2) != 3) {
        return 3
    }
    if (c.g(1, 2, 3) != 10) {
        return 4
    }
    return 0
}

/* SCAN
error: the upper bound 'Int8' of generic parameter 'Generics-T' must be class or interface
 ==> upper_bound_int_binary.cj:9:9:
  | 
9 | class C<T, V> where T <: Int8, V <: Int8 {
  |         ^ 
  | 

error: the upper bound 'Int8' of generic parameter 'Generics-V' must be class or interface
 ==> upper_bound_int_binary.cj:9:12:
  | 
9 | class C<T, V> where T <: Int8, V <: Int8 {
  |            ^ 
  | 

error: invalid binary operator '+' on type 'Generics-T' and 'Generics-T'
  ==> upper_bound_int_binary.cj:11:18:
   | 
11 |         return a + a
   |                  ^ 
   | 
   # note: you may want to implement 'operator func +(right: Generics-T)' for type 'Generics-T'

error: invalid binary operator '+' on type 'Generics-T' and 'Int64'
  ==> upper_bound_int_binary.cj:14:18:
   | 
14 |         return a + 1
   |                  ^ 
   | 
   # note: you may want to implement 'operator func +(right: Int64)' for type 'Generics-T'

error: invalid binary operator '+' on type 'Generics-T' and 'Generics-V'
  ==> upper_bound_int_binary.cj:17:18:
   | 
17 |         return a + b
   |                  ^ 
   | 
   # note: you may want to implement 'operator func +(right: Generics-V)' for type 'Generics-T'

error: invalid binary operator '+' on type 'Int8' and 'Generics-V'
  ==> upper_bound_int_binary.cj:20:34:
   | 
20 |         return 1 + a + 1 + b + 1 + c + 1
   |                                  ^ 
   | 
   # note: you may want to implement 'operator func +(right: Generics-V)' for type 'Int8'

error: no matching function declaration for function call 'g'
  ==> upper_bound_int_binary.cj:29:11:
   | 
29 |     if (c.g(1) != 2) {
   |           ^ 
   | 

error: no matching function declaration for function call 'g'
  ==> upper_bound_int_binary.cj:32:11:
   | 
32 |     if (c.g(1, 2) != 3) {
   |           ^ 
   | 

error: no matching function declaration for function call 'g'
  ==> upper_bound_int_binary.cj:35:11:
   | 
35 |     if (c.g(1, 2, 3) != 10) {
   |           ^ 
   | 

9 errors generated, 9 errors printed.
*/
