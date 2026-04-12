// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt -Woff all

@Annotation
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
type P<T> = Array<T>
@APILevel[12] @APILevel[255]
type Rk = UIntNative

main() {}

/* SCAN
error: unexpected parameters for macro invocation here
  ==> type.cj:18:1:
   | 
18 | type P<T> = Array<T>
   | ^ expected declaration like: function, enum, class, interface, variable, property, extend ...
   | 

error: unexpected parameters for macro invocation here
  ==> type.cj:20:1:
   | 
20 | type Rk = UIntNative
   | ^ expected declaration like: function, enum, class, interface, variable, property, extend ...
   | 

2 errors generated, 2 errors printed.
*/
