// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    public A (var a:Int64 <!parse_expected_dot_lparen!>=<!> 3, var b:Int64 <!parse_expected_dot_lparen!>=<!>this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

class B {
    public var a :Int64 = 2
    public B (a:Int64 <!parse_expected_dot_lparen!>=<!> 3, var b:Int64 <!parse_expected_dot_lparen!>=<!>this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

open class Parent {
   public var b :Int64 = 3
    public Parent(a:String <!parse_expected_dot_lparen!>=<!> "1", var b :Int64 <!parse_expected_dot_lparen!>=<!> 3) {
     var c: Int64 = 10
     this.b = 10
   }
}

class C {
    public C (var a:Int64 <!parse_expected_dot_lparen!>=<!> 3,  <!parse_member_parameter_after_regular!>b:Int64<!> ) {
     var d: Int64 = a
   }
}

open class D<X,Y> {
    public D<T>( x: Int64, y: X, public var z: Y) {}
}

class E {
     public E (var a:Int64 <!parse_expected_dot_lparen!>=<!> 3, var b:Int64 <!parse_expected_dot_lparen!>=<!> 4 ) { }
     public init (c:Int64 <!parse_expected_dot_lparen!>=<!> 3,  d:Int64 <!parse_expected_dot_lparen!>=<!> 4 ) {
        this.a = c
        this.b = d
     }
}

class F {
    public F (<!parse_illegal_modifier_in_scope!>static<!> var a:Int64) {
   }
}

class G {
    public G (<!parse_illegal_modifier_in_scope!>static<!> private var a:Int64) {
   }
}

main():Int64{
    return 0
}
