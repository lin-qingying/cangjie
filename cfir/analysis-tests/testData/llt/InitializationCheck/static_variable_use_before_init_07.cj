// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A<T> {
    static prop test: Int64 {
        get() {
            c
        }
    }
    mut static prop foo: Int64 {
        get() {
            1
        }
        set(v) {
            b
        }
    }
    static var b: Int64
    static var c: Int64 = test
    static init() {
        b = if (true) {
            foo = 2
            foo
        } else {
            2
        }
    }
}

main() {}

/* SCAN
error: static member cannot depend on generic parameter 'Generics-T'
  ==> static_variable_use_before_init_07.cj:24:27:
   | 
24 |     static var c: Int64 = test
   |                           ^^^^ 
   | 
note: generic argument declaration here
 ==> static_variable_use_before_init_07.cj:9:9:
  | 
9 | class A<T> {
  |         ^ 
  | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> static_variable_use_before_init_07.cj:27:13:
   | 
27 |             foo = 2
   |             ^^^ 
   | 
note: generic argument declaration here
 ==> static_variable_use_before_init_07.cj:9:9:
  | 
9 | class A<T> {
  |         ^ 
  | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> static_variable_use_before_init_07.cj:28:13:
   | 
28 |             foo
   |             ^^^ 
   | 
note: generic argument declaration here
 ==> static_variable_use_before_init_07.cj:9:9:
  | 
9 | class A<T> {
  |         ^ 
  | 

error: global/static variable 'b' is used before initialization during initializing 'b'
  ==> static_variable_use_before_init_07.cj:20:13:
   | 
20 |             b
   |             ^ 
   | 

error: global/static variable 'c' is used before initialization during initializing 'c'
  ==> static_variable_use_before_init_07.cj:12:13:
   | 
12 |             c
   |             ^ 
   | 

5 errors generated, 5 errors printed.
*/
