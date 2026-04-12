// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

@Annotation
public sealed class A { const init() {} }

main() {}

/* SCAN
error: unexpected annotation '@Annotation' on sealed
  ==> err_sealed.cj:9:1:
   | 
9  | @Annotation
   | ^^^^^^^^^^^ unexpected annotation here
10 | public sealed class A { const init() {} }
   |        ~~~~~~ on sealed
   | 

warning: redundant modifier: 'public'
  ==> err_sealed.cj:10:1:
   | 
10 | public sealed class A { const init() {} }
   | ^^^^^^ ~~~~~~ 
   | |
   | 'sealed' implies 'public'
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff parser`

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
