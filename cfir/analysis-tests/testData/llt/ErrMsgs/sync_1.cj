// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

internal import std.sync.*

main() {
    let b = synchronized(()) {}
    let c: Int32 = synchronized(false) {
        1;
        true
    } // should raise two error messages: one for the lock object and one for the sync body.

    let mtx: ReentrantMutex = ReentrantMutex()
    let x: Int64 = synchronized(mtx) {
        true
    }
    x + 2 // should not raise an error
    c >> 3 // should not raise an error
    0
}

/* SCAN
error: mismatched types
  ==> sync_1.cj:12:26:
   | 
12 |     let b = synchronized(()) {}
   |                          ^^ expected 'Interface-Lock', found 'Unit'
   | 

error: cannot convert a boolean literal to type 'Interface-Lock'
  ==> sync_1.cj:13:33:
   | 
13 |     let c: Int32 = synchronized(false) {
   |                                 ^ 
   | 

error: cannot convert a boolean literal to type 'Int32'
  ==> sync_1.cj:15:9:
   | 
15 |         true
   |         ^ 
   | 

error: cannot convert a boolean literal to type 'Int64'
  ==> sync_1.cj:20:9:
   | 
20 |         true
   |         ^ 
   | 

warning: class 'ReentrantMutex' is deprecated. Use `public class Mutex` instead.
  ==> sync_1.cj:18:14:
   | 
18 |     let mtx: ReentrantMutex = ReentrantMutex()
   |              ^^^^^^^^^^^^^^ deprecated
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff deprecated`

warning: class 'ReentrantMutex' is deprecated. Use `public class Mutex` instead.
  ==> sync_1.cj:18:31:
   | 
18 |     let mtx: ReentrantMutex = ReentrantMutex()
   |                               ^^^^^^^^^^^^^^ deprecated
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff deprecated`

4 errors generated, 4 errors printed.
2 warnings generated, 2 warnings printed.
*/
