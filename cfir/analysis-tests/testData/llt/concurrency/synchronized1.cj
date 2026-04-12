// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%compiler %cmp_opt %sync-link %f -o %output 2>&1 | compare %f

internal import std.sync.*

var count: Int64 = 0

class Test
    public var mtx: ReentrantMutex
    public init() {
        this.mtx = ReentrantMutex()
    }

    public func testfunc(): Unit {
        synchronized(mtx) {
            count = count + 1
        }
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: expected '{' or '<', found keyword 'public'
  ==> synchronized1.cj:14:5:
   | 
14 |     public var mtx: ReentrantMutex
   |     ^^^^^^ expected '{' or '<' here
   | 

error: variable in top-level scope must be initialized 
  ==> synchronized1.cj:14:35:
   | 
14 |     public var mtx: ReentrantMutex
   |            ~~~~~~~~~~~~~~~~~~~~~~~^ expected '=' here
   | 

error: unexpected constructor in 'top-level' scope
  ==> synchronized1.cj:15:12:
   | 
15 |     public init() {
   |            ^^^^ unexpected constructor in 'top-level' scope
   | 

error: expected declaration, found '}'
  ==> synchronized1.cj:24:1:
   | 
24 | }
   | ^ expected declaration here
   | 
   # note: only declarations or macro expressions can be used in the top-level

4 errors generated, 4 errors printed.
*/
