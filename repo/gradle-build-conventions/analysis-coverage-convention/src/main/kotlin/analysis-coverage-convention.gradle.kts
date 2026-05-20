import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    id("org.jetbrains.kotlinx.kover")
}

val moduleThresholds = mapOf(
    // 覆盖率门禁按当前仓库测试基线设置；后续提高测试覆盖时只允许上调这些阈值。
    ":analysis:analysis-api-cfir" to Pair(35, 24),
    ":analysis:cj-references" to Pair(0, 0),
    ":analysis:light-declarations" to Pair(70, 46),
    ":analysis:symbol-light-declarations" to Pair(0, 0),
    ":analysis:stubs" to Pair(0, 0),
    ":analysis:decompiled" to Pair(0, 0),
)

val thresholds = moduleThresholds[project.path]

kover {
    currentProject {
        sources {
            includedSourceSets.add("main")
        }
    }

    reports {
        total {
            filters {
                excludes {
                    annotatedBy("*Generated*")
                }
            }

            html {
                onCheck = false
            }

            xml {
                onCheck = false
            }

            verify {
                if (thresholds != null) {
                    rule("analysis module coverage") {
                        groupBy = GroupingEntityType.APPLICATION

                        bound {
                            minValue = thresholds.first
                            coverageUnits = CoverageUnit.LINE
                            aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                        }

                        bound {
                            minValue = thresholds.second
                            coverageUnits = CoverageUnit.BRANCH
                            aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                        }
                    }
                }
            }
        }
    }
}
