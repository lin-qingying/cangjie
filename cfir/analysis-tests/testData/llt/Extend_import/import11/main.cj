// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj pkg1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkg1.cj -o pkg1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkg.cj pkg1.%middle -o pkg.%middle
// EXEC: %compiler %cmp_opt %enableO2 %f pkg.%middle pkg1.%middle -o %output 2>&1 | compare %f

internal import <!package_search_error!>pkg1<!>.*

internal import <!package_search_error!>pkg<!>.*

interface I2<T> <: I<T> {}

extend<T3> A<T3> <: I2<T3> {}

extend<T3> B<T3> <: I2<T3> {}

main() {}
