# rawBuilder Coverage Matrix

Format example:

`<PsiNode>: <relative-path-1>, <relative-path-2>, ...`

All paths are relative to `cfir/raw-cfir/psi2cfir/testData/rawBuilder/`.

- `CjFile`: declarations/file-structure/emptyFile.cj
- `CjPackageDirective,CjImportDirective`: declarations/file-structure/packageAndImport.cj
- `CjMainFunction`: declarations/top-level/mainEntryOfficial.cj
- `CjNamedFunction`: declarations/top-level/topLevelFunction.cj, expressions/basics/trailingClosureOfficial.cj
- `CjProperty`: declarations/top-level/topLevelProperty.cj, expressions/basics/functionExpressions.cj
- `CjTypeAlias`: declarations/top-level/typeAlias.cj, types/type-references/typeAliasRefsOfficial.cj
- `CjClass`: declarations/class-like/emptyClass.cj, declarations/class-like/classWithMembers.cj, declarations/class-like/classWithSupertype.cj, declarations/class-like/classWithTypeParameters.cj, declarations/class-like/classMembersOrderStability.cj
- `CjInterface`: declarations/class-like/interfaceDeclaration.cj
- `CjStruct`: declarations/class-like/structDeclaration.cj
- `CjEnum,CjEnumConstructor`: declarations/class-like/enumDeclaration.cj, cangjie-features/match/matchExpressionOfficial.cj
- `CjPrimaryConstructor,CjSecondaryConstructor`: declarations/class-like/classWithMembers.cj
- `CjExtend`: cangjie-features/extend/extendDeclaration.cj, cangjie-features/extend/extendWithWhereOfficial.cj
- `CjTypeParameter`: declarations/class-like/classWithTypeParameters.cj, cangjie-features/extend/extendWithWhereOfficial.cj
- `CjBasicType,CjUserType,CjFunctionType,CjTupleType,CjVArrayType`: types/type-references/typeAliasRefsOfficial.cj, cangjie-features/varray/varrayTypeRef.cj
- `CjBlockExpression`: expressions/basics/functionExpressions.cj, control-flow/valid/controlFlow.cj
- `CjConstantExpression,CjStringTemplateExpression`: expressions/basics/functionExpressions.cj
- `CjBinaryExpression,CjPrefixExpression,CjPostfixExpression`: expressions/basics/functionExpressions.cj, recovery/expressions/binaryMissingRightOperand.cj
- `CjCallExpression,CjDotQualifiedExpression,CjNameReferenceExpression,CjLambdaExpression`: expressions/basics/trailingClosureOfficial.cj, cangjie-features/spawn/spawnExpressionOfficial.cj
- `CjIfExpression,CjForExpression,CjWhileExpression,CjReturnExpression`: control-flow/valid/controlFlow.cj
- `CjThrowExpression`: recovery/control-flow/throwMissingExpression.cj
- `CjMatchExpression`: cangjie-features/match/matchExpressionOfficial.cj
- `CjSpawnExpression`: cangjie-features/spawn/spawnExpressionOfficial.cj
- `Recovery-MissingIfCondition`: recovery/control-flow/ifMissingCondition.cj
- `Recovery-MissingWhileCondition`: recovery/control-flow/whileMissingCondition.cj
- `Recovery-MissingForIterable`: recovery/control-flow/forMissingIterable.cj
- `Recovery-MissingBinaryRhs`: recovery/expressions/binaryMissingRightOperand.cj
