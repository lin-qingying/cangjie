// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

/*
  @Name:            08_06_02_a02_24

  @Assertion:       8.6.2(2) When an interface extension is in a different `package` from the type being extended, whether the interface extension is exported is determined by the smallest access level of the type used in the interface type and the generic constraints (if any). Other `packages` must import the extended type, the corresponding interface, and the type used by the constraint (if any) to access the extended members contained in the corresponding interface.

  @Description:     Direct extensions are defined in different `package` as the extended type
                    Generic,contains multiple generic constraints
                    Extended type modifier private
                    Extend in a subpackage defined by the interface
                    Access extension members in a subpackage of the extended type
                    All constraint types are modified by public/protected
                    Generic constraint type imported at all
                    Extension implements five interfaces, multiple interfaces have different modifiers
                    Interfaces are imported at all

  @Mode:            run

  @Negative:        no

  @Structure:       complex-main

  @Dependencies:    aux_extend_test_a02_24.cj aux_test_a02_24.cj

  @Author:          l60050068

*/

// DEPENDENCE: ./aux_extend_test_a02_24.cj ./aux_test_a02_24.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt aux_test_a02_24.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt aux_extend_test_a02_24.cj -o b.%middle
// (not CJNATIVE) EXEC: %compiler %cmp_opt a.%middle b.%middle %f -o %output
// (CJNATIVE) EXEC: %compiler %cmp_opt %f b.%middle a.%middle -o %output
// EXEC-PIPE-0: %run %output

package a.b.c.d

import a.b.c.*


main() {
    var myClassImpl = MyClass<Int64>()
    if (1 != myClassImpl.test1()) {
        return 1
    }
    if (2 != myClassImpl.test2()) {
        return 2
    }
    if (3 != myClassImpl.test3()) {
        return 3
    }
    if (5 != myClassImpl.test5()) {
        return 4
    }
    return 0
}