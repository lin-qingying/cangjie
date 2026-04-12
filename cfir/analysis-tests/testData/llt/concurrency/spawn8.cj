// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

class MyCustomThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
}

interface MyInterfaceThreadContext <: ThreadContext {
    func custom(): Unit
}

class MyExtendThreadContext {}

extend MyExtendThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
}

main(): Int64 {
    let fut = spawn (MyCustomThreadContext()) {}
    return 0
}

/* SCAN
error: user defined decl 'MyCustomThreadContext' not support to inherit, implement or extend 'ThreadContext'
  ==> spawn8.cj:9:1:
   | 
9  |   class MyCustomThreadContext <: ThreadContext {
   |  _^
10 | |     public func end(): Unit {}
   | | ...
14 | | }
   | |_^
   | 

error: user defined decl 'MyInterfaceThreadContext' not support to inherit, implement or extend 'ThreadContext'
  ==> spawn8.cj:16:1:
   | 
16 |   interface MyInterfaceThreadContext <: ThreadContext {
   |  _^
17 | |     func custom(): Unit
18 | | }
   | |_^
   | 

error: user defined decl 'MyExtendThreadContext' not support to inherit, implement or extend 'ThreadContext'
  ==> spawn8.cj:20:1:
   | 
20 | class MyExtendThreadContext {}
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: invalid argument of spawn expr, user-defined `ThreadContext` types are prohibited now
  ==> spawn8.cj:30:22:
   | 
30 |     let fut = spawn (MyCustomThreadContext()) {}
   |                      ^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
