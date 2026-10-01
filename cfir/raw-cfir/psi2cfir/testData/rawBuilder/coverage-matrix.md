# rawBuilder Coverage Matrix

Format:
`<PSI/Scenario>`: relative/path1.cj, relative/path2.cj

All paths are relative to `cfir/raw-cfir/psi2cfir/testData/rawBuilder/`.

- `Group/cangjie-features/extend`: cangjie-features/extend/extendDeclaration.cj, cangjie-features/extend/extendGenericWhereChain.cj, cangjie-features/extend/extendWithWhereOfficial.cj
- `Group/cangjie-features/effects`: cangjie-features/effects/effectTryHandleOfficial.cj
- `Group/cangjie-features/match`: cangjie-features/match/matchExpressionOfficial.cj, cangjie-features/match/matchRichPatternsOfficial.cj, cangjie-features/match/matchVarOrEnumPatternOfficial.cj
- `Group/cangjie-features/spawn`: cangjie-features/spawn/spawnExpressionOfficial.cj
- `Group/cangjie-features/varray`: cangjie-features/varray/varrayTypeRef.cj
- `Group/control-flow/valid`: control-flow/valid/breakAndContinue.cj, control-flow/valid/controlFlow.cj, control-flow/valid/doWhileLoop.cj, control-flow/valid/forWithPatternGuard.cj
- `Group/declarations/class-like`: declarations/class-like/accessControlMemberModifiers.cj, declarations/class-like/accessControlVisibilityDisplay.cj, declarations/class-like/classMembersOrderStability.cj, declarations/class-like/classWithMembers.cj, declarations/class-like/classWithModifiers.cj, declarations/class-like/classWithSuperReference.cj, declarations/class-like/classWithSupertype.cj, declarations/class-like/classWithThisReference.cj, declarations/class-like/classWithTypeParameters.cj, declarations/class-like/emptyClass.cj, declarations/class-like/enumDeclaration.cj, declarations/class-like/genericWhereTypeDeclarations.cj, declarations/class-like/interfaceDeclaration.cj, declarations/class-like/publicVisibilityDisplay.cj, declarations/class-like/structDeclaration.cj
- `Group/declarations/file-structure`: declarations/file-structure/emptyFile.cj, declarations/file-structure/featuresDirective.cj, declarations/file-structure/packageAndImport.cj
- `Group/declarations/top-level`: declarations/top-level/genericWhereFunction.cj, declarations/top-level/localScopeInNestedBlock.cj, declarations/top-level/localScopeVisibility.cj, declarations/top-level/mainEntryOfficial.cj, declarations/top-level/topLevelFunction.cj, declarations/top-level/topLevelProperty.cj, declarations/top-level/typeAlias.cj
- `Group/expressions/basics`: expressions/basics/arrayAndTupleLiterals.cj, expressions/basics/dotQualifiedAccess.cj, expressions/basics/functionExpressions.cj, expressions/basics/isTypeCheckExpression.cj, expressions/basics/opAndIfExpressions.cj, expressions/basics/optionalChainAccess.cj, expressions/basics/rangeExpression.cj, expressions/basics/specialExpressionsPreserve.cj, expressions/basics/stringInterpolation.cj, expressions/basics/subscriptAccess.cj, expressions/basics/trailingClosureOfficial.cj, expressions/basics/tryExpression.cj
- `Group/recovery/control-flow`: recovery/control-flow/forMissingIterable.cj, recovery/control-flow/ifMissingCondition.cj, recovery/control-flow/throwMissingExpression.cj, recovery/control-flow/whileMissingCondition.cj
- `Group/recovery/expressions`: recovery/expressions/binaryMissingRightOperand.cj
- `Group/types/type-references`: types/type-references/nestedFunctionAndTupleTypes.cj, types/type-references/optionalTypeRefs.cj, types/type-references/typeAliasRefsOfficial.cj
- `Group/control-flow/valid/nested-guards`: control-flow/valid/nestedLoopsAndGuards.cj

- `Group/declarations/class-like/constructors`: declarations/class-like/constructorDelegation.cj

- `Group/declarations/class-like/operators`: declarations/class-like/operatorOverloads.cj

- `Group/declarations/class-like/properties`: declarations/class-like/propertyAccessors.cj

- `Group/declarations/class-like/this-super`: declarations/class-like/thisAndSuperUsage.cj

- `Group/declarations/file-structure/conditional-compilation`: declarations/file-structure/conditionalCompilation.cj

- `Group/declarations/file-structure/imports`: declarations/file-structure/importForms.cj

- `Group/declarations/file-structure/macro-package`: declarations/file-structure/macroPackage.cj

- `Group/declarations/top-level/const`: declarations/top-level/constFunc.cj

- `Group/declarations/top-level/foreign`: declarations/top-level/foreignDecls.cj

- `Group/declarations/top-level/main-args`: declarations/top-level/mainWithArgs.cj

- `Group/expressions/basics/inc-dec`: expressions/basics/incDecExpressions.cj

- `Group/expressions/basics/lambda-params`: expressions/basics/lambdaParameters.cj

- `Group/expressions/basics/let-patterns`: expressions/basics/ifLetWhileLet.cj

- `Group/expressions/basics/optional-chain-invoke`: expressions/basics/optionalChainInvoke.cj

- `Group/expressions/basics/string-templates`: expressions/basics/stringTemplates.cj

- `Group/expressions/basics/throw`: expressions/basics/throwExpression.cj

- `Group/expressions/basics/type-conversion`: expressions/basics/typeConversions.cj

- `Group/expressions/patterns/let-binding`: expressions/patterns/letPatternBinding.cj

- `Group/expressions/patterns/match-forms`: expressions/patterns/matchPatternForms.cj

- `Group/recovery/declarations/missing-clause`: recovery/declarations/doWhileMissingCondition.cj

- `Group/recovery/expressions/missing-operands`: recovery/expressions/prefixMissingOperand.cj

- `Group/types/type-references/cffi`: types/type-references/cffiTypeRefs.cj

- `Group/types/type-references/this-type`: types/type-references/thisType.cj
- `Group/cangjie-features/quote`: cangjie-features/quote/quoteTokens.cj, cangjie-features/quote/quoteInterpolation.cj
- `Group/cangjie-features/if-available`: cangjie-features/ifAvailableExpression.cj

- `Group/control-flow/valid/if-let-guard`: control-flow/valid/ifLetWithGuard.cj

- `Group/declarations/class-like/abstract-sealed-struct`: declarations/class-like/abstractAndStructForms.cj

- `Group/declarations/class-like/finalizer`: declarations/class-like/finalizerWithReturn.cj

- `Group/declarations/class-like/struct-member`: declarations/class-like/structMemberFunction.cj

- `Group/declarations/top-level/nothing`: declarations/top-level/nothingReturn.cj

- `Group/declarations/top-level/params`: declarations/top-level/namedAndDefaultParams.cj

- `Group/expressions/basics/lambda-block`: expressions/basics/lambdaBlockBody.cj

- `Group/expressions/basics/option-coalescing`: expressions/basics/optionAndCoalescing.cj, expressions/basics/coalescingOperator.cj

- `Group/expressions/basics/slice`: expressions/basics/sliceExpression.cj

- `Group/expressions/patterns/binding`: expressions/patterns/matchBindingPattern.cj

- `Group/expressions/patterns/const-kinds`: expressions/patterns/matchConstPatternKinds.cj

- `Group/expressions/patterns/for-tuple`: expressions/patterns/forTuplePattern.cj

- `Group/types/type-references/generic-aliases`: types/type-references/genericTypeAliases.cj
- `Group/control-flow/valid/do-while-continue`: control-flow/valid/continueInDoWhile.cj

- `Group/control-flow/valid/for-where-guard`: control-flow/valid/forWithWhereGuard.cj

- `Group/control-flow/valid/if-else-if`: control-flow/valid/ifElseIfChain.cj

- `Group/control-flow/valid/try-nested`: control-flow/valid/tryNestedFinally.cj

- `Group/declarations/class-like/extend-interface-where`: declarations/class-like/extendInterfaceWithWhere.cj

- `Group/declarations/class-like/interface-mut-default`: declarations/class-like/interfaceMutAndDefault.cj

- `Group/declarations/class-like/static-const`: declarations/class-like/staticConstMember.cj

- `Group/declarations/class-like/static-init-assign`: declarations/class-like/staticInitAssign.cj

- `Group/declarations/top-level/main-args-size`: declarations/top-level/mainArgsSize.cj

- `Group/expressions/basics/array-tuple-nested`: expressions/basics/nestedArrayAndTupleLiterals.cj

- `Group/expressions/basics/interpolation-call`: expressions/basics/stringInterpolationCall.cj

- `Group/expressions/basics/literals`: expressions/basics/literalForms.cj

- `Group/expressions/basics/raw-string`: expressions/basics/rawStringEscapes.cj

- `Group/types/type-references/cffi-params`: types/type-references/cffiParameterTypes.cj
- `Group/cangjie-features/extend/alias-stdlib`: cangjie-features/extend/extendAliasAndStdlibType.cj

- `Group/cangjie-features/extend/generic-stdlib`: cangjie-features/extend/extendGenericStdlibType.cj

- `Group/control-flow/valid/catch-finally`: control-flow/valid/catchTypedFinally.cj

- `Group/declarations/class-like/generic-option-field`: declarations/class-like/genericOptionField.cj

- `Group/declarations/class-like/interface-static-prop`: declarations/class-like/interfaceStaticProp.cj

- `Group/declarations/class-like/multiple-extend`: declarations/class-like/multipleExtendBlocks.cj

- `Group/declarations/class-like/prop-this`: declarations/class-like/propUsingThis.cj

- `Group/declarations/class-like/struct-static-const`: declarations/class-like/structStaticConst.cj

- `Group/declarations/class-like/this-call`: declarations/class-like/thisQualifiedCall.cj

- `Group/declarations/top-level/cfunc-named-call`: declarations/top-level/cfuncCallNamedParams.cj

- `Group/declarations/top-level/main-no-return`: declarations/top-level/mainWithoutReturnType.cj

- `Group/expressions/basics/tuple-return`: expressions/basics/tupleReturnValue.cj

- `Group/expressions/patterns/let-nested-option`: expressions/patterns/letPatternNestedOption.cj

- `Group/types/type-references/function-type-named`: types/type-references/functionTypeNamedParams.cj
- `Group/expressions/basics/parenthesized`: expressions/basics/parenthesizedExpression.cj
- `Group/declarations/class-like/finalizer-only`: declarations/class-like/finalizerInClass.cj

## 词法 / 语法 / 大文件批次（2026-10-01）

下列用例全部以官方 cjc 1.0.5 与 1.1.3 逐文件编译取证：非 recovery 用例两版均零 error，recovery 用例两版均产生官方
诊断（诊断名与位置写在各文件头部注释里）。`syntax/declarations/macroCallForms.cj` 例外：它把宏定义与宏调用放在同一文件以便
覆盖语法，cjc 报 `macro_unexpect_def_and_call_in_same_pkg` 属官方语义限制（宏定义与调用不能同包）。

- `Group/lexical`: lexical/boolsAndUnit.cj, lexical/byteLiterals.cj, lexical/commentForms.cj, lexical/comments.cj, lexical/escapeForms.cj, lexical/floats.cj, lexical/identifiers.cj, lexical/importForms.cj, lexical/integerLiteralForms.cj, lexical/integers.cj, lexical/multilineStrings.cj, lexical/numericEdges.cj, lexical/rawIdentifiers.cj, lexical/rawStrings.cj, lexical/runes.cj, lexical/stringInterpolation.cj, lexical/strings.cj, lexical/whenConditions.cj
- `Group/syntax/declarations`: syntax/declarations/annotationForms.cj, syntax/declarations/cInteropForms.cj, syntax/declarations/classBodyForms.cj, syntax/declarations/constAndForeign.cj, syntax/declarations/constExpressions.cj, syntax/declarations/constructorForms.cj, syntax/declarations/enumForms.cj, syntax/declarations/extendBuiltinForms.cj, syntax/declarations/extendForms.cj, syntax/declarations/extendStructForms.cj, syntax/declarations/functionModifiers.cj, syntax/declarations/functionParams.cj, syntax/declarations/genericDeclarationForms.cj, syntax/declarations/genericForms.cj, syntax/declarations/interfaceForms.cj, syntax/declarations/macroCallForms.cj, syntax/declarations/macroDeclVariants.cj, syntax/declarations/macroQuoteForms.cj, syntax/declarations/memberInitForms.cj, syntax/declarations/operatorOverloadForms.cj, syntax/declarations/packageAndImportVariants.cj, syntax/declarations/propAccessorForms.cj, syntax/declarations/redefMemberForms.cj, syntax/declarations/staticInitAndFinalizer.cj, syntax/declarations/staticMemberForms.cj, syntax/declarations/structForms.cj, syntax/declarations/typeAliasForms.cj, syntax/declarations/typeAliasSignatureForms.cj, syntax/declarations/varDeclForms.cj, syntax/declarations/whereConstraints.cj
- `Group/syntax/expressions`: syntax/expressions/arrayOfInterfaceTypes.cj, syntax/expressions/assignmentForms.cj, syntax/expressions/bitwiseCompoundAssignForms.cj, syntax/expressions/blockAndParenForms.cj, syntax/expressions/constructionForms.cj, syntax/expressions/controlFlowExprForms.cj, syntax/expressions/exceptionThrowForms.cj, syntax/expressions/genericCallForms.cj, syntax/expressions/ifWhileForms.cj, syntax/expressions/incDecForms.cj, syntax/expressions/indexAndSliceForms.cj, syntax/expressions/lambdaCallForms.cj, syntax/expressions/lambdaForms.cj, syntax/expressions/lambdaTypedForms.cj, syntax/expressions/letPatternForms.cj, syntax/expressions/matchForms.cj, syntax/expressions/memberAccessForms.cj, syntax/expressions/multipleAssignForms.cj, syntax/expressions/nestedTypeRefForms.cj, syntax/expressions/numericConversionForms.cj, syntax/expressions/operatorForms.cj, syntax/expressions/powerOperatorForms.cj, syntax/expressions/questForms.cj, syntax/expressions/rangeStepForms.cj, syntax/expressions/stringTemplateForms.cj, syntax/expressions/subscriptOperatorForms.cj, syntax/expressions/synchronizedExprForms.cj, syntax/expressions/synchronizedForms.cj, syntax/expressions/thisSuperQualified.cj, syntax/expressions/thisTypeReturns.cj, syntax/expressions/tryCatchForms.cj, syntax/expressions/tryForms.cj, syntax/expressions/tryResourceForms.cj, syntax/expressions/typeCheckForms.cj, syntax/expressions/typeConvForms.cj, syntax/expressions/unsafeAndForeignCall.cj
- `Group/syntax/types`: syntax/types/builtinTypeRefs.cj, syntax/types/collectionTypeRefs.cj, syntax/types/functionTypeRefs.cj, syntax/types/genericTypeRefs.cj, syntax/types/questTypeRefs.cj
- `Group/syntax/recovery`: syntax/recovery/abstractPropInClass.cj, syntax/recovery/annotationDeclaration.cj, syntax/recovery/arrayPattern.cj, syntax/recovery/arraySingleArgConstruction.cj, syntax/recovery/bareArrowType.cj, syntax/recovery/bitwiseNotPrefix.cj, syntax/recovery/ccharTypeUndeclared.cj, syntax/recovery/cfuncNamedParam.cj, syntax/recovery/cfuncUnitTypeName.cj, syntax/recovery/cfuncValueTypeName.cj, syntax/recovery/coalescingAssign.cj, syntax/recovery/collectionTypeNoImport.cj, syntax/recovery/constArrayInitializer.cj, syntax/recovery/constructorMemberParams.cj, syntax/recovery/defaultParamOnUnnamed.cj, syntax/recovery/doWhileWithoutBrace.cj, syntax/recovery/emptyMultilineString.cj, syntax/recovery/emptyWhenCondition.cj, syntax/recovery/enumCtorParenEmpty.cj, syntax/recovery/enumCtorWithoutPipe.cj, syntax/recovery/enumDuplicateConstructor.cj, syntax/recovery/enumEllipsisNotSupported.cj, syntax/recovery/enumInterfaceUnimplemented.cj, syntax/recovery/enumTrailingPipe.cj, syntax/recovery/extendBuiltinOperator.cj, syntax/recovery/extendGenericUnused.cj, syntax/recovery/extendWhereNonGeneric.cj, syntax/recovery/flatNestedArrayLiteral.cj, syntax/recovery/forLetPattern.cj, syntax/recovery/forWithLetPattern.cj, syntax/recovery/foreignCArrayType.cj, syntax/recovery/foreignNamedParam.cj, syntax/recovery/funcMainKeyword.cj, syntax/recovery/functionExpressionBody.cj, syntax/recovery/functionTypeMixedParamNames.cj, syntax/recovery/functionTypeNamedParamBang.cj, syntax/recovery/functionTypeParamDefault.cj, syntax/recovery/genericVarianceMarker.cj, syntax/recovery/hexOverflow.cj, syntax/recovery/ifAvailableBracketForm.cj, syntax/recovery/immutablePropSetter.cj, syntax/recovery/initCallingInit.cj, syntax/recovery/inoutParameter.cj, syntax/recovery/integerOverflow.cj, syntax/recovery/interfaceInit.cj, syntax/recovery/invalidModifierMutFunc.cj, syntax/recovery/jstringLowerCase.cj, syntax/recovery/jstringUpperCase.cj, syntax/recovery/lambdaDefaultValue.cj, syntax/recovery/lambdaNamedParameter.cj, syntax/recovery/lambdaWithoutArrow.cj, syntax/recovery/logicalOperatorOverload.cj, syntax/recovery/macroNamedParams.cj, syntax/recovery/macroOutsideMacroPackage.cj, syntax/recovery/macroQuoteInterpolation.cj, syntax/recovery/mainBadParameterType.cj, syntax/recovery/mainCurriedParameterList.cj, syntax/recovery/mainVariadicArgs.cj, syntax/recovery/mainWithModifier.cj, syntax/recovery/matchRangePattern.cj, syntax/recovery/missingBodyInFunc.cj, syntax/recovery/missingNameInFunc.cj, syntax/recovery/missingTypeInVar.cj, syntax/recovery/namedArgumentInFuncValueCall.cj, syntax/recovery/nestedClassDecl.cj, syntax/recovery/nonPublicMacroDecl.cj, syntax/recovery/operatorNotOverloadable.cj, syntax/recovery/powerOperatorChain.cj, syntax/recovery/powerOperatorRightOperand.cj, syntax/recovery/prefixIncDec.cj, syntax/recovery/privateInitCalledOutside.cj, syntax/recovery/quoteDollarIdentifier.cj, syntax/recovery/quoteOutsideMacroPackage.cj, syntax/recovery/rangeChainedOperators.cj, syntax/recovery/rangeMissingOperands.cj, syntax/recovery/rangeStringOperands.cj, syntax/recovery/redefInstanceMember.cj, syntax/recovery/resultTypeUndeclared.cj, syntax/recovery/staticInitInEnum.cj, syntax/recovery/stepFunctionCall.cj, syntax/recovery/strayCloseBrace.cj, syntax/recovery/structFinalizer.cj, syntax/recovery/subscriptSetUnnamedParameter.cj, syntax/recovery/synchronizedWithoutLock.cj, syntax/recovery/thisTypeAsParameter.cj, syntax/recovery/thisTypeInStruct.cj, syntax/recovery/tryResourceAsValue.cj, syntax/recovery/tryResourceExpressionOnly.cj, syntax/recovery/tryResourceMissingBlock.cj, syntax/recovery/tryResourceWithoutResourceInterface.cj, syntax/recovery/unclosedBrace.cj, syntax/recovery/unclosedParen.cj, syntax/recovery/unterminatedMultilineString.cj, syntax/recovery/unterminatedRawString.cj, syntax/recovery/unterminatedString.cj, syntax/recovery/varPatternInMatch.cj, syntax/recovery/varrayConstructorArgForm.cj, syntax/recovery/varrayConstructorArgName.cj, syntax/recovery/varrayNestedArray.cj, syntax/recovery/varrayRefTypeArg.cj, syntax/recovery/varraySliceNotSupported.cj, syntax/recovery/varrayTypeArgumentWithoutDollar.cj, syntax/recovery/whereOnTypeAlias.cj, syntax/recovery/whereWithoutTypeParams.cj, syntax/recovery/withExpression.cj
- `Group/large`: large/deepGenerics.cj, large/deepNesting.cj, large/largeComments.cj, large/largeLineComment.cj, large/longOperatorChain.cj, large/longStrings.cj, large/manyDeclarations.cj, large/manyMatchCases.cj, large/wideArrayLiterals.cj
