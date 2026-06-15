// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>c<!>lass A<<!MULTIPLE_CLASS_UPPER_BOUNDS!>T<!>> <: I where T <: A<T> & A<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>I<!>nt64> {}

main() {}
