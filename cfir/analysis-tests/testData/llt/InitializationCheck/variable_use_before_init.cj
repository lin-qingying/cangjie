// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

class Aoo {
    public var a: Int32 = 1
    public let b: Int32 = 2
    public var c = if (true) {
        func foo1(x!: Int32 = a) {
            return x
        }
        func foo2() {
            var x = a;
            return x
        }
        func foo3() {
            return a
        }
        func foo4() {
            a;
            return 1
        }
        func foo5() {
            a
        }

        func goo1() {
            var a = foo1
            return a
        }
        func goo2() {
            var a = foo2
            return a
        }
        func goo3() {
            var a = foo3
            return a
        }
        func goo4() {
            var a = foo4
            return a
        }
        func goo5() {
            var a = foo5
            return a
        }
    }
}

/* SCAN
error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> variable_use_before_init.cj:17:31:
   | 
17 |         func foo1(x!: Int32 = a) {
   |                               ^ 
   | 

error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> variable_use_before_init.cj:21:21:
   | 
21 |             var x = a;
   |                     ^ 
   | 

error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> variable_use_before_init.cj:25:20:
   | 
25 |             return a
   |                    ^ 
   | 

error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> variable_use_before_init.cj:28:13:
   | 
28 |             a;
   |             ^ 
   | 

error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> variable_use_before_init.cj:32:13:
   | 
32 |             a
   |             ^ 
   | 

5 errors generated, 5 errors printed.
*/
