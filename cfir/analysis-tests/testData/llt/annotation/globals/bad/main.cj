// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt

@Annotation[target: [GlobalFunction]]
public class APILevel {
    public const init() {}
}

@APILevel
main() {}

/* SCAN
error: unexpected annotation '@APILevel' on main
  ==> main.cj:14:1:
   | 
14 | @APILevel
   | ^^^^^^^^^ unexpected annotation here
15 | main() {}
   | ~~~~ on main
   | 

1 error generated, 1 error printed.
*/
