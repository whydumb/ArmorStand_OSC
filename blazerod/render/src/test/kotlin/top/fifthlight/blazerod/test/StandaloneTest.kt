package top.fifthlight.blazerod.test

import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import top.fifthlight.blazerod.test.model.node.TransformMapTest
import top.fifthlight.blazerod.test.layout.Std140Test
import top.fifthlight.blazerod.test.layout.Std430Test

@Suite
@SelectClasses(Std140Test::class, Std430Test::class, TransformMapTest::class)
class StandaloneTest
