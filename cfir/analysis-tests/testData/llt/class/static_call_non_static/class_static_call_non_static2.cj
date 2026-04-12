// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class Base {
    public var x: Int32 = 1
    public static var test: Int32 = 2
    public func f(): Int32 {
        return 1
    }
    public static func f1(): Int32 {
        return 1
    }
    public static func get(): Int32 {
        func a(x1!: Int32 = f(), x2!: Int32 = x): Int32 {
            f()
            f1()
            test
            x
            var lambda1 = {=> f()}
            return f()
        }
        var lambda = {=> f()}
        return a()
    }
    public static var lam = {
        =>
        f();
        0
    }
    public static var lam1 = {
        =>
        x;
        0
    }
}

/* SCAN
error: 'f' is non-static member, cannot be accessed by static function 'a'
  ==> class_static_call_non_static2.cj:19:29:
   | 
19 |         func a(x1!: Int32 = f(), x2!: Int32 = x): Int32 {
   |                             ^ 
   | 

error: 'f' is non-static member, cannot be accessed by static function 'a'
  ==> class_static_call_non_static2.cj:20:13:
   | 
20 |             f()
   |             ^ 
   | 

error: invalid use of non-static member 'f' in static lambda expression
  ==> class_static_call_non_static2.cj:24:31:
   | 
24 |             var lambda1 = {=> f()}
   |                               ^ 
   | 

error: 'f' is non-static member, cannot be accessed by static function 'a'
  ==> class_static_call_non_static2.cj:25:20:
   | 
25 |             return f()
   |                    ^ 
   | 

error: invalid use of non-static member 'f' in static lambda expression
  ==> class_static_call_non_static2.cj:27:26:
   | 
27 |         var lambda = {=> f()}
   |                          ^ 
   | 

error: invalid use of non-static member 'f' in static lambda expression
  ==> class_static_call_non_static2.cj:32:9:
   | 
32 |         f();
   |         ^ 
   | 

error: 'x' is non-static member, cannot be accessed by static function 'a'
  ==> class_static_call_non_static2.cj:19:47:
   | 
19 |         func a(x1!: Int32 = f(), x2!: Int32 = x): Int32 {
   |                                               ^ 
   | 

error: 'x' is non-static member, cannot be accessed by static function 'a'
  ==> class_static_call_non_static2.cj:23:13:
   | 
23 |             x
   |             ^ 
   | 

error: invalid use of non-static member 'x' in static lambda expression
  ==> class_static_call_non_static2.cj:37:9:
   | 
37 |         x;
   |         ^ 
   | 

error: 'main' is missing
 ==> class_static_call_non_static2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

10 errors generated, 10 errors printed.
*/
