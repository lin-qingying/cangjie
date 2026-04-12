// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

public abstract class A {
    public func a(): Int64
}

public abstract class B <: A {
    public func c(): Int64
    public func b(): Int64 {
        0
    }
}

public class C <: B {
    public func c(): Int64 {
        3
    }
}

main() {
    var c = C()
    if (c.c() == 3 && c.b() == 4) {
        print("Pass\n")
    }
    return 0
}

/* SCAN
error: class 'C' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> class15.cj:20:1:
   | 
20 | public class C <: B {
   | ^ 
   | 
note: unimplemented abstract function 'a'
  ==> class15.cj:10:5:
   | 
10 |     public func a(): Int64
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
