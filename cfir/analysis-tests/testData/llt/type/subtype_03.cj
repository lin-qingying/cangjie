// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC: %compiler %cmp_opt %f -o %n.%suffix
// (CJNATIVE)RUN-EXEC: %run %run_opt %output %run_args
// (not CJNATIVE)ERRCHECK: %compiler %f %cmp_opt

interface I {}

class A {}

extend A <: I {}

main() {
    let a = (A(), A())
    let a1: (I, I) = a

    let b = {x: I => A()}
    let b1: (A) -> I = b
}

/* SCAN
error: mismatched types
  ==> subtype_03.cj:11:22:
   |
11 |     let a1: (I, I) = a
   |                      ^ expected 'Tuple<Interface-I, Interface-I>', found 'Tuple<Class-A, Class-A>'
   |

error: mismatched types
  ==> subtype_03.cj:14:22:
   |
14 |     let b1: (A)->I = b
   |                      ^ expected '(Class-A) -> Interface-I', found '(Interface-I) -> Class-A'
   |

2 errors generated, 2 errors printed.
 */
