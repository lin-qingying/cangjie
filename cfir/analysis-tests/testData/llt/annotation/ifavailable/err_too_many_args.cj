// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt
func f1() {
    @IfAvailable(APILevel:11, { => println(0) }, { => println(0) }, { => println(0) })
}

main() {
}

/* SCAN
error: unclosed delimiter: '('
 ==> err_too_many_args.cj:9:67:
  | 
9 |     @IfAvailable(APILevel:11, { => println(0) }, { => println(0) }, { => println(0) })
  |                 ~                                                 ^ expected ')' here
  |                 |
  |                 to match this opening '('
  | 

1 error generated, 1 error printed.
*/
