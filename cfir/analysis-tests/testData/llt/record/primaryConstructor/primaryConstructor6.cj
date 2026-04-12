// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    return run()
}

struct Test {
    public let width2: Rune
    public let length2: Rune

    Test(width: Int64, length: Int64) {
        width2 = width
        length2 = length
    }
}

func run(): Int64 {
    Test(2, 3)
    return 1
}

/* SCAN
error: mismatched types
  ==> primaryConstructor6.cj:18:18:
   | 
18 |         width2 = width
   |                  ^^^^^ expected 'Rune', found 'Int64'
   | 

error: mismatched types
  ==> primaryConstructor6.cj:19:19:
   | 
19 |         length2 = length
   |                   ^^^^^^ expected 'Rune', found 'Int64'
   | 

2 errors generated, 2 errors printed.
*/
