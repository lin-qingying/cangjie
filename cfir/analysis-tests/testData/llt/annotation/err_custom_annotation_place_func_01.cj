// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

@Annotation
public class A {
    const init() {}
}

@A
class B {
    func f() {
        @A
        func g() {}
        g()
    }
}

main() {
    B().f()
}

/* SCAN
error: cannot use custom annotation
  ==> err_custom_annotation_place_func_01.cj:18:14:
   | 
17 |         @A
   |         ~~ 
18 |         func g() {}
   |              ^ 
   | 
   # note: custom annotations can only be used on non-local declarations

1 error generated, 1 error printed.
*/
