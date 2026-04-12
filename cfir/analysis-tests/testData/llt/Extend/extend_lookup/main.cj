// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o testa.%middle
// EXEC: %compiler %cmp_opt testa.%middle %f -o %output
// EXEC: %run %run_opt %output %run_args

import pkg1.*

main() {
    Int64.f<Int16>(1)
    1.g()
    A.f<Int16>(1)
    A().g()

    return 0
}
