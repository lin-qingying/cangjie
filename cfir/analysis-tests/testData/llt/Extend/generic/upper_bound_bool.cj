// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C<T, V> where T <: Bool, V <: Bool {
    func f(a: T) {
        return a && true
    }
    func g(a: T) {
        return a || false
    }
    func g(a: T, b: V) {
        return a && b
    }
    func g(a: T, b: Bool, c: V) {
        return false || a || false || b || false || c || false
    }
}

main() {
    var c = C<Bool, Bool>()
    if (c.f(false)) {
        return 1
    }
    if (!c.g(true)) {
        return 2
    }
    if (!c.g(true, true) && c.g(true, false)) {
        return 3
    }
    if (!c.g(true, false, false)) {
        return 4
    }
    return 0
}

/* SCAN
error: the upper bound 'Bool' of generic parameter 'Generics-T' must be class or interface
 ==> upper_bound_bool.cj:9:9:
  | 
9 | class C<T, V> where T <: Bool, V <: Bool {
  |         ^ 
  | 

error: the upper bound 'Bool' of generic parameter 'Generics-V' must be class or interface
 ==> upper_bound_bool.cj:9:12:
  | 
9 | class C<T, V> where T <: Bool, V <: Bool {
  |            ^ 
  | 

2 errors generated, 2 errors printed.
*/
