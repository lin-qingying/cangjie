// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

open class c {}
interface i {}
class C<T> <: c & i where T <: c {
    public var c : Int64 = 1
    public var i : Int64 = 1
}
interface I<T> <: i where T <: i {
    let i : Int64 = 1
}

class E<T> <: T { 
    public var T:Int32 = 1
}
class F<T> <: T { 
    public var T: Int32 = 1
}

main() {
    var a = C<c>().c + I<i>.i
    return 0
}

/* SCAN
error: unexpected variable declaration in interface body
  ==> class_ref.cj:16:5:
   | 
15 | interface I<T> <: i where T <: i {
   | ~~~~~~~~~ in interface
16 |     let i : Int64 = 1
   |     ^^^ unexpected variable declaration
   | 

1 error generated, 1 error printed.
*/