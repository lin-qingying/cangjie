// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %cmp_opt  %typecheck %f 2>&1 | compare %f

interface A<X,Y> where X<:A<X,X> & B<Y,Y>, Y<:A<Y,Y> & B<X,X>{}
interface B<X,Y> where X<:A<,X>,X,X>,X,X>,X,X>,X,X>,X,X>,X,X> & B<Y,Y>, Y<:A<Y,Y> & B<X,X>{} 

class C <: A<C,C> & B<C,C>{}

main():Int64{
    return 0
}

/* SCAN
error: expected type name after '<', found ','
  ==> generic_parameters7.cj:10:29:
   | 
10 | interface B<X,Y> where X<:A<,X>,X,X>,X,X>,X,X>,X,X>,X,X>,X,X> & B<Y,Y>, Y<:A<Y,Y> & B<X,X>{} 
   |                            ~^ expected type name here
   |                            |
   |                            after '<'
   | 

error: expected '<:', found ','
  ==> generic_parameters7.cj:10:34:
   | 
10 | interface B<X,Y> where X<:A<,X>,X,X>,X,X>,X,X>,X,X>,X,X>,X,X> & B<Y,Y>, Y<:A<Y,Y> & B<X,X>{} 
   |                                  ^ expected '<:' here
   | 

error: expected '{', found '>'
  ==> generic_parameters7.cj:10:36:
   | 
10 | interface B<X,Y> where X<:A<,X>,X,X>,X,X>,X,X>,X,X>,X,X>,X,X> & B<Y,Y>, Y<:A<Y,Y> & B<X,X>{} 
   |                                    ^ expected '{' here
   | 

3 errors generated, 3 errors printed.
*/
