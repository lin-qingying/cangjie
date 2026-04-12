// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    if (B<String, Int64>().m != 1) {
        return 1
    }
    return 0
}

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

interface J<T, V> <: I<T> & I<V> {}

class B<T, V> <: I<T> & I<V> {
    public prop m: Int64 {
        get() {
            return foo()
        }
    }
}

/* SCAN
error: interface function 'foo' must be implemented in 'J'
  ==> generic_inheritance_nongeneric_funcParameters.cj:22:1:
   | 
22 | interface J<T, V> <: I<T> & I<V> {}
   | ^ 
   | 

error: interface function 'foo' must be implemented in 'B'
  ==> generic_inheritance_nongeneric_funcParameters.cj:24:1:
   | 
24 | class B<T, V> <: I<T> & I<V> {
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
