package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestSpecBase
import org.cangnova.cangjie.test.CfirParser
import org.junit.jupiter.api.Test

/** PSI 入口运行本轮全部 SOURCE 存储成员实例化 fixture。 */
class CfirCjmpStoredMemberInstantiationTest : AbstractCfirCjmpStoredMemberInstantiationTest(CfirParser.Psi)

/** LightTree 入口继承同一组测试，逐项运行全部 SOURCE fixture。 */
class CfirCjmpStoredMemberInstantiationLightTreeTest : AbstractCfirCjmpStoredMemberInstantiationTest(CfirParser.LightTree)

/**
 * C3 语义依据：20261113-c3-generic-member-identity-probes 的 cjc 1.1.3 result.json 与 source-trace-v113.json。
 * 使用已有规格诊断管线；common/specific 模式及 SOURCE refinement 来自 fixture 的模块指令。
 * 期望只取原生已执行输入，诊断主范围采用完整类型引用、完整声明及修饰符 token。
 * 不根据声明名字、匹配指针或 common/specific 角色切换编译行为，也不在测试中重建成员表算法。
 *
 * 本轮覆盖声明泛型域 counterpart 更新、构造器及 private 补入实例副本、继承与 unmatched 晚期撞型。
 * 余额：真实 CJO roundtrip；不同 scope、private extend、不同继承路径及同声明多路径、
 * 自身 GENERIC 排除、static/非函数类别、具体 parent 与 abstract child 等未执行边界。
 * combined String 仅有 f 诊断，g 完整性未知；这些余额不作为本套件已验证的语义。
 */
abstract class AbstractCfirCjmpStoredMemberInstantiationTest(parser: CfirParser) : AbstractCfirDiagnosticTestSpecBase(parser) {

    @Test
    fun nominalStoredOverloadsCollideAfterStringSubstitution() = runTest(ROOT + "nominalStringCollision.cj")

    @Test
    fun nominalStoredOverloadsRemainDistinctAfterInt64Substitution() = runTest(ROOT + "nominalInt64NoCollision.cj")

    @Test
    fun extendStoredOverloadsCollideOnlyAfterStringSubstitution() = runTest(ROOT + "extendStringCollision.cj")

    @Test
    fun extendStoredOverloadsRemainDistinctAfterInt64Substitution() = runTest(ROOT + "extendInt64NoCollision.cj")

    @Test
    fun constructorFixedAndGenericCandidatesCollideAfterMarkerSubstitution() = runTest(ROOT + "constructorMarkerCollision.cj")

    @Test
    fun constructorExactCounterpartUpdateDoesNotCreateDuplicateCandidate() = runTest(ROOT + "constructorExactCounterpartNoCollision.cj")

    @Test
    fun constructorFixedAndGenericCandidatesRemainDistinctForInt64() = runTest(ROOT + "constructorInt64NoCollision.cj")

    @Test
    fun inheritedStoredCounterpartAndFixedDefaultCollideAfterMarkerSubstitution() = runTest(ROOT + "inheritedCounterpartMarkerCollision.cj")

    @Test
    fun privateNominalMembersAreReaddedBeforeInt64CollisionCheck() = runTest(ROOT + "privateNominalInt64Collision.cj")

    @Test
    fun privateNominalMembersRemainDistinctAfterInt32Substitution() = runTest(ROOT + "privateNominalInt32NoCollision.cj")

    @Test
    fun unmatchedNominalMemberStillParticipatesInLateStringCollision() = runTest(ROOT + "unmatchedNominalStringCollision.cj")

    @Test
    fun unmatchedExtendMemberStillParticipatesInLateStringCollision() = runTest(ROOT + "unmatchedExtendStringCollision.cj")

    private companion object {
        const val ROOT = "cfir/analysis-tests/testData/diagnostics2/common-specific/stored-member-instantiation/"
    }
}
