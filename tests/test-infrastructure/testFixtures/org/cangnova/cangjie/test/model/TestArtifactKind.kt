package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.services.CompilationStage

/**
 * ��ʾ `TestArtifactKind`�����ز��Ի�����ʩ�е����ݻ������̡�
 */
abstract class TestArtifactKind<R : ResultingArtifact<R>>(private val representation: String) {
    /**
     * ���� `shouldRunAnalysis`�������Ի�����ʩ���̶�ȡ��
     */
    open val shouldRunAnalysis: Boolean
        get() = true

    /**
     * ִ�� `toString` ��Ӧ�Ĳ��Ի�����ʩ������
     */
    override fun toString(): String {
        return representation
    }
}

/**
 * �ṩ `SourcesKind` ���������г��ز��Ի�����ʩ�Ĺ�����Ϊ������
 */
object SourcesKind : TestArtifactKind<ResultingArtifact.Source>("Sources")

/**
 * ��ʾ `FrontendKind`�����ز��Ի�����ʩ�е����ݻ������̡�
 */
abstract class FrontendKind<R : ResultingArtifact.FrontendOutput<R>>(representation: String) : TestArtifactKind<R>(representation) {
    /**
     * �ṩ `NoFrontend` ���������г��ز��Ի�����ʩ�Ĺ�����Ϊ������
     */
    object NoFrontend : FrontendKind<ResultingArtifact.FrontendOutput.Empty>("NoFrontend") {
        /**
         * ���� `shouldRunAnalysis`�������Ի�����ʩ���̶�ȡ��
         */
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}

/**
 * ��ʾ `BackendKind`�����ز��Ի�����ʩ�е����ݻ������̡�
 */
abstract class BackendKind<I : ResultingArtifact.BackendInput<I>>(representation: String) : TestArtifactKind<I>(representation) {
    /**
     * �ṩ `NoBackend` ���������г��ز��Ի�����ʩ�Ĺ�����Ϊ������
     */
    object NoBackend : BackendKind<ResultingArtifact.BackendInput.Empty>("NoBackend") {
        /**
         * ���� `shouldRunAnalysis`�������Ի�����ʩ���̶�ȡ��
         */
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}

/**
 * ��ʾ `ArtifactKind`�����ز��Ի�����ʩ�е����ݻ������̡�
 */
abstract class ArtifactKind<A : ResultingArtifact.Binary<A>>(
    representation: String,
    /**
     * ���� `producedBy`�������Ի�����ʩ���̶�ȡ��
     */
    val producedBy: CompilationStage,
) : TestArtifactKind<A>(representation) {
    /**
     * �ṩ `NoArtifact` ���������г��ز��Ի�����ʩ�Ĺ�����Ϊ������
     */
    object NoArtifact : ArtifactKind<ResultingArtifact.Binary.Empty>("NoArtifact", CompilationStage.FIRST) {
        /**
         * ���� `shouldRunAnalysis`�������Ի�����ʩ���̶�ȡ��
         */
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}
