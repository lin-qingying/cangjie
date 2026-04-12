// ERRCHECK: %compiler %cmp_opt %f -o %output
// TIMEOUT: 5

const func myTarget(x: Int64) {
    match(x) {
        case 0 => x
        case 1 => x
        case 2 => x
        case 3 => x
        case 4 => x
        case _ => Type
    }
}
@Annotation[target:[(MemberVariable, myTarget(2))[0]]]
public class A {
    const init() {
    }
}
@A
class B {
}
main() {
}

/* SCAN
error: '@A' not applicable to type
  ==> err_const_arg_01.cj:19:1:
   | 
19 | @A
   | ^^ 
   | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
