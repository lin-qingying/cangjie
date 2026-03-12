# rawBuilder Coverage Matrix

Format example:

`<PsiNode>: <relative-path-1>, <relative-path-2>, ...`

All paths are relative to `cfir/raw-cfir/psi2cfir/testData/rawBuilder/`.

- `CjFile`: declarations/file-structure/emptyFile.cj
- `CjPackageDirective,CjImportDirective`: declarations/file-structure/packageAndImport.cj
- `CjMainFunction`: declarations/top-level/mainEntryOfficial.cj
- `CjNamedFunction`: declarations/top-level/topLevelFunction.cj, declarations/top-level/genericWhereFunction.cj, expressions/basics/trailingClosureOfficial.cj, expressions/basics/opAndIfExpressions.cj
- `CjProperty`: declarations/top-level/topLevelProperty.cj, expressions/basics/functionExpressions.cj
- `CjTypeAlias`: declarations/top-level/typeAlias.cj, types/type-references/typeAliasRefsOfficial.cj, types/type-references/nestedFunctionAndTupleTypes.cj
- `CjClass`: declarations/class-like/emptyClass.cj, declarations/class-like/classWithMembers.cj, declarations/class-like/classWithSupertype.cj, declarations/class-like/classWithTypeParameters.cj, declarations/class-like/classMembersOrderStability.cj, declarations/class-like/genericWhereTypeDeclarations.cj
- `CjInterface`: declarations/class-like/interfaceDeclaration.cj, declarations/class-like/genericWhereTypeDeclarations.cj
- `CjStruct`: declarations/class-like/structDeclaration.cj, declarations/class-like/genericWhereTypeDeclarations.cj
- `CjEnum,CjEnumConstructor`: declarations/class-like/enumDeclaration.cj, declarations/class-like/genericWhereTypeDeclarations.cj, cangjie-features/match/matchExpressionOfficial.cj, cangjie-features/match/matchRichPatternsOfficial.cj
- `CjPrimaryConstructor,CjSecondaryConstructor`: declarations/class-like/classWithMembers.cj
- `CjExtend`: cangjie-features/extend/extendDeclaration.cj, cangjie-features/extend/extendWithWhereOfficial.cj, cangjie-features/extend/extendGenericWhereChain.cj
- `CjTypeParameter`: declarations/class-like/classWithTypeParameters.cj, declarations/class-like/genericWhereTypeDeclarations.cj, declarations/top-level/genericWhereFunction.cj, cangjie-features/extend/extendWithWhereOfficial.cj, cangjie-features/extend/extendGenericWhereChain.cj
- `CjBasicType,CjUserType,CjFunctionType,CjTupleType,CjVArrayType`: types/type-references/typeAliasRefsOfficial.cj, types/type-references/nestedFunctionAndTupleTypes.cj, cangjie-features/varray/varrayTypeRef.cj
- `CjBlockExpression`: expressions/basics/functionExpressions.cj, expressions/basics/opAndIfExpressions.cj, control-flow/valid/controlFlow.cj, control-flow/valid/forWithPatternGuard.cj
- `CjConstantExpression,CjStringTemplateExpression`: expressions/basics/functionExpressions.cj, cangjie-features/match/matchRichPatternsOfficial.cj
- `CjBinaryExpression,CjPrefixExpression,CjPostfixExpression`: expressions/basics/functionExpressions.cj, expressions/basics/opAndIfExpressions.cj, control-flow/valid/forWithPatternGuard.cj, recovery/expressions/binaryMissingRightOperand.cj
- `CjCallExpression,CjDotQualifiedExpression,CjNameReferenceExpression,CjLambdaExpression`: expressions/basics/trailingClosureOfficial.cj, cangjie-features/spawn/spawnExpressionOfficial.cj
- `CjIfExpression,CjForExpression,CjWhileExpression,CjReturnExpression`: control-flow/valid/controlFlow.cj, control-flow/valid/forWithPatternGuard.cj, declarations/top-level/genericWhereFunction.cj, expressions/basics/opAndIfExpressions.cj
- `CjThrowExpression`: recovery/control-flow/throwMissingExpression.cj
- `CjMatchExpression`: cangjie-features/match/matchExpressionOfficial.cj, cangjie-features/match/matchRichPatternsOfficial.cj
- `CjSpawnExpression`: cangjie-features/spawn/spawnExpressionOfficial.cj
- `Recovery-MissingIfCondition`: recovery/control-flow/ifMissingCondition.cj
- `Recovery-MissingWhileCondition`: recovery/control-flow/whileMissingCondition.cj
- `Recovery-MissingForIterable`: recovery/control-flow/forMissingIterable.cj
- `Recovery-MissingBinaryRhs`: recovery/expressions/binaryMissingRightOperand.cj
