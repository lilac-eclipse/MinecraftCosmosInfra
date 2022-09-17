package com.lilaceclipse.minecraftcosmos.lambda

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SampleClassTest {

    @Test
    fun testGetMessage() {
        val sample = SampleClass()

        Assertions.assertEquals(3, sample.addTwo(1, 2))
    }
}