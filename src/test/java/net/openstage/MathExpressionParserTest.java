package net.openstage;

import net.openstage.core.math.MathExpressionParser;
import net.openstage.core.math.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MathExpressionParser Security, Boundary, and Evaluation Tests")
class MathExpressionParserTest {
    private static final float EPSILON = 1e-5f;

    @Test
    @DisplayName("Input length > 256 characters must fail with ParseException")
    void testInputLengthExceedingLimitThrows() {
        StringBuilder sb257 = new StringBuilder();
        while (sb257.length() < 256) {
            sb257.append("1+");
        }
        sb257.append("1");
        assertTrue(sb257.length() > 256);

        ParseException ex = assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(sb257.toString(), 0.0f, 0.0f));
        assertTrue(ex.getMessage().contains("maximum length"),
                "Exception should report maximum length exceeded: " + ex.getMessage());

        String hugeInput = "1+".repeat(5000) + "1";
        assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(hugeInput, 0.0f, 0.0f));
    }

    @Test
    @DisplayName("Input length <= 256 characters passes length validation")
    void testInputLengthAtLimitPasses() {
        String expr = "1 + 1";
        String padded = " ".repeat(256 - expr.length()) + expr;
        assertEquals(256, padded.length());

        float result = assertDoesNotThrow(() ->
                MathExpressionParser.evaluate(padded, 0.0f, 0.0f));
        assertEquals(2.0f, result, EPSILON);
    }

    @Test
    @DisplayName("Parentheses nesting depth > 5 must fail with ParseException")
    void testBracketDepthExceedingLimitThrows() {
        String depth6 = "((((((1))))))";
        ParseException ex6 = assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(depth6, 0.0f, 0.0f));
        assertTrue(ex6.getMessage().contains("depth"),
                "Exception should mention depth limit: " + ex6.getMessage());

        String funcDepth6 = "math.sin((((((1))))))";
        assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(funcDepth6, 0.0f, 0.0f));

        assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate("((((((((42))))))))", 0.0f, 0.0f));
    }

    @Test
    @DisplayName("Parentheses nesting depth <= 5 succeeds")
    void testBracketDepthAtLimitPasses() {
        String depth5 = "(((((1)))))";
        float result = assertDoesNotThrow(() ->
                MathExpressionParser.evaluate(depth5, 0.0f, 0.0f));
        assertEquals(1.0f, result, EPSILON);
        String sequential = "(1) + (2) + (3) + (4) + (5) + (6)";
        float seqResult = assertDoesNotThrow(() ->
                MathExpressionParser.evaluate(sequential, 0.0f, 0.0f));
        assertEquals(21.0f, seqResult, EPSILON);
    }

    @Test
    @DisplayName("Operation count > 50 must fail with ParseException")
    void testOperationCountExceedingLimitThrows() {
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 51; i++) {
            sb.append("+1");
        }
        String expr51Ops = sb.toString();

        ParseException ex = assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(expr51Ops, 0.0f, 0.0f));
        assertTrue(ex.getMessage().contains("operations"),
                "Exception should report maximum operations exceeded: " + ex.getMessage());
    }

    @Test
    @DisplayName("Operation count <= 50 succeeds")
    void testOperationCountAtLimitPasses() {
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 50; i++) {
            sb.append("+1");
        }
        String expr50Ops = sb.toString();

        float result = assertDoesNotThrow(() ->
                MathExpressionParser.evaluate(expr50Ops, 0.0f, 0.0f));
        assertEquals(51.0f, result, EPSILON);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "while",
            "for",
            "WHILE",
            "FOR",
            "while(true)",
            "while (1)",
            "for (;;)",
            "WHILE(true)",
            "1 + while",
            "1 + for",
            "while_loop + 1"
    })
    @DisplayName("Prohibited loop tokens must be strictly rejected")
    void testProhibitedTokensRejected(String prohibited) {
        ParseException ex = assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(prohibited, 0.0f, 0.0f));
        assertTrue(ex.getMessage().contains("Loops are not allowed"),
                "Exception should reject loops: " + ex.getMessage());
    }

    @Test
    @DisplayName("math.sin operates in degrees: sin(90) == 1.0, sin(0) == 0.0, sin(270) == -1.0")
    void testMathSinDegrees() throws ParseException {
        assertEquals(1.0f, MathExpressionParser.evaluate("math.sin(90)", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("math.sin(0)", 0.0f, 0.0f), EPSILON);
        assertEquals(-1.0f, MathExpressionParser.evaluate("math.sin(270)", 0.0f, 0.0f), EPSILON);
        assertEquals(0.5f, MathExpressionParser.evaluate("math.sin(30)", 0.0f, 0.0f), EPSILON);
    }

    @Test
    @DisplayName("math.cos operates in degrees: cos(0) == 1.0, cos(180) == -1.0, cos(60) == 0.5")
    void testMathCosDegrees() throws ParseException {
        assertEquals(1.0f, MathExpressionParser.evaluate("math.cos(0)", 0.0f, 0.0f), EPSILON);
        assertEquals(-1.0f, MathExpressionParser.evaluate("math.cos(180)", 0.0f, 0.0f), EPSILON);
        assertEquals(0.5f, MathExpressionParser.evaluate("math.cos(60)", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("math.cos(90)", 0.0f, 0.0f), EPSILON);
    }

    @Test
    @DisplayName("math.floor works for positive and negative numbers: floor(2.9) == 2.0")
    void testMathFloor() throws ParseException {
        assertEquals(2.0f, MathExpressionParser.evaluate("math.floor(2.9)", 0.0f, 0.0f), EPSILON);
        assertEquals(2.0f, MathExpressionParser.evaluate("math.floor(2.0)", 0.0f, 0.0f), EPSILON);
        assertEquals(-3.0f, MathExpressionParser.evaluate("math.floor(-2.1)", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("math.floor(0.999)", 0.0f, 0.0f), EPSILON);
    }

    @Test
    @DisplayName("Variables t (time in seconds) and frame (frame number) are evaluated properly")
    void testVariablesTAndFrame() throws ParseException {
        float t = 2.5f;
        float frame = 60.0f;

        assertEquals(2.5f, MathExpressionParser.evaluate("t", t, frame), EPSILON);
        assertEquals(60.0f, MathExpressionParser.evaluate("frame", t, frame), EPSILON);
        assertEquals(62.5f, MathExpressionParser.evaluate("t + frame", t, frame), EPSILON);
        assertEquals(20.0f, MathExpressionParser.evaluate("frame / 3", t, frame), EPSILON);
        assertEquals(1.0f, MathExpressionParser.evaluate("math.sin(t * 36)", t, frame), EPSILON);
    }

    @Test
    @DisplayName("Division by zero safely evaluates to 0.0 without throwing ArithmeticException")
    void testDivisionByZero() throws ParseException {
        assertEquals(0.0f, MathExpressionParser.evaluate("1 / 0", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("0 / 0", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("-100 / 0", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("10 / (5 - 5)", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("1 / (0.0)", 0.0f, 0.0f), EPSILON);

        assertEquals(15.0f, MathExpressionParser.evaluate("10 + (20 / 0) + 5", 0.0f, 0.0f), EPSILON);
        assertEquals(0.0f, MathExpressionParser.evaluate("5 * (10 / 0)", 0.0f, 0.0f), EPSILON);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "\t\n",
            "(",
            ")",
            "((1)",
            "(1))",
            "1 +",
            "+ 1",
            "1 -",
            "1 *",
            "1 /",
            "1 + * 2",
            "1 2",
            "abc",
            "math.sin",
            "math.sin(",
            "math.sin()",
            "math.unknown(1)",
            "1.2.3",
            ".",
            "..",
            "--5"
    })
    @DisplayName("Malformed expressions must throw ParseException")
    void testMalformedExpressionsThrow(String malformed) {
        assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(malformed, 0.0f, 0.0f));
    }

    @Test
    @DisplayName("Null expression throws ParseException")
    void testNullExpressionThrows() {
        assertThrows(ParseException.class, () ->
                MathExpressionParser.evaluate(null, 0.0f, 0.0f));
    }

    @Test
    @DisplayName("Operator precedence (*, / before +, -) is respected")
    void testOperatorPrecedence() throws ParseException {
        assertEquals(14.0f, MathExpressionParser.evaluate("2 + 3 * 4", 0.0f, 0.0f), EPSILON);

        assertEquals(20.0f, MathExpressionParser.evaluate("(2 + 3) * 4", 0.0f, 0.0f), EPSILON);

        assertEquals(4.0f, MathExpressionParser.evaluate("10 - 4 - 2", 0.0f, 0.0f), EPSILON);

        assertEquals(2.0f, MathExpressionParser.evaluate("24 / 6 / 2", 0.0f, 0.0f), EPSILON);

        assertEquals(-12.0f, MathExpressionParser.evaluate("-3 * 4", 0.0f, 0.0f), EPSILON);
        assertEquals(-12.0f, MathExpressionParser.evaluate("4 * -3", 0.0f, 0.0f), EPSILON);
    }
}

