// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum TimeUnit2<T> where U <: Num { Year2 }
enum TimeUnit2<T> where U <: Num { Year2 }
enum TimeUnit2<T> where U <!parse_expected_character!><<!> Num { Year2 }
enum TimeUnit2<T> where U <: <!parse_expected_type!>where<!> { Year2 }
