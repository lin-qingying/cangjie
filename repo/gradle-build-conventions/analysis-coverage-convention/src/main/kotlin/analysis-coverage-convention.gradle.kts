import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    id("org.jetbrains.kotlinx.kover")
}

val moduleThresholds = mapOf(
    ":analysis:analysis-api-cfir" to Pair(80, 65),
    ":analysis:low-level-api-cfir" to Pair(80, 65),
    ":analysis:cj-references" to Pair(80, 65),
    ":analysis:light-declarations" to Pair(70, 55),
    ":analysis:symbol-light-declarations" to Pair(70, 55),
    ":analysis:stubs" to Pair(70, 55),
    ":analysis:decompiled" to Pair(70, 55),
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
