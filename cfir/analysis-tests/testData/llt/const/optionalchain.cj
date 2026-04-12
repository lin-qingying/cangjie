// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f

class B1 {
    let v = Some(B2())
    const init() {
    }
}

class B2 {
    let v = Some(B3())
    const init() {}
}

class B3 {
    let v = Some(B4())
    const init() {}
}

class B4 {
    let a = Some(1)
    const init() {}
}

const func foo() {
    let b = Some(B1())
    return b?.v?.v?.v?.a
}

main() {}

/* SCAN
error: expected 'const' expression
  ==> optionalchain.cj:32:12:
   | 
32 |     return b?.v?.v?.v?.a
   |            ^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
