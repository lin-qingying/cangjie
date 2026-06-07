// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {
    public A (var a:Int64 <!parse_expected_dot_lparen!>=<!> 3, var b:Int64 <!parse_expected_dot_lparen!>=<!>this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

struct B {
    public var a :Int64 = 2
    public B (a:Int64 <!parse_expected_dot_lparen!>=<!> 3, var b:Int64 <!parse_expected_dot_lparen!>=<!>this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

struct Parent {
   public var b :Int64 = 3
    public Parent(a:String <!parse_expected_dot_lparen!>=<!> "1", var b :Int64 <!parse_expected_dot_lparen!>=<!> 3) {
     var c: Int64 = 10
     this.b = 10
   }
}

struct C {
    public C (var a!:Int64 = 3,  <!parse_named_parameter_after_unnamed!>b:Int64<!> ) {
     var d: Int64 =a
   }
}

struct B<X,Y> {
    public B<T>( x: Int64, y: X, public var z: Y) {}
    public init<T>() {}
}

main():Int64{
    return 0
}
