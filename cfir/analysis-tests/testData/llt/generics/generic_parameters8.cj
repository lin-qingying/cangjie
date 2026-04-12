// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

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

interface J<T, Y> <: I<T> & I<V> where T <: String, V <: Int64 {
    prop m: Int64 {
        get() {
            return foo()
        }
    }
}

class B<T, V> <: J<T> where T <: String, V <: Int64 {}

/* SCAN
error: undeclared type name 'V'
  ==> generic_parameters8.cj:22:31:
   | 
22 | interface J<T, Y> <: I<T> & I<V> where T <: String, V <: Int64 {
   |                               ^ 
   | 

error: undeclared type name 'V'
  ==> generic_parameters8.cj:22:53:
   | 
22 | interface J<T, Y> <: I<T> & I<V> where T <: String, V <: Int64 {
   |                                                     ^ 
   | 

error: the upper bound 'Struct-String' of generic parameter 'Generics-T' must be class or interface
  ==> generic_parameters8.cj:22:13:
   | 
22 | interface J<T, Y> <: I<T> & I<V> where T <: String, V <: Int64 {
   |             ^ 
   | 

error: the upper bound 'Struct-String' of generic parameter 'Generics-T' must be class or interface
  ==> generic_parameters8.cj:30:9:
   | 
30 | class B<T, V> <: J<T> where T <: String, V <: Int64 {}
   |         ^ 
   | 

error: the upper bound 'Int64' of generic parameter 'Generics-V' must be class or interface
  ==> generic_parameters8.cj:30:12:
   | 
30 | class B<T, V> <: J<T> where T <: String, V <: Int64 {}
   |            ^ 
   | 

error: interface 'J' can only inherit interface
  ==> generic_parameters8.cj:22:1:
   | 
22 | interface J<T, Y> <: I<T> & I<V> where T <: String, V <: Int64 {
   | ^ 
   | 

error: type argument's number does not match type parameter's number
  ==> generic_parameters8.cj:30:18:
   | 
30 | class B<T, V> <: J<T> where T <: String, V <: Int64 {}
   |                  ^^^^ 
   | 

7 errors generated, 7 errors printed.
*/
