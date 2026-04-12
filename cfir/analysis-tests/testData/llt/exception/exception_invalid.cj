// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    try {
    } catch ((a, b)) {
        println(a, b)
    }
}

/* SCAN
error: expected wildcard or exception type pattern, found '('
  ==> exception_invalid.cj:11:14:
   | 
11 |     } catch ((a, b)) {
   |              ^ expected wildcard or exception type pattern here
   | 

error: unmatched delimiter: ')'
  ==> exception_invalid.cj:11:20:
   | 
11 |     } catch ((a, b)) {
   |                    ^ unmatched delimiter
   | 

error: expected expression or declaration, found '}'
  ==> exception_invalid.cj:14:1:
   | 
14 | }
   | ^ expected expression or declaration here
   | 

3 errors generated, 3 errors printed.
*/
