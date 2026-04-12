// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

@Annotation
public open class A { const init() {} }

main() {}

/* SCAN
error: unexpected annotation '@Annotation' on open
  ==> err_open.cj:9:1:
   | 
9  | @Annotation
   | ^^^^^^^^^^^ unexpected annotation here
10 | public open class A { const init() {} }
   |        ~~~~ on open
   | 

1 error generated, 1 error printed.
*/
