// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum bbb {
    center | Month | Day | Hour
}

func aaa(a: Int32) {
    0
}

func aaa(ctx!: Int64, callback!: Option<(Int64, Int64) -> Unit> = Option<(Int64, Int64) -> Unit>.None) {}

func ccc(a: Int16): Int16 {
    1
}

func ccc(a: Int32): Int32 {
    1
}

func ccc(a!: Int32, b!: Int32): Int32 {
    1
}

func Test() {
    aaa(
        ctx: 10,
        callback: Option<(Int64, Int64) -> Unit>.Some(
            {
                start: Int64, end: Int64 =>
                let a = Some<bbb>(bbb.cente)
                return
            }
        )
    )

    aaa(ccc(Int16(1)))
    aaa(ccc(r'a'))
    aaa(ccc(a: r'a', 1))
    aaa(ccc(a: r'a', a: 1))
    aaa(ccc(a: r'a', b: 1))
    return 0
}

main() {
    Test()
    return 10
}

/* SCAN
error: 'cente' is not a member of enum 'bbb'
  ==> depth_call.cj:37:39:
   | 
37 |                 let a = Some<bbb>(bbb.cente)
   |                                       ^^^^^ 
   | 

error: mismatched types
  ==> depth_call.cj:43:9:
   | 
43 |     aaa(ccc(Int16(1)))
   |         ^^^^^^^^^^^^^ expected 'Int32', found 'Int16'
   | 

error: cannot convert a character literal to type 'Int16'
  ==> depth_call.cj:44:13:
   | 
44 |     aaa(ccc(r'a'))
   |             ^ 
   | 

error: positional argument cannot appear after named argument
  ==> depth_call.cj:45:22:
   | 
45 |     aaa(ccc(a: r'a', 1))
   |                      ^ 
   | 

error: named argument prefix 'a:' cannot appeared more than once in call
  ==> depth_call.cj:46:22:
   | 
46 |     aaa(ccc(a: r'a', a: 1))
   |                      ^ 
   | 

error: cannot convert a character literal to type 'Int32'
  ==> depth_call.cj:47:16:
   | 
47 |     aaa(ccc(a: r'a', b: 1))
   |                ^ 
   | 

6 errors generated, 6 errors printed.
*/
