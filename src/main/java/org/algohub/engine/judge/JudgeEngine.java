package org.algohub.engine.judge;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.algohub.engine.compiler.java.CompileErrorException;
import org.algohub.engine.compiler.java.MemoryJavaCompiler;
import org.algohub.engine.pojo.Function;
import org.algohub.engine.pojo.JudgeResult;
import org.algohub.engine.pojo.Problem;
import org.algohub.engine.serde.ObjectMapperInstance;
import org.algohub.engine.type.InternalTestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JudgeEngine {

    private static final Pattern[] PATTERNS_FOR_FINDING_CLASS_NAME =
            new Pattern[]{Pattern.compile("public\\s+class\\s+(\\w+)\\s+"),
                    Pattern.compile("final\\s+public\\s+class\\s+(\\w+)\\s+"),
                    Pattern.compile("public\\s+final\\s+class\\s+(\\w+)\\s+"),};

    private static final String PACKAGE_NAME = "org.algohub";
    private static final String IMPORTS =
            "package " + PACKAGE_NAME + ";\n" + "import java.util.*;\n\n\n";
    private static final int IMPORTS_LINES = 4;

    private static JudgeResult judge(final Object clazz, final Method method,
                                     final InternalTestCase[] testCases, final Problem problem) throws JsonProcessingException {

        System.gc();
        Runtime runtime = Runtime.getRuntime();
        final long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        final long start = System.nanoTime();
        for (int i = 0; i < testCases.length; ++i) {
            final InternalTestCase testCase = testCases[i];
            final JudgeOneCaseResult oneResult = judge(clazz, method, testCase);
            if (!oneResult.correct) {
                return new JudgeResult(StatusCode.WRONG_ANSWER.toString(), null,
                        i + 1, testCases.length, -1, -1);
            }
        }
        final double time = (System.nanoTime() - start) / (1000.0 * 1000.0);
        final long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        final long memoryInKb = (memoryAfter - memoryBefore) / 1024;
        return new JudgeResult(StatusCode.ACCEPTED.toString(), null, testCases.length,
                testCases.length, time, memoryInKb);
    }

    private static JudgeOneCaseResult judge(final Object clazz, final Method method,
                                            final InternalTestCase testCase) throws JsonProcessingException {
        final Object output;
        try {
            output = method.invoke(clazz, testCase.getInput());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e.getMessage());
        }
        final JudgeOneCaseResult result = new JudgeOneCaseResult();
        if (equal(testCase.getOutput(), output)) {
            result.correct = true;
        } else {
            result.wrongOutput = ObjectMapperInstance.INSTANCE.writeValueAsString(output);
        }
        return result;
    }

    private static Optional<String> getClassName(final String javaCode) {
        for (final Pattern pattern : PATTERNS_FOR_FINDING_CLASS_NAME) {
            final Matcher matcher = pattern.matcher(javaCode);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    /**
     * More powerful than java.util.Objects.equals().
     */
    private static boolean equal(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        }

        boolean eq;
        if (a instanceof Object[] && b instanceof Object[]) {
            eq = Arrays.deepEquals((Object[]) a, (Object[]) b);
        } else if (a instanceof byte[] && b instanceof byte[]) {
            eq = Arrays.equals((byte[]) a, (byte[]) b);
        } else if (a instanceof short[] && b instanceof short[]) {
            eq = Arrays.equals((short[]) a, (short[]) b);
        } else if (a instanceof int[] && b instanceof int[]) {
            eq = Arrays.equals((int[]) a, (int[]) b);
        } else if (a instanceof long[] && b instanceof long[]) {
            eq = Arrays.equals((long[]) a, (long[]) b);
        } else if (a instanceof char[] && b instanceof char[]) {
            eq = Arrays.equals((char[]) a, (char[]) b);
        } else if (a instanceof float[] && b instanceof float[]) {
            eq = Arrays.equals((float[]) a, (float[]) b);
        } else if (a instanceof double[] && b instanceof double[]) {
            eq = Arrays.equals((double[]) a, (double[]) b);
        } else if (a instanceof boolean[] && b instanceof boolean[]) {
            eq = Arrays.equals((boolean[]) a, (boolean[]) b);
        } else {
            eq = a.equals(b);
        }
        return eq;
    }

    public JudgeResult judge(final Problem problem, final String userCode) throws JsonProcessingException {

        Problem.TestCase[] testCases = problem.getTestCases();
        Function function = problem.getFunction();

        final InternalTestCase[] internalTestCases = new InternalTestCase[testCases.length];
        for (int i = 0; i < testCases.length; ++i) {
            internalTestCases[i] = new InternalTestCase(testCases[i], function);
        }
        return judge(problem, internalTestCases, userCode);
    }

    private JudgeResult judge(final Problem problem, final InternalTestCase[] testCases,
                              final String userCode) throws JsonProcessingException {
        final Object clazz;
        final Method method;

        Function function = problem.getFunction();

        try {
            final String completeUserCode = IMPORTS + userCode;
            final Optional<String> className = getClassName(completeUserCode);
            if (!className.isPresent()) {
                return new JudgeResult("ClassNotFoundException: No public class found");
            }
            final Object[] tmp = MemoryJavaCompiler.INSTANCE
                    .compileMethod("org.algohub." + className.get(), function.getName(), completeUserCode);
            clazz = tmp[0];
            method = (Method) tmp[1];
        } catch (ClassNotFoundException e) {
            return new JudgeResult(e.getClass() + " : " + e.getMessage());
        } catch (CompileErrorException e) {
            return new JudgeResult(createFriendlyMessage(e.getMessage()));
        }

        return judge(clazz, method, testCases, problem);
    }

    private String createFriendlyMessage(final String errorMessage) {
        final StringBuilder sb = new StringBuilder();
        final String[] lines = errorMessage.split("\n");
        for (final String line : lines) {
            final int pos = line.indexOf(".java:");
            if (pos > 0) {
                // get the line number
                final int pos2 = line.indexOf(':', pos + ".java:".length());
                final int lineNumber;
                {
                    final String numberStr = line.substring(pos + ".java:".length(), pos2);
                    lineNumber = Integer.valueOf(numberStr) - IMPORTS_LINES;
                }
                final String friendlyMessage = "Line:" + lineNumber + line.substring(pos2);
                sb.append(friendlyMessage).append('\n');
            } else {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static class JudgeOneCaseResult {
        boolean correct;
        String wrongOutput;
    }
}
