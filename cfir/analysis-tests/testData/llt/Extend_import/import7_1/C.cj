// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: A.cj B.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt A.cj -o A.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt B.cj -o B.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %f 2>&1 | compare %f
// ASSERT: regex interface function 'f' must be implemented in 'extend Foo'

package C

internal import A.Foo
internal import B.I1

public interface I2 {
    func f(): Unit {}
}

extend Foo <: I2 {}
