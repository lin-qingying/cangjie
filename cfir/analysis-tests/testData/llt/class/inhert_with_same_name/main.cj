// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// DEPENDENCE: testa.cj, testb.cj
// EXEC: %compiler testa.cj -o testa.%middle %compile_lib_opt
// EXEC: %compiler testb.cj -o testb.%middle %compile_lib_opt
// (CJNATIVE) EXEC-0: %compiler  %f testa.%middle testb.%middle

package c

import b

public class A <: b.A {
    public func some() {
        let dd: b.My = st()
    }
}

main() {}
