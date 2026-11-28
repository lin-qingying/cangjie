@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.CompositeValueIndex
import PackageFormat.ConstValue
import PackageFormat.DeclKind
import PackageFormat.MemberValue
import PackageFormat.Package
import PackageFormat.TypeKind
import PackageFormat.VarInfo
import com.google.flatbuffers.Table
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirConstantValueExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.registerTypeAwareSupertypeComponents
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.io.TempDir
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 统一常量产品的 typed schema 材化正控：正式 Writer → Manager → Library provider
 * → 变量 evaluatedInitializer；不调用内部常量 decoder，不提供测试语义解释器。
 *
 * 固定证据：
 * native 14276740ffec1262b81b6b5236b1d840af270b37，ModuleFormat.fbs:85–119、675；
 * runtime fc9f54db5d92079eb1845968abe98e52df92d70d，std/core/float.cj:65–75；
 * docs 70281ea0ecb4adb86260ac233cf87fb4dbb23bc9，basic_data_type/integer.md:21；
 * Kotlin 3c7a5041f238102285764d370665f56f587f9dc4，FirConstDeserializer / context。
 *
 * 固定 native SaveVarDecl 不填 value，包 writer 不填 allValues，ConstPattern 不写 values。
 * 以下非空 v 池和回环均为真实 schema 构造输入，不声称官方 SOURCE/cjc 生成过它们，
 * 也不声称 const ctor 能生成循环对象。binary16 的 Double 表示保留 sign 和 NaN fraction
 * 的逐位扩展；使用 toRawBits，不能由 Double.toBits 的 NaN 规范化掩盖载荷变化。
 */
class CjoTypedConstantGraphTest {
    /** 同一个 Int16Value payload，由实际 t 槽区分 Int16 数学值与 Float16 位表示。 */
    @Test
    fun binary16AndInt16KeepDistinctTypesAndRawBits(@TempDir root: Path) {
        val declarations = halfCases.flatMapIndexed { index, sample ->
            listOf(
                constant("half" + index, 1u, CjoInt16ConstValue(sample.bits.toShort())),
                constant("integer" + index, 2u, CjoInt16ConstValue(sample.bits.toShort())),
            )
        }
        val fixture = fixture(root, metadata("halfconstants", declarations, listOf(
            CjoTypeMetadata(TypeKind.Float16), CjoTypeMetadata(TypeKind.Int16),
        )))
        assertEquals(0, fixture.wire.allValuesLength)
        halfCases.forEachIndexed { index, sample ->
            assertEquals(ConstValue.Int16Value, variableInfo(fixture.wire, index * 2).valueType)
            assertEquals(ConstValue.Int16Value, variableInfo(fixture.wire, index * 2 + 1).valueType)
            val halfVariable = fixture.variable("half" + index)
            val half = primitive(constantValue(halfVariable), ConePrimitiveType.FLOAT16, CfirLiteralKind.FLOAT)
            assertEquals(sample.doubleBits.toLong(), assertIs<Double>(half.value).toRawBits(),
                "binary16 bits=" + sample.bits.toString(16))
            assertEquals(ConePrimitiveType.FLOAT16, halfVariable.returnTypeRef.coneType)
            assertSame(half, constantValue(fixture.variable("half" + index)))

            val integer = primitive(fixture.value("integer" + index), ConePrimitiveType.INT16, CfirLiteralKind.INT)
            assertEquals(BigInteger.valueOf(sample.bits.toShort().toLong()), integer.value)
        }
    }

    /** 类 header 先完成再填自身 static const；不能为值查询递归创建还在构造的同一类。 */
    @Test
    fun staticConstantOfItsOwnClassUsesTheCompletedHeader(@TempDir root: Path) {
        val input = graphMetadata()
        val declarations = input.declarations.toMutableList()
        val ownerInfo = assertIs<CjoClassInfo>(declarations[0].info)
        val staticIndex = (declarations.size + 1).toUInt()
        declarations[0] = declarations[0].copy(info = ownerInfo.copy(body = ownerInfo.body + staticIndex))
        declarations += constant("sharedSelf", 1u, CjoCompositeConstValue(0u)).copy(
            isTopLevel = false, attributes = flags(Attribute.PUBLIC, Attribute.STATIC))
        val fixture = fixture(root, input.copy(declarations = declarations))
        val owner = fixture.classifier("Leaf")
        val stored = field(owner, "sharedSelf")
        val value = assertIs<CfirConstantValue.ObjectValue>(constantValue(stored))
        val number = field(owner, "number")
        assertEquals(listOf(number.symbol), value.fields.keys.toList())
        assertEquals(BigInteger.valueOf(42), primitive(value.fields.getValue(number.symbol), ConePrimitiveType.INT64, CfirLiteralKind.INT).value)
        assertSame(value, fixture.value("first"))
        assertSame(value, fixture.value("second"))
        assertSame(owner, fixture.classifier("Leaf"))
        assertSame(stored, fixture.provider.deserializeCjoDeclaration(fixture.path, fixture.packageName, staticIndex.toInt() - 1))
    }

    /** 固定 schema 的 CompositeValue 也表示 tuple，字段顺序与各自 t 槽独立于 nominal classifier。 */
    @Test
    fun compositeTupleUsesItsOrderedTypesWithoutAClassifier(@TempDir root: Path) {
        val input = metadata("compositetuple", listOf(constant("tuple", 3u, CjoCompositeConstValue(0u))), listOf(
            CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Float16), CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 2u)),
        ), values = listOf(CjoCompositeValueMetadata(3u, listOf(
            valueField("first", 1u, CjoInt64ConstValue(3)), valueField("second", 2u, CjoInt16ConstValue(0x4a40.toShort())),
        ))))
        val fixture = fixture(root, input)
        val variable = fixture.variable("tuple")
        val value = assertIs<CfirConstantValue.Tuple>(constantValue(variable))
        assertSame(variable.returnTypeRef.coneType, value.type)
        assertEquals(BigInteger.valueOf(3), primitive(value.elements[0], ConePrimitiveType.INT64, CfirLiteralKind.INT).value)
        assertEquals(12.5, primitive(value.elements[1], ConePrimitiveType.FLOAT16, CfirLiteralKind.FLOAT).value)
        assertSame(value, fixture.value("tuple"))
    }

    /** UInt64 保留无符号数学域；FLOAT kind 的 payload 为 Double，type 仍区分 Float32/64。 */
    @Test
    fun unsigned64AndFloatingScalarsKeepTheirActualTypes(@TempDir root: Path) {
        val unsigned = listOf(0uL, Long.MAX_VALUE.toULong(), 1uL shl 63, ULong.MAX_VALUE)
        val single = listOf(0.0f, -0.0f, Float.MIN_VALUE, Float.MAX_VALUE, 1.25f)
        val double = listOf(0.0, -0.0, Double.MIN_VALUE, Double.MAX_VALUE, 1.25)
        val declarations = unsigned.mapIndexed { index, value ->
            constant("unsigned" + index, 1u, CjoUInt64ConstValue(value))
        } + single.mapIndexed { index, value ->
            constant("single" + index, 2u, CjoFloat32ConstValue(value))
        } + double.mapIndexed { index, value ->
            constant("double" + index, 3u, CjoFloat64ConstValue(value))
        }
        val fixture = fixture(root, metadata("scalarconstants", declarations, listOf(
            CjoTypeMetadata(TypeKind.UInt64), CjoTypeMetadata(TypeKind.Float32), CjoTypeMetadata(TypeKind.Float64),
        )))
        unsigned.forEachIndexed { index, expected ->
            val variable = fixture.variable("unsigned" + index)
            val value = primitive(constantValue(variable), ConePrimitiveType.UINT64, CfirLiteralKind.INT)
            assertEquals(BigInteger(expected.toString()), assertIs<BigInteger>(value.value))
            assertEquals(ConePrimitiveType.UINT64, variable.returnTypeRef.coneType)
        }
        single.forEachIndexed { index, expected ->
            val variable = fixture.variable("single" + index)
            val value = primitive(constantValue(variable), ConePrimitiveType.FLOAT32, CfirLiteralKind.FLOAT)
            assertEquals(expected.toDouble().toRawBits(), assertIs<Double>(value.value).toRawBits())
            assertEquals(ConePrimitiveType.FLOAT32, variable.returnTypeRef.coneType)
        }
        double.forEachIndexed { index, expected ->
            val variable = fixture.variable("double" + index)
            val value = primitive(constantValue(variable), ConePrimitiveType.FLOAT64, CfirLiteralKind.FLOAT)
            assertEquals(expected.toRawBits(), assertIs<Double>(value.value).toRawBits())
            assertEquals(ConePrimitiveType.FLOAT64, variable.returnTypeRef.coneType)
        }
    }

    /** union vector 自身不携带元素类型；Tuple/VArray 必须消费实际容器 t 槽。 */
    @Test
    fun tupleAndVArrayUseTheirElementTypes(@TempDir root: Path) {
        val unsigned = listOf(0uL, Long.MAX_VALUE.toULong(), 1uL shl 63, ULong.MAX_VALUE)
        val fixture = fixture(root, metadata("containerconstants", listOf(
            constant("tuple", 5u, CjoArrayConstValue(listOf(
                CjoUInt64ConstValue(ULong.MAX_VALUE), CjoInt16ConstValue(0x8000.toShort()),
                CjoFloat32ConstValue(1.25f), CjoFloat64ConstValue(-0.0),
            ))),
            constant("vector", 6u, CjoArrayConstValue(unsigned.map(::CjoUInt64ConstValue))),
        ), listOf(
            CjoTypeMetadata(TypeKind.UInt64), CjoTypeMetadata(TypeKind.Float16),
            CjoTypeMetadata(TypeKind.Float32), CjoTypeMetadata(TypeKind.Float64),
            CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 2u, 3u, 4u)),
            CjoTypeMetadata(TypeKind.VArray, listOf(1u), semanticInfo = CjoArrayTypeInfoMetadata(4L)),
        )))
        val tupleVariable = fixture.variable("tuple")
        val tuple = assertIs<CfirConstantValue.Tuple>(constantValue(tupleVariable))
        assertSame(tupleVariable.returnTypeRef.coneType, tuple.type)
        assertEquals(listOf(ConePrimitiveType.UINT64, ConePrimitiveType.FLOAT16,
            ConePrimitiveType.FLOAT32, ConePrimitiveType.FLOAT64), assertIs<ConeTupleType>(tuple.type).elementTypes)
        assertEquals(BigInteger("18446744073709551615"),
            primitive(tuple.elements[0], ConePrimitiveType.UINT64, CfirLiteralKind.INT).value)
        assertEquals(Long.MIN_VALUE,
            assertIs<Double>(primitive(tuple.elements[1], ConePrimitiveType.FLOAT16, CfirLiteralKind.FLOAT).value).toRawBits())
        assertEquals(1.25, primitive(tuple.elements[2], ConePrimitiveType.FLOAT32, CfirLiteralKind.FLOAT).value)
        assertEquals(Long.MIN_VALUE,
            assertIs<Double>(primitive(tuple.elements[3], ConePrimitiveType.FLOAT64, CfirLiteralKind.FLOAT).value).toRawBits())
        val vectorVariable = fixture.variable("vector")
        val vector = assertIs<CfirConstantValue.VArray>(constantValue(vectorVariable))
        assertSame(vectorVariable.returnTypeRef.coneType, vector.type)
        assertEquals(4L, assertIs<ConeVArrayType>(vector.type).size)
        assertEquals(ConePrimitiveType.UINT64, vector.type.elementType)
        assertEquals(unsigned.map { BigInteger(it.toString()) }, vector.elements.map {
            primitive(it, ConePrimitiveType.UINT64, CfirLiteralKind.INT).value
        })
    }

    /** 两顶层 const 与两个字段共享 v0；相等的 v1 保留独立身份，顺序不改变绑定。 */
    @Test
    fun sharedPoolIdentitySurvivesValueFirstAndClassifierFirstQueries(@TempDir root: Path) {
        for (classifierFirst in listOf(false, true)) {
            for (secondFirst in listOf(false, true)) {
                val fixture = fixture(root.resolve("$classifierFirst-$secondFirst"), graphMetadata())
                if (classifierFirst) {
                    fixture.classifier("Leaf")
                    fixture.classifier("Pair")
                }
                val firstName = if (secondFirst) "second" else "first"
                val secondName = if (secondFirst) "first" else "second"
                val firstVariable = fixture.variable(firstName)
                val first = assertIs<CfirConstantValue.ObjectValue>(constantValue(firstVariable))
                assertSame(first, fixture.value(secondName))
                val equal = assertIs<CfirConstantValue.ObjectValue>(fixture.value("equal"))
                assertNotSame(first, equal)
                val leaf = fixture.classifier("Leaf")
                val number = field(leaf, "number")
                assertEquals(listOf(number.symbol), first.fields.keys.toList())
                assertEquals(first.fields.keys.toList(), equal.fields.keys.toList())
                assertEquals(BigInteger.valueOf(42), first.fields.getValue(number.symbol).let {
                    primitive(it, ConePrimitiveType.INT64, CfirLiteralKind.INT).value
                })
                assertEquals(BigInteger.valueOf(42), equal.fields.getValue(number.symbol).let {
                    primitive(it, ConePrimitiveType.INT64, CfirLiteralKind.INT).value
                })
                val pair = assertIs<CfirConstantValue.ObjectValue>(fixture.value("pair"))
                val owner = fixture.classifier("Pair")
                val left = field(owner, "left")
                val right = field(owner, "right")
                assertEquals(listOf(left.symbol, right.symbol), pair.fields.keys.toList())
                assertSame(first, pair.fields.getValue(left.symbol))
                assertSame(first, pair.fields.getValue(right.symbol))
                assertSame(number, fixture.provider.deserializeCjoDeclaration(fixture.path, fixture.packageName, 1))
                assertSame(firstVariable, fixture.variable(firstName))
                assertSame(first, fixture.value(firstName))
                assertSame(pair, fixture.value("pair"))
                assertProvenance(number, fixture.path, 1)
                assertSame(fixture.module, number.moduleData)
            }
        }
    }

    /** 合同回边经唯一对象壳完成；只观察正式发布产品，不以截断对象树代替循环。 */
    @Test
    fun selfAndMutualCyclesPublishTheOriginalShells(@TempDir root: Path) {
        for (classifierFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(classifierFirst.toString()), graphMetadata())
            if (classifierFirst) fixture.classifier("Ring")
            val self = assertIs<CfirConstantValue.ObjectValue>(fixture.value("self"))
            val a = assertIs<CfirConstantValue.ObjectValue>(fixture.value("cycleA"))
            val b = assertIs<CfirConstantValue.ObjectValue>(fixture.value("cycleB"))
            val owner = fixture.classifier("Ring")
            val next = field(owner, "next")
            val number = field(owner, "number")
            for (value in listOf(self, a, b)) {
                assertEquals(listOf(next.symbol, number.symbol), value.fields.keys.toList())
                assertEquals(owner.symbol.classId, assertIs<ConeClassLikeType>(value.type).classId)
            }
            assertSame(self, self.fields.getValue(next.symbol))
            assertSame(b, a.fields.getValue(next.symbol))
            assertSame(a, b.fields.getValue(next.symbol))
            assertNotSame(self, a)
            assertNotSame(a, b)
            assertEquals(listOf(11L, 12L, 13L).map(BigInteger::valueOf),
                listOf(self, a, b).map {
                    primitive(it.fields.getValue(number.symbol), ConePrimitiveType.INT64, CfirLiteralKind.INT).value
                })
            assertSame(self, fixture.value("self"))
            assertSame(a, fixture.value("cycleA"))
            assertSame(b, fixture.value("cycleB"))
        }
    }

    /** 相同内容/包名/index 的两个真实 Manager/session context 不能共享值或字段 symbol。 */
    @Test
    fun independentContextsKeepIndependentPoolIdentities(@TempDir root: Path) {
        val first = fixture(root.resolve("one"), graphMetadata())
        val second = fixture(root.resolve("two"), graphMetadata())
        val a = assertIs<CfirConstantValue.ObjectValue>(first.value("first"))
        val b = assertIs<CfirConstantValue.ObjectValue>(second.value("first"))
        assertNotSame(a, b)
        assertNotSame(a.fields.keys.single(), b.fields.keys.single())
        assertSame(first.module, a.fields.keys.single().cfir.moduleData)
        assertSame(second.module, b.fields.keys.single().cfir.moduleData)
        assertSame(a, first.value("second"))
        assertSame(b, second.value("second"))
    }

    /** v0 是实际有效 ordinal；只有 union NONE 才表示 VarInfo 没有保存常量。 */
    @Test
    fun zeroValueIndexIsPresentWhileNoneIsAbsent(@TempDir root: Path) {
        val input = graphMetadata()
        val fixture = fixture(root, input.copy(declarations = input.declarations +
            constant("unsaved", 1u, null)))
        val present = variableInfo(fixture.wire, 8)
        assertEquals(ConstValue.CompositeValue, present.valueType)
        assertEquals(0u, compositeReference(present).idx)
        val absent = variableInfo(fixture.wire, input.declarations.size)
        assertEquals(ConstValue.NONE, absent.valueType)
        assertNull(fixture.variable("unsaved").evaluatedInitializer)
        val value = assertIs<CfirConstantValue.ObjectValue>(fixture.value("first"))
        assertEquals(1, value.fields.size)
        assertSame(value, fixture.value("second"))
    }

    /** payload 声明类型为 T，值的实际 UInt64 来自 MemberValue.type，字段仍是 Box.T 的真实成员。 */
    @Test
    fun genericObjectUsesWireFieldTypesAndCanonicalMemberSymbols(@TempDir root: Path) {
        for (classifierFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(classifierFirst.toString()), genericMetadata("genericconstants"))
            if (classifierFirst) fixture.classifier("Box")
            val variable = fixture.variable("box")
            val value = assertIs<CfirConstantValue.ObjectValue>(constantValue(variable))
            val owner = fixture.classifier("Box")
            val parameter = assertIs<CfirTypeParameter>(owner.typeParameters.single())
            val payload = field(owner, "payload")
            val half = field(owner, "half")
            assertSame(owner.symbol, parameter.containingDeclarationSymbol)
            assertSame(parameter.symbol, assertIs<ConeTypeParameterType>(payload.returnTypeRef.coneType)
                .lookupTag.typeParameterSymbol)
            assertEquals(listOf(payload.symbol, half.symbol), value.fields.keys.toList())
            assertEquals(BigInteger("18446744073709551615"),
                primitive(value.fields.getValue(payload.symbol), ConePrimitiveType.UINT64, CfirLiteralKind.INT).value)
            assertEquals(12.5,
                primitive(value.fields.getValue(half.symbol), ConePrimitiveType.FLOAT16, CfirLiteralKind.FLOAT).value)
            val type = assertIs<ConeClassLikeType>(value.type)
            assertEquals(owner.symbol.classId, type.classId)
            assertEquals(ConePrimitiveType.UINT64, type.typeArguments.single().type)
            assertSame(variable.returnTypeRef.coneType, value.type)
            assertSame(value, fixture.value("boxAgain"))
            assertSame(payload, fixture.provider.deserializeCjoDeclaration(fixture.path, fixture.packageName, 2))
            assertProvenance(payload, fixture.path, 2)
            assertSame(fixture.module, payload.moduleData)
        }
    }

    /** 入口 type 与值池 type 都有效但不兼容；缓存命中也必须检查，不把 Leaf 放进 Int64。 */
    @Test
    fun compositeReferencesCheckExpectedTypesBeforeAndAfterCachePublication(@TempDir root: Path) {
        for (valueFirst in listOf(false, true)) {
            val input = graphMetadata()
            val fixture = fixture(root.resolve(valueFirst.toString()), input.copy(declarations = input.declarations +
                constant("wrongType", 2u, CjoCompositeConstValue(0u))))
            val before = if (valueFirst) fixture.value("first") else null
            repeat(2) { assertInvalidConstant { fixture.variable("wrongType") } }
            val value = assertIs<CfirConstantValue.ObjectValue>(fixture.value("first"))
            if (before != null) assertSame(before, value)
            assertSame(value, fixture.value("second"))
            assertEquals(fixture.classifier("Leaf").symbol.classId, assertIs<ConeClassLikeType>(value.type).classId)
        }
    }

    /** 合法动态子类值可经静态基类声明读取，兼容检查不能退化为类型相等。 */
    @Test
    fun derivedObjectRemainsOneValueAcrossCompatibleBaseAndDerivedDeclarations(@TempDir root: Path) {
        val input = metadata("subtypeconstants", listOf(
            CjoPackageDeclaration("Base", DeclKind.ClassDecl, type = 1u, attributes = flags(Attribute.PUBLIC, Attribute.OPEN), info = CjoClassInfo()),
            CjoPackageDeclaration("Derived", DeclKind.ClassDecl, type = 2u, attributes = flags(Attribute.PUBLIC), info = CjoClassInfo(inheritedTypes = listOf(1u))),
            constant("baseView", 1u, CjoCompositeConstValue(0u)), constant("derivedView", 2u, CjoCompositeConstValue(0u)),
        ), listOf(nominal(1u), nominal(2u)), values = listOf(CjoCompositeValueMetadata(2u)))
        val fixture = fixture(root, input)
        val value = assertIs<CfirConstantValue.ObjectValue>(fixture.value("baseView"))
        val derived = fixture.classifier("Derived")
        assertSame(derived.symbol, assertIs<ConeClassLikeType>(value.type).lookupTag.toSymbol(fixture.module.session))
        assertSame(value, fixture.value("derivedView"))
        assertSame(value, fixture.value("baseView"))
        assertEquals(fixture.classifier("Base").symbol.classId, assertIs<ConeClassLikeType>(fixture.variable("baseView").returnTypeRef.coneType).classId)
        assertEquals(derived.symbol.classId, assertIs<ConeClassLikeType>(fixture.variable("derivedView").returnTypeRef.coneType).classId)
    }

    /** T→UInt64 的字段声明约束独立核验；合法 Float16 wire/tag 不能写入 Box<UInt64>.payload。 */
    @Test
    fun genericFieldRejectsAValidButIncompatibleWireType(@TempDir root: Path) {
        val input = genericMetadata("wronggenericfield")
        val wrong = input.values.single().copy(fields = listOf(
            valueField("payload", 4u, CjoInt16ConstValue(0x4a40.toShort())),
            valueField("half", 4u, CjoInt16ConstValue(0x4a40.toShort())),
        ))
        val fixture = fixture(root, input.copy(
            declarations = input.declarations + constant("wrong", 3u, CjoCompositeConstValue(1u)),
            values = input.values + wrong,
        ))
        val completed = assertIs<CfirConstantValue.ObjectValue>(fixture.value("box"))
        val fields = completed.fields
        repeat(2) {
            assertInvalidConstant { fixture.variable("wrong") }
            assertSame(completed, fixture.value("box"))
            assertSame(completed, fixture.value("boxAgain"))
            assertSame(fields, completed.fields)
        }
        val owner = fixture.classifier("Box")
        val parameter = assertIs<CfirTypeParameter>(owner.typeParameters.single())
        val payload = field(owner, "payload")
        assertSame(parameter.symbol, assertIs<ConeTypeParameterType>(payload.returnTypeRef.coneType).lookupTag.typeParameterSymbol)
        assertEquals(BigInteger("18446744073709551615"), primitive(completed.fields.getValue(payload.symbol), ConePrimitiveType.UINT64, CfirLiteralKind.INT).value)
    }

    /** 两个合法 kind/index 的错配也须拒绝；以前完成的真实声明/type 不被另一种 Cone 取代。 */
    @Test
    fun nominalWireKindCannotWrapADifferentDeclarationKind(@TempDir root: Path) {
        val cases = listOf(
            Triple(DeclKind.InterfaceDecl, TypeKind.Interface, TypeKind.Class),
            Triple(DeclKind.StructDecl, TypeKind.Struct, TypeKind.Class),
            Triple(DeclKind.ClassDecl, TypeKind.Class, TypeKind.Struct),
            Triple(DeclKind.ClassDecl, TypeKind.Class, TypeKind.Interface),
            Triple(DeclKind.ClassDecl, TypeKind.Class, TypeKind.Enum),
        )
        for ((declarationKind, correctKind, wrongKind) in cases) {
            val info = when (declarationKind) {
                DeclKind.InterfaceDecl -> CjoInterfaceInfo()
                DeclKind.StructDecl -> CjoStructInfo()
                else -> CjoClassInfo()
            }
            val input = metadata("mismatch" + declarationKind + wrongKind, listOf(
                CjoPackageDeclaration("Actual", declarationKind, type = 1u, attributes = flags(Attribute.PUBLIC), info = info),
                constant("good", 1u, null), constant("bad", 2u, null),
            ), listOf(
                CjoTypeMetadata(correctKind, semanticInfo = CjoCompositeTypeInfoMetadata(1u)),
                CjoTypeMetadata(wrongKind, semanticInfo = CjoCompositeTypeInfoMetadata(1u)),
            ))
            val fixture = fixture(root.resolve("$declarationKind-$wrongKind"), input)
            val good = fixture.variable("good")
            val type = good.returnTypeRef.coneType
            repeat(2) {
                assertInvalidConstant { fixture.variable("bad") }
                assertSame(good, fixture.variable("good"))
                assertSame(type, good.returnTypeRef.coneType)
            }
        }
    }

    /** 固定 schema/native 未指定 Bool/Rune 的整数 union 编码；语言构造转换不能授权 wire tag。 */
    @Test
    fun booleanAndRuneDoNotAcceptUnspecifiedIntegerUnionEncodings(@TempDir root: Path) {
        for (kind in listOf(TypeKind.Bool, TypeKind.Rune)) {
            val fixture = fixture(root.resolve(kind.toString()), metadata("unsupportedinteger" + kind, listOf(
                constant("signed", 1u, CjoInt8ConstValue(1)), constant("unsigned", 1u, CjoUInt64ConstValue(1uL)),
            ), listOf(CjoTypeMetadata(kind))))
            for (name in listOf("signed", "unsigned")) repeat(2) { assertInvalidConstant { fixture.variable(name) } }
        }
    }

    /** 真正双 parent：consumer 仅依赖 Q2；输入顺序和 classifier/value 首次请求顺序均交换。 */
    @Test
    fun doubleParentSelectsQ2ClassFieldsAndProvenanceInEitherOrder(@TempDir root: Path) {
        for (reversePaths in listOf(false, true)) {
            for (classifierFirst in listOf(false, true)) {
                val directory = root.resolve("$reversePaths-$classifierFirst")
                val q1Path = directory.resolve("q1/parent.cjo").toAbsolutePath().normalize()
                val q2Path = directory.resolve("q2/parent.cjo").toAbsolutePath().normalize()
                val consumerPath = directory.resolve("consumer.cjo").toAbsolutePath().normalize()
                val parent = genericMetadata("parent").let {
                    it.copy(declarations = it.declarations.take(4), values = emptyList())
                }
                CjoPackageWriter.write(q1Path, parent.copy(moduleName = "Q1"))
                CjoPackageWriter.write(q2Path, parent.copy(moduleName = "Q2"))
                CjoPackageWriter.write(consumerPath, metadata("consumer", listOf(
                    constant("selected", 1u, CjoCompositeConstValue(0u)),
                    constant("selectedAgain", 1u, CjoCompositeConstValue(0u)),
                ), listOf(
                    CjoTypeMetadata(TypeKind.Class, listOf(2u), semanticInfo = CjoCompositeTypeInfoMetadata(
                        0u, packageId = 0, declarationKey = "parent::Box")),
                    CjoTypeMetadata(TypeKind.UInt64), CjoTypeMetadata(TypeKind.Float16),
                ), values = listOf(CjoCompositeValueMetadata(1u, listOf(
                    valueField("payload", 2u, CjoUInt64ConstValue(ULong.MAX_VALUE)),
                    valueField("half", 3u, CjoInt16ConstValue(0x4a40.toShort())),
                ))), imports = listOf("parent")).copy(moduleName = "P"))
                val parentPaths = if (reversePaths) listOf(q2Path, q1Path) else listOf(q1Path, q2Path)
                val manager = CjoManager(CjoSearchPath(envProvider = { null }),
                    CjoFormatVersionPolicy.DeferredToConsumerSession,
                    explicitCommonPartPaths = parentPaths + listOf(consumerPath))
                val q1 = Module("Q1")
                val q2 = Module("Q2")
                val p = Module("P", dependencies = listOf(q2))
                val library = Module("library")
                val modules = linkedMapOf<CfirModuleData, LibraryPathFilter>(
                    library to LibraryPathFilter.RejectAll,
                    p to LibraryPathFilter.LibraryList(setOf(consumerPath)),
                )
                parentPaths.forEach { path ->
                    modules[if (path == q1Path) q1 else q2] = LibraryPathFilter.LibraryList(setOf(path))
                }
                val provider = CfirDeserializedSymbolProvider(library.session, manager, CfirCangJieScopeProvider(),
                    library, MultipleModuleDataProvider(modules, regularDependenciesModuleData = library))
                library.registerProvider(provider)
                p.registerProvider(provider)
                val selected = assertNotNull(provider.selectImportedSnapshot(p, "parent"))
                assertEquals(q2Path, selected.sourcePath)
                assertEquals(if (reversePaths) listOf("Q2", "Q1") else listOf("Q1", "Q2"),
                    manager.loadPackageSnapshots("parent").map { it.pkg.moduleName })
                val warmed = assertNotNull(provider.getClassLikeSymbolByClassId(ClassId.topLevel(FqName("parent.Box"))))
                assertSame(if (reversePaths) q2 else q1, warmed.cfir.moduleData)
                if (classifierFirst) provider.deserializeCjoDeclaration(q2Path, "parent", 0)
                val variable = topLevelVariable(provider, "consumer", "selected")
                val value = assertIs<CfirConstantValue.ObjectValue>(constantValue(variable))
                val owner = assertIs<CfirClass>(provider.deserializeCjoDeclaration(q2Path, "parent", 0))
                val payload = field(owner, "payload")
                val half = field(owner, "half")
                assertSame(q2, owner.moduleData)
                assertSame(q2, payload.moduleData)
                assertSame(q2, half.moduleData)
                assertProvenance(owner, q2Path, 0)
                assertProvenance(payload, q2Path, 2)
                assertProvenance(half, q2Path, 3)
                assertSame(payload, provider.deserializeCjoDeclaration(q2Path, "parent", 2))
                assertEquals(listOf(payload.symbol, half.symbol), value.fields.keys.toList())
                assertEquals(BigInteger("18446744073709551615"),
                    primitive(value.fields.getValue(payload.symbol), ConePrimitiveType.UINT64, CfirLiteralKind.INT).value)
                assertEquals(12.5,
                    primitive(value.fields.getValue(half.symbol), ConePrimitiveType.FLOAT16, CfirLiteralKind.FLOAT).value)
                assertEquals(owner.symbol.classId, assertIs<ConeClassLikeType>(value.type).classId)
                assertSame(owner.symbol, assertIs<ConeClassLikeType>(value.type).lookupTag.toSymbol(p.session))
                assertEquals(ConePrimitiveType.UINT64, assertIs<ConeClassLikeType>(value.type).typeArguments.single().type)
                assertSame(p, variable.moduleData)
                assertProvenance(variable, consumerPath, 0)

                val other = assertIs<CfirClass>(provider.deserializeCjoDeclaration(q1Path, "parent", 0))
                assertEquals(owner.symbol.classId, other.symbol.classId)
                assertNotSame(owner.symbol, other.symbol)
                assertNotSame(payload.symbol, field(other, "payload").symbol)
                assertNotSame(half.symbol, field(other, "half").symbol)
                assertSame(q1, other.moduleData)
                assertProvenance(other, q1Path, 0)
                assertProvenance(field(other, "payload"), q1Path, 2)
                assertSame(value, constantValue(topLevelVariable(provider, "consumer", "selectedAgain")))
                assertSame(variable, topLevelVariable(provider, "consumer", "selected"))
                assertSame(payload.symbol, value.fields.keys.first())
                assertSame(owner.symbol, assertIs<ConeClassLikeType>(value.type).lookupTag.toSymbol(p.session))
                assertSame(selected, provider.selectImportedSnapshot(p, "parent"))
            }
        }
    }

    /** 合法 writer 输入编码后只破坏一个必需 v 引用；后续失败不能替换先前完成的 v0。 */
    @Test
    fun invalidRequiredValueReferencePreservesPreviouslyCompletedValues(@TempDir root: Path) {
        val fixture = fixture(root, graphMetadata()) { wire ->
            val right = assertNotNull(assertNotNull(wire.allValues(2)).fields(1))
            compositeReference(right).mutateIdx(wire.allValuesLength.toUInt())
        }
        assertFailurePreservesCompletedLeaf(fixture, "pair")
    }

    /** 必需字段 t=0 或越界都必须失败，不能用声明字段的类型或 Unit 掩盖损坏。 */
    @Test
    fun invalidFieldTypePreservesPreviouslyCompletedValuesAndRetries(@TempDir root: Path) {
        for (missing in listOf(true, false)) {
            val original = graphMetadata()
            val values = original.values.toMutableList()
            values[2] = values[2].copy(fields = listOf(
                valueField("left", 1u, CjoCompositeConstValue(0u)),
                valueField("right", 1u, CjoCompositeConstValue(1u)),
            ))
            val fixture = fixture(root.resolve(missing.toString()), original.copy(values = values)) { wire ->
                // pair.left 先访问已完成 v0，pair.right 才访问本次新增 v1 的坏字段。
                val field = assertNotNull(assertNotNull(wire.allValues(1)).fields(0))
                assertTrue(field.mutateType(if (missing) 0u else (wire.allTypesLength + 1).toUInt()))
            }
            assertFailurePreservesCompletedLeaf(fixture, "pair")
            // 换顶层入口仍必须拒绝本次失败图中的同一 v1，不能命中遗留壳。
            assertFailurePreservesCompletedLeaf(fixture, "equal")
        }
    }

    /** 对象自身的必需 type 不能由顶层变量的静态 Leaf 类型代填。 */
    @Test
    fun invalidCompositeTypeRejectsWithoutReplacingCompletedValues(@TempDir root: Path) {
        for (missing in listOf(true, false)) {
            val fixture = fixture(root.resolve(missing.toString()), graphMetadata()) { wire ->
                val value = assertNotNull(wire.allValues(1))
                assertTrue(value.mutateType(if (missing) 0u else (wire.allTypesLength + 1).toUInt()))
            }
            assertFailurePreservesCompletedLeaf(fixture, "equal")
        }
    }

    /** 非空 VarInfo union 缺少实际声明 t 槽，不能仅凭 tag 发布有类型的成功值。 */
    @Test
    fun missingDeclarationTypeRejectsInsteadOfInventingAType(@TempDir root: Path) {
        val fixture = fixture(root, metadata("missingconstanttype", listOf(
            constant("good", 1u, CjoUInt64ConstValue(ULong.MAX_VALUE)),
            constant("bad", 1u, CjoUInt64ConstValue(1uL)),
        ), listOf(CjoTypeMetadata(TypeKind.UInt64)))) { wire ->
            assertTrue(assertNotNull(wire.allDecls(1)).mutateType(0u))
        }
        val goodVariable = fixture.variable("good")
        val completedResult = goodVariable.evaluatedInitializer
        val good = constantValue(goodVariable)
        repeat(3) {
            assertInvalidConstant { fixture.variable("bad") }
            assertSame(goodVariable, fixture.variable("good"))
            assertSame(completedResult, goodVariable.evaluatedInitializer)
            assertSame(good, fixture.value("good"))
            assertEquals(BigInteger("18446744073709551615"),
                primitive(good, ConePrimitiveType.UINT64, CfirLiteralKind.INT).value)
        }
    }

    /** 失败请求不得以残留对象壳在下一次请求成功，也不得重填已经发布的字段集合。 */
    private fun assertFailurePreservesCompletedLeaf(fixture: Fixture, badName: String) {
        val variable = fixture.variable("first")
        val result = variable.evaluatedInitializer
        val completed = assertIs<CfirConstantValue.ObjectValue>(constantValue(variable))
        val fields = completed.fields
        val numberSymbol = fields.keys.single()
        val numberValue = fields.getValue(numberSymbol)
        repeat(3) {
            assertInvalidConstant { fixture.variable(badName) }
            assertSame(variable, fixture.variable("first"))
            assertSame(result, variable.evaluatedInitializer)
            assertSame(completed, fixture.value("first"))
            assertSame(completed, fixture.value("second"))
            assertSame(fields, completed.fields)
            assertEquals(listOf(numberSymbol), fields.keys.toList())
            assertSame(numberValue, fields.getValue(numberSymbol))
            assertEquals(BigInteger.valueOf(42),
                primitive(numberValue, ConePrimitiveType.INT64, CfirLiteralKind.INT).value)
        }
    }

    /** 仅接受无效输入的显式拒绝；StackOverflow/断言失败/意外运行期异常不能冒充负例成功。 */
    private fun assertInvalidConstant(action: () -> Unit) {
        val failure = assertFails(block = action)
        assertTrue(failure is IllegalArgumentException || failure is IllegalStateException,
            "Expected an explicit invalid-constant rejection, got " + failure.javaClass.name)
        assertTrue(assertNotNull(failure.message).isNotBlank())
    }

    private fun primitive(value: CfirConstantValue, type: ConeCangJieType,
                          kind: CfirLiteralKind): CfirConstantValue.Primitive =
        assertIs<CfirConstantValue.Primitive>(value).also {
            assertEquals(type, it.type)
            assertEquals(kind, it.kind)
        }

    /** 唯一语义观察入口是变量正式求值产品，不读取 decoder cache 或 context 内部 working 图。 */
    private fun constantValue(variable: CfirFieldVariable): CfirConstantValue {
        val result = assertIs<CfirEvaluatorResult.Evaluated>(variable.evaluatedInitializer)
        val expression = assertIs<CfirConstantValueExpression>(result.result)
        assertSame(expression.constantValue.type, expression.coneTypeOrNull)
        return expression.constantValue
    }

    private fun topLevelVariable(provider: CfirDeserializedSymbolProvider, packageName: String,
                                 name: String): CfirFieldVariable =
        assertIs<CfirFieldVariable>(provider.getTopLevelCallableSymbols(FqName(packageName), Name.identifier(name)).single().cfir)

    private fun field(owner: CfirClass, name: String): CfirFieldVariable =
        owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == name }

    private fun assertProvenance(declaration: CfirDeclaration, path: Path, index: Int) {
        val origin = assertNotNull(declaration.cjoDeclarationOrigin)
        assertEquals(path.toAbsolutePath().normalize(), Path.of(origin.packagePath).toAbsolutePath().normalize())
        assertEquals(index, origin.declarationIndex)
    }

    private fun constant(name: String, type: UInt, value: CjoConstValueInfo?): CjoPackageDeclaration =
        CjoPackageDeclaration(name, DeclKind.VarDecl, type = type,
            attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoVariableInfo(isConst = true, constValue = value))

    private fun member(name: String, type: UInt, exportId: String? = null): CjoPackageDeclaration =
        CjoPackageDeclaration(name, DeclKind.VarDecl, isTopLevel = false, type = type,
            exportId = exportId, attributes = flags(Attribute.PUBLIC), info = CjoVariableInfo())

    private fun nominal(declaration: UInt, arguments: List<UInt> = emptyList()): CjoTypeMetadata =
        CjoTypeMetadata(TypeKind.Class, arguments, semanticInfo = CjoCompositeTypeInfoMetadata(declaration))

    private fun valueField(name: String, type: UInt, value: CjoConstValueInfo): CjoCompositeValueFieldMetadata =
        CjoCompositeValueFieldMetadata(name, type, value)

    /** 实例化 storage type 和 field symbol 来自真实 v loader，不能按字段声明的 T 代替 UInt64。 */
    @Test
    fun exportPreparationCapturesConstantStorageTypesAndCanonicalFields(@TempDir root: Path) {
        val fixture = fixture(root, genericMetadata("r38.constants"))
        val variable = fixture.variable("box")
        val value = assertIs<CfirConstantValue.ObjectValue>(constantValue(variable))
        val guard = R38ExportPreparationGuard(fixture.module.session, fixture.provider)
        val roots = listOf<CfirDeclaration>(variable)
        val prepared = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
            emptyList(), roots, ::r38PhysicalMembers)
        guard.observeOwners(prepared.declarations)
        val owner = fixture.classifier("Box")
        val payload = field(owner, "payload")
        val parameter = assertIs<CfirTypeParameter>(owner.typeParameters.single())
        assertSame(parameter.symbol, assertIs<org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType>(
            payload.returnTypeRef.coneType).lookupTag.typeParameterSymbol)
        assertTrue(prepared.declarations.any { it === payload })
        assertTrue(prepared.declarations.any { it === owner })
        assertTrue(prepared.declarations.any { it === parameter })
        val snapshot = guard.readOnly {
            org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
                emptyList(), roots, ::r38PhysicalMembers, preparedInput = prepared)
        }
        guard.readOnly {
            val graph = assertNotNull(snapshot.constantValueOf(variable))
            val node = assertIs<org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantNode.Object>(
                graph.nodes.single { it.identity == graph.root })
            val storage = node.fields.single { it.symbol === payload.symbol }
            assertEquals(ConePrimitiveType.UINT64, storage.type)
            assertSame(storage.type, snapshot.typeElement(storage.type, fixture.module.session).identity)
            assertSame(value.type, node.type)
            kotlin.test.assertFalse(snapshot.hasCanonicalSourceOf(payload))
            snapshot.validateSourceUnchanged()
        }
    }

    /** d/t 为 1-based；v 为零基 ordinal。类型和成员声明均有真实 class body/FullId。 */
    private fun graphMetadata(): CjoPackageMetadata = metadata("constantgraph", listOf(
        CjoPackageDeclaration("Leaf", DeclKind.ClassDecl, type = 1u, attributes = flags(Attribute.PUBLIC),
            info = CjoClassInfo(body = listOf(2u))),
        member("number", 2u),
        CjoPackageDeclaration("Pair", DeclKind.ClassDecl, type = 3u, attributes = flags(Attribute.PUBLIC),
            info = CjoClassInfo(body = listOf(4u, 5u))),
        member("left", 1u), member("right", 1u),
        CjoPackageDeclaration("Ring", DeclKind.ClassDecl, type = 4u, attributes = flags(Attribute.PUBLIC),
            info = CjoClassInfo(body = listOf(7u, 8u))),
        member("next", 4u), member("number", 2u),
        constant("first", 1u, CjoCompositeConstValue(0u)),
        constant("second", 1u, CjoCompositeConstValue(0u)),
        constant("equal", 1u, CjoCompositeConstValue(1u)),
        constant("pair", 3u, CjoCompositeConstValue(2u)),
        constant("self", 4u, CjoCompositeConstValue(3u)),
        constant("cycleA", 4u, CjoCompositeConstValue(4u)),
        constant("cycleB", 4u, CjoCompositeConstValue(5u)),
    ), listOf(nominal(1u), CjoTypeMetadata(TypeKind.Int64), nominal(3u), nominal(6u)), values = listOf(
        CjoCompositeValueMetadata(1u, listOf(valueField("number", 2u, CjoInt64ConstValue(42L)))),
        CjoCompositeValueMetadata(1u, listOf(valueField("number", 2u, CjoInt64ConstValue(42L)))),
        CjoCompositeValueMetadata(3u, listOf(
            valueField("left", 1u, CjoCompositeConstValue(0u)), valueField("right", 1u, CjoCompositeConstValue(0u)))),
        CjoCompositeValueMetadata(4u, listOf(
            valueField("next", 4u, CjoCompositeConstValue(3u)), valueField("number", 2u, CjoInt64ConstValue(11L)))),
        CjoCompositeValueMetadata(4u, listOf(
            valueField("next", 4u, CjoCompositeConstValue(5u)), valueField("number", 2u, CjoInt64ConstValue(12L)))),
        CjoCompositeValueMetadata(4u, listOf(
            valueField("next", 4u, CjoCompositeConstValue(4u)), valueField("number", 2u, CjoInt64ConstValue(13L)))),
    ))

    /** Box<T>.payload 的未实例化声明类型是 T；allValues[0] 的实际对象为 Box<UInt64>。 */
    private fun genericMetadata(packageName: String): CjoPackageMetadata = metadata(packageName, listOf(
        CjoPackageDeclaration("Box", DeclKind.ClassDecl, type = 1u, exportId = "$packageName::Box",
            attributes = flags(Attribute.PUBLIC), generic = CjoGenericMetadata(listOf(2u)),
            info = CjoClassInfo(body = listOf(3u, 4u))),
        CjoPackageDeclaration("T", DeclKind.GenericParamDecl, isTopLevel = false, type = 2u,
            exportId = "$packageName::Box.T"),
        member("payload", 2u, "$packageName::Box.payload"),
        member("half", 4u, "$packageName::Box.half"),
        constant("box", 3u, CjoCompositeConstValue(0u)),
        constant("boxAgain", 3u, CjoCompositeConstValue(0u)),
    ), listOf(
        nominal(1u, listOf(2u)),
        CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(2u)),
        nominal(1u, listOf(5u)), CjoTypeMetadata(TypeKind.Float16), CjoTypeMetadata(TypeKind.UInt64),
    ), values = listOf(CjoCompositeValueMetadata(3u, listOf(
        valueField("payload", 5u, CjoUInt64ConstValue(ULong.MAX_VALUE)),
        valueField("half", 4u, CjoInt16ConstValue(0x4a40.toShort())),
    ))))

    private fun metadata(
        packageName: String,
        declarations: List<CjoPackageDeclaration>,
        types: List<CjoTypeMetadata>,
        values: List<CjoCompositeValueMetadata> = emptyList(),
        imports: List<String> = emptyList(),
    ): CjoPackageMetadata = CjoPackageMetadata(
        fullPackageName = packageName, moduleName = packageName,
        schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3, cjoVersion = CjoFormatVersion(0u, 1u, 0u),
        allFiles = listOf("typed-constant-schema.cj"), declarations = declarations,
        types = types, values = values, imports = imports,
    )

    private data class Fixture(
        val packageName: String,
        val path: Path,
        val module: Module,
        val manager: CjoManager,
        val provider: CfirDeserializedSymbolProvider,
        val wire: Package,
    ) {
        fun variable(name: String): CfirFieldVariable =
            assertIs<CfirFieldVariable>(provider.getTopLevelCallableSymbols(FqName(packageName), Name.identifier(name)).single().cfir)
        fun classifier(name: String): CfirClass =
            assertIs<CfirClass>(assertNotNull(provider.getClassLikeSymbolByClassId(
                ClassId(FqName(packageName), Name.identifier(name)))).cfir)
        fun value(name: String): CfirConstantValue {
            val evaluated = assertIs<CfirEvaluatorResult.Evaluated>(variable(name).evaluatedInitializer)
            return assertIs<CfirConstantValueExpression>(evaluated.result).constantValue
        }
    }

    /** 损坏注入在正式 writer 字节上、Manager 装载之前；之后仅使用真实不可变快照。 */
    private fun fixture(root: Path, input: CjoPackageMetadata, corrupt: ((Package) -> Unit)? = null): Fixture {
        Files.createDirectories(root)
        val path = root.resolve(input.fullPackageName + ".cjo").toAbsolutePath().normalize()
        val bytes = CjoPackageWriter.toByteArray(input)
        corrupt?.invoke(Package.getRootAsPackage(ByteBuffer.wrap(bytes)))
        Files.write(path, bytes)
        val manager = CjoManager(
            CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
        )
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.registerProvider(provider)
        val wire = assertNotNull(manager.loadPackage(input.fullPackageName))
        assertEquals(input.declarations.size, wire.allDeclsLength)
        assertEquals(input.types.size, wire.allTypesLength)
        assertEquals(input.values.size, wire.allValuesLength)
        return Fixture(input.fullPackageName, path, module, manager, provider, wire)
    }

    private fun variableInfo(wire: Package, index: Int): VarInfo =
        assertIs<VarInfo>(assertNotNull(wire.allDecls(index)).info(VarInfo()))

    /**
     * flatc 的 union accessor 仅接受 Table，而 CompositeValueIndex 是 Struct。
     * 此目标只暴露生成 accessor 指向的位置，以核对/损坏 wire idx；不做语义解码或物化。
     */
    private class UnionTarget : Table() {
        override fun __reset(position: Int, buffer: ByteBuffer) {
            bb_pos = position
            bb = buffer
        }
        fun reference(): CompositeValueIndex = CompositeValueIndex().__assign(bb_pos, getByteBuffer())
    }

    private fun compositeReference(info: VarInfo): CompositeValueIndex {
        assertEquals(ConstValue.CompositeValue, info.valueType)
        val target = UnionTarget()
        assertNotNull(info.value(target))
        return target.reference()
    }

    private fun compositeReference(field: MemberValue): CompositeValueIndex {
        assertEquals(ConstValue.CompositeValue, field.valueType)
        val target = UnionTarget()
        assertNotNull(field.value(target))
        return target.reference()
    }

    private fun flags(vararg attributes: Attribute): List<ULong> =
        MutableList(attributes.maxOf { it.ordinal / 64 } + 1) { 0uL }.apply {
            attributes.forEach { this[it.ordinal / 64] = this[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        }

    /** 与现有 CjoTypeGraphDeserializationTest 相同的真实 Library session 与模块装配。 */
    private class Module(
        moduleName: String = "cjo-typed-constant-graph-test",
        override val dependencies: List<CfirModuleData> = emptyList(),
    ) : CfirModuleData() {
        override val name = Name.identifier(moduleName)
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = moduleName
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            it.register(TypeComponents::class, TypeComponents(it))
            it.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            it.registerTypeAwareSupertypeComponents()
        }
        init { bindSession(session) }

        fun registerProvider(provider: CfirSymbolProvider) {
            session.register(CfirSymbolProvider::class, provider)
            session.register(CfirExtendProvider::class, CfirExtendProviderComposer.fromSymbolProviders(listOf(provider)))
        }
    }

    private data class HalfCase(val bits: Int, val doubleBits: ULong)

    /** 显式 IEEE binary16 → binary64 位向量，包括 signed/signaling NaN 的原始 fraction。 */
    private val halfCases = listOf(
        HalfCase(0x0000, 0x0000000000000000uL),
        HalfCase(0x8000, 0x8000000000000000uL),
        HalfCase(0x0001, 0x3e70000000000000uL),
        HalfCase(0x8001, 0xbe70000000000000uL),
        HalfCase(0x03ff, 0x3f0ff80000000000uL),
        HalfCase(0x0400, 0x3f10000000000000uL),
        HalfCase(0x3c00, 0x3ff0000000000000uL),
        HalfCase(0x4a40, 0x4029000000000000uL),
        HalfCase(0x7bff, 0x40effc0000000000uL),
        HalfCase(0x7c00, 0x7ff0000000000000uL),
        HalfCase(0xfc00, 0xfff0000000000000uL),
        HalfCase(0x7e01, 0x7ff8040000000000uL),
        HalfCase(0xfe01, 0xfff8040000000000uL),
        HalfCase(0x7c01, 0x7ff0040000000000uL),
        HalfCase(0xfc01, 0xfff0040000000000uL),
    )
}
