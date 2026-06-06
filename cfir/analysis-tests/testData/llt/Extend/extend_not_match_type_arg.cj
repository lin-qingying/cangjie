// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// If the type being extended is a generic type, the same generic variant declaration needs to be maintained.

class A<T, R> { }
extend<T, T> A<T, T> { }
extend<T> A<T> { }
extend A { }
extend<T, R, P> A<T, R, P> { }
extend<<!parse_expected_name!>Int64<!>, R> A<Int64, R> { }

class B { }
extend<T> B<T> { }
extend<<!parse_expected_name!>Int64<!>> B<Int64> { }
extend<T, <!parse_expected_name!>Int64<!>> B<T, Int64> { }
