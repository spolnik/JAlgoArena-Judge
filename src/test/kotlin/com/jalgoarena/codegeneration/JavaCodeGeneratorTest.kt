package com.jalgoarena.codegeneration

import com.jalgoarena.judge.Function
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JavaCodeGeneratorTest {

    @Test
    fun generates_skeleton_function_for_TWO_SUM() {
        val twoSumGenerated = JavaCodeGenerator.generateEmptyFunction(TWO_SUM)

        val twoSumExpected = """import java.util.*;
import com.jalgoarena.type.*;

public class Solution {
    /**
     * @param numbers An array of Integers
     * @param target target = numbers[index1] + numbers[index2]
     * @return [index1 + 1, index2 + 1] (index1 < index2)
     */
    public int[] twoSum(int[] numbers, int target) {
        // Write your code here
    }
}
"""
        assertThat(twoSumGenerated).isEqualTo(twoSumExpected)
    }

    @Test
    fun generates_skeleton_function_for_WORD_LADDER() {
        val wordLadderGenerated = JavaCodeGenerator.generateEmptyFunction(WORD_LADDER)

        val wordLadderExpected = """import java.util.*;
import com.jalgoarena.type.*;

public class Solution {
    /**
     * @param begin_word the begin word
     * @param end_word the end word
     * @param dict the dictionary
     * @return The shortest length
     */
    public int ladderLength(String begin_word, String end_word, HashSet dict) {
        // Write your code here
    }
}
"""

        assertThat(wordLadderGenerated).isEqualTo(wordLadderExpected)
    }

    companion object {


        private val TWO_SUM = Function("twoSum",
                Function.Return("[I",
                        "[index1 + 1, index2 + 1] (index1 < index2)"),
                        arrayOf(Function.Parameter("numbers", "[I", "An array of Integers"),
                                Function.Parameter("target", "java.lang.Integer",
                                "target = numbers[index1] + numbers[index2]")
                        )
        )

        private val WORD_LADDER = Function("ladderLength",
                Function.Return("java.lang.Integer", "The shortest length"),
                        arrayOf(Function.Parameter("begin_word", "java.lang.String", "the begin word"),
                                Function.Parameter("end_word", "java.lang.String", "the end word"),
                                Function.Parameter("dict", "java.util.HashSet", "the dictionary")
                        )
        )
    }
}