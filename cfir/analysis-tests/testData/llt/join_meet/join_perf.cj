// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I80 {}

interface I79 <: I80 {}

interface I78 <: I80 {}

interface I77 <: I78 & I79 {}

interface I76 <: I78 & I79 {}

interface I75 <: I76 & I77 {}

interface I74 <: I76 & I77 {}

interface I73 <: I74 & I75 {}

interface I72 <: I74 & I75 {}

interface I71 <: I72 & I73 {}

interface I70 <: I72 & I73 {}

interface I69 <: I70 & I71 {}

interface I68 <: I70 & I71 {}

interface I67 <: I68 & I69 {}

interface I66 <: I68 & I69 {}

interface I65 <: I66 & I67 {}

interface I64 <: I66 & I67 {}

interface I63 <: I64 & I65 {}

interface I62 <: I64 & I65 {}

interface I61 <: I62 & I63 {}

interface I60 <: I62 & I63 {}

interface I59 <: I60 & I61 {}

interface I58 <: I60 & I61 {}

interface I57 <: I58 & I59 {}

interface I56 <: I58 & I59 {}

interface I55 <: I56 & I57 {}

interface I54 <: I56 & I57 {}

interface I53 <: I54 & I55 {}

interface I52 <: I54 & I55 {}

interface I51 <: I52 & I53 {}

interface I50 <: I52 & I53 {}

interface I49 <: I50 & I51 {}

interface I48 <: I50 & I51 {}

interface I47 <: I48 & I49 {}

interface I46 <: I48 & I49 {}

interface I45 <: I46 & I47 {}

interface I44 <: I46 & I47 {}

interface I43 <: I44 & I45 {}

interface I42 <: I44 & I45 {}

interface I41 <: I42 & I43 {}

interface I40 <: I42 & I43 {}

interface I39 <: I40 & I41 {}

interface I38 <: I40 & I41 {}

interface I37 <: I38 & I39 {}

interface I36 <: I38 & I39 {}

interface I35 <: I36 & I37 {}

interface I34 <: I36 & I37 {}

interface I33 <: I34 & I35 {}

interface I32 <: I34 & I35 {}

interface I31 <: I32 & I33 {}

interface I30 <: I32 & I33 {}

interface I29 <: I30 & I31 {}

interface I28 <: I30 & I31 {}

interface I27 <: I28 & I29 {}

interface I26 <: I28 & I29 {}

interface I25 <: I26 & I27 {}

interface I24 <: I26 & I27 {}

interface I23 <: I24 & I25 {}

interface I22 <: I24 & I25 {}

interface I21 <: I22 & I23 {}

interface I20 <: I22 & I23 {}

interface I19 <: I20 & I21 {}

interface I18 <: I20 & I21 {}

interface I17 <: I18 & I19 {}

interface I16 <: I18 & I19 {}

interface I15 <: I16 & I17 {}

interface I14 <: I16 & I17 {}

interface I13 <: I14 & I15 {}

interface I12 <: I14 & I15 {}

interface I11 <: I12 & I13 {}

interface I10 <: I12 & I13 {}

interface I9 <: I10 & I11 {}

interface I8 <: I10 & I11 {}

interface I7 <: I8 & I9 {}

interface I6 <: I8 & I9 {}

interface I5 <: I6 & I7 {}

interface I4 <: I6 & I7 {}

interface I3 <: I4 & I5 {}

interface I2 <: I4 & I5 {}

interface I1 <: I2 & I3 {}

interface I0 <: I2 & I3 {}

class C1 <: I0 & I1 {}

class C2 <: I0 & I1 {}

main() {
    if (C1() is C2) {
        C1() as I0
    } else {
        C2() as I1
    }
    ()
}
