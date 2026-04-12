// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {}

class B<T> {}

class C {
    public var list: B<A> = B<A>()
    public func onInit() {
        let data = [A(), A()]
        this.list = data
    }
}

/* SCAN
error: mismatched types
  ==> assign_003.cj:17:21:
   | 
17 |         this.list = data
   |                     ^^^^ expected 'Class-B<Class-A>', found 'Struct-Array<Class-A>'
   | 

1 error generated, 1 error printed.
*/
