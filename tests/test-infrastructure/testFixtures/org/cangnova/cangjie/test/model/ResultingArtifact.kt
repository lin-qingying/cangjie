package org.cangnova.cangjie.test.model

/**
 * 表示 `ResultingArtifact`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class ResultingArtifact<A : ResultingArtifact<A>> {
    /**
     * 保存 `kind`，供测试模型在测试执行期间读取或传递。
     */
    abstract val kind: TestArtifactKind<A>

    /**
     * 表示 `Source`，承载测试模型中的配置数据、测试产物或处理步骤。
     */
    class Source : ResultingArtifact<Source>() {
        /**
         * 保存 `kind`，供测试模型在测试执行期间读取或传递。
         */
        override val kind: TestArtifactKind<Source>
            get() = SourcesKind
    }

    /**
     * 表示 `FrontendOutput`，承载测试模型中的配置数据、测试产物或处理步骤。
     */
    abstract class FrontendOutput<R : FrontendOutput<R>> : ResultingArtifact<R>() {
        /**
         * 保存 `kind`，供测试模型在测试执行期间读取或传递。
         */
        abstract override val kind: FrontendKind<R>

        /**
         * 提供 `Empty` 单例，集中承载测试模型的共享状态、常量或默认行为。
         */
        object Empty : FrontendOutput<Empty>() {
            /**
             * 保存 `kind`，供测试模型在测试执行期间读取或传递。
             */
            override val kind: FrontendKind<Empty>
                get() = FrontendKind.NoFrontend
        }
    }

    /**
     * 表示 `BackendInput`，承载测试模型中的配置数据、测试产物或处理步骤。
     */
    abstract class BackendInput<I : BackendInput<I>> : ResultingArtifact<I>() {
        /**
         * 保存 `kind`，供测试模型在测试执行期间读取或传递。
         */
        abstract override val kind: BackendKind<I>

        /**
         * 提供 `Empty` 单例，集中承载测试模型的共享状态、常量或默认行为。
         */
        object Empty : BackendInput<Empty>() {
            /**
             * 保存 `kind`，供测试模型在测试执行期间读取或传递。
             */
            override val kind: BackendKind<Empty>
                get() = BackendKind.NoBackend
        }
    }

    /**
     * 表示 `Binary`，承载测试模型中的配置数据、测试产物或处理步骤。
     */
    abstract class Binary<A : Binary<A>> : ResultingArtifact<A>() {
        /**
         * 保存 `kind`，供测试模型在测试执行期间读取或传递。
         */
        abstract override val kind: ArtifactKind<A>

        /**
         * 提供 `Empty` 单例，集中承载测试模型的共享状态、常量或默认行为。
         */
        object Empty : Binary<Empty>() {
            /**
             * 保存 `kind`，供测试模型在测试执行期间读取或传递。
             */
            override val kind: ArtifactKind<Empty>
                get() = ArtifactKind.NoArtifact
        }
    }
}
