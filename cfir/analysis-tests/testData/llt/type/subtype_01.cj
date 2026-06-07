// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

<!sema_generic_type_argument_not_match_constraint!>c<!>lass A<<!sema_multiple_class_upperbounds!>T<!>> <: I where T <: A<T> & A<<!sema_generic_type_argument_not_match_constraint!>I<!>nt64> {}

main() {}
