// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public func g(): Int64 { // g1
        func g(): Bool {
            return true
        } // g2

        // g2 shadows g1, only 1 candidate here
        let x: Int64 = g()
        let y: () -> Int64 = g
        return 1
    }
    public func f(): Int32 {
        return 0
    }
    public func aaa(): Int64 {
        let x: () -> Int64 = f
        return f()
    }
}

func f(): Int64 { // f1
    func f(): Bool {
        return true
    } // f2

    // f2 shadows f1, only 1 candidate here
    let x: Int64 = f()
    let y: () -> Int64 = f
    return 1
}

main(): Int64 {
    return 0
}

/* SCAN
error: mismatched types
  ==> call25.cj:16:24:
   | 
16 |         let x: Int64 = g()
   |                        ^^^ expected 'Int64', found 'Bool'
   | 

error: mismatched types
  ==> call25.cj:17:30:
   | 
17 |         let y: () -> Int64 = g
   |                              ^ expected '() -> Int64', found '() -> Bool'
   | 

error: mismatched types
  ==> call25.cj:24:30:
   | 
24 |         let x: () -> Int64 = f
   |                              ^ expected '() -> Int64', found '() -> Int32'
   | 

error: mismatched types
  ==> call25.cj:25:16:
   | 
25 |         return f()
   |                ^^^ expected 'Int64', found 'Int32'
   | 

error: mismatched types
  ==> call25.cj:35:20:
   | 
35 |     let x: Int64 = f()
   |                    ^^^ expected 'Int64', found 'Bool'
   | 

error: mismatched types
  ==> call25.cj:36:26:
   | 
36 |     let y: () -> Int64 = f
   |                          ^ expected '() -> Int64', found '() -> Bool'
   | 

6 errors generated, 6 errors printed.
*/
