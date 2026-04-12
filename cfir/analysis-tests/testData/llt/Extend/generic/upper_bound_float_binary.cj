// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C<T, V> where T <: Float32, V <: Float32 {
    func f(a: T) {
        return a + a
    }
    func g(a: T) {
        return a + 1.0
    }
    func g(a: T, b: V) {
        return a + b
    }
    func g(a: T, b: Float32, c: V) {
        return 1.0 + a + 1.0 + b + 1.0 + c + 1.0
    }
}

main() {
    var c = C<Float32, Float32>()
    if (c.f(1.0) < 1.999 || c.f(1.0) > 2.001) {
        return 1
    }
    if (c.g(1.0) < 1.999 || c.g(1.0) > 2.001) {
        return 2
    }
    if (c.g(1.0, 2.0) < 2.999 || c.g(1.0, 2.0) > 3.001) {
        return 3
    }
    if (c.g(1.0, 2.0, 3.0) < 9.999 || c.g(1.0, 2.0, 3.0) > 10.001) {
        return 4
    }
    return 0
}

/* SCAN
error: the upper bound 'Float32' of generic parameter 'Generics-T' must be class or interface
 ==> upper_bound_float_binary.cj:9:9:
  | 
9 | class C<T, V> where T <: Float32, V <: Float32 {
  |         ^ 
  | 

error: the upper bound 'Float32' of generic parameter 'Generics-V' must be class or interface
 ==> upper_bound_float_binary.cj:9:12:
  | 
9 | class C<T, V> where T <: Float32, V <: Float32 {
  |            ^ 
  | 

error: invalid binary operator '+' on type 'Generics-T' and 'Generics-T'
  ==> upper_bound_float_binary.cj:11:18:
   | 
11 |         return a + a
   |                  ^ 
   | 
   # note: you may want to implement 'operator func +(right: Generics-T)' for type 'Generics-T'

error: invalid binary operator '+' on type 'Generics-T' and 'Float64'
  ==> upper_bound_float_binary.cj:14:18:
   | 
14 |         return a + 1.0
   |                  ^ 
   | 
   # note: you may want to implement 'operator func +(right: Float64)' for type 'Generics-T'

error: invalid binary operator '+' on type 'Generics-T' and 'Generics-V'
  ==> upper_bound_float_binary.cj:17:18:
   | 
17 |         return a + b
   |                  ^ 
   | 
   # note: you may want to implement 'operator func +(right: Generics-V)' for type 'Generics-T'

error: invalid binary operator '+' on type 'Float32' and 'Generics-V'
  ==> upper_bound_float_binary.cj:20:40:
   | 
20 |         return 1.0 + a + 1.0 + b + 1.0 + c + 1.0
   |                                        ^ 
   | 
   # note: you may want to implement 'operator func +(right: Generics-V)' for type 'Float32'

error: no matching function declaration for function call 'g'
  ==> upper_bound_float_binary.cj:29:11:
   | 
29 |     if (c.g(1.0) < 1.999 || c.g(1.0) > 2.001) {
   |           ^ 
   | 

error: no matching function declaration for function call 'g'
  ==> upper_bound_float_binary.cj:32:11:
   | 
32 |     if (c.g(1.0, 2.0) < 2.999 || c.g(1.0, 2.0) > 3.001) {
   |           ^ 
   | 

error: no matching function declaration for function call 'g'
  ==> upper_bound_float_binary.cj:35:11:
   | 
35 |     if (c.g(1.0, 2.0, 3.0) < 9.999 || c.g(1.0, 2.0, 3.0) > 10.001) {
   |           ^ 
   | 

9 errors generated, 9 errors printed.
*/
