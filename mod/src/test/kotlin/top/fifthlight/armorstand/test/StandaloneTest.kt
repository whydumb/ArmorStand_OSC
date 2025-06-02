package top.fifthlight.armorstand.test

import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import top.fifthlight.armorstand.test.std140.Std140Test

@Suite
@SelectClasses(Std140Test::class)
class StandaloneTest
