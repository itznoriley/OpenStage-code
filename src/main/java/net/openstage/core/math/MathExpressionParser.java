package net.openstage.core.math;

import java.util.Locale;

public final class MathExpressionParser {
    public static final int MAX_EXPRESSION_LENGTH = 256;
    public static final int MAX_BRACKET_DEPTH = 5;
    public static final int MAX_OPERATIONS = 50;

    private MathExpressionParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static float evaluate(String expression, float t, float frame) throws ParseException {
        if (expression == null || expression.isBlank()) {
            throw new ParseException("Expression must not be null or blank");
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new ParseException("Expression exceeds maximum length of " + MAX_EXPRESSION_LENGTH);
        }

        String lower = expression.toLowerCase(Locale.ROOT);
        if (lower.contains("while") || lower.contains("for")) {
            throw new ParseException("Loops are not allowed in expressions");
        }

        Parser parser = new Parser(expression.trim(), t, frame);
        float result = parser.parseExpression();

        if (parser.pos < parser.input.length()) {
            throw new ParseException("Unexpected character at position " + parser.pos +
                                     ": '" + parser.input.charAt(parser.pos) + "'");
        }

        return result;
    }

    private static final class Parser {
        final String input;
        final float t;
        final float frame;
        int pos = 0;
        int depth = 0;
        int operations = 0;

        Parser(String input, float t, float frame) {
            this.input = input;
            this.t = t;
            this.frame = frame;
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            return pos < input.length() ? input.charAt(pos) : '\0';
        }

        private char consume() throws ParseException {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new ParseException("Unexpected end of expression");
            }
            return input.charAt(pos++);
        }

        private boolean matchChar(char c) {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        private void countOp() throws ParseException {
            operations++;
            if (operations > MAX_OPERATIONS) {
                throw new ParseException("Expression exceeds maximum of " +
                                         MAX_OPERATIONS + " operations");
            }
        }

        float parseExpression() throws ParseException {
            float value = parseTerm();
            while (true) {
                skipWhitespace();
                if (matchChar('+')) {
                    countOp();
                    value += parseTerm();
                } else if (matchChar('-')) {
                    countOp();
                    value -= parseTerm();
                } else {
                    break;
                }
            }
            return value;
        }

        private float parseTerm() throws ParseException {
            float value = parseUnary();
            while (true) {
                skipWhitespace();
                if (matchChar('*')) {
                    countOp();
                    value *= parseUnary();
                } else if (matchChar('/')) {
                    countOp();
                    float divisor = parseUnary();
                    if (divisor == 0.0f) {
                        value = 0.0f;
                    } else {
                        value /= divisor;
                    }
                } else {
                    break;
                }
            }
            return value;
        }

        private float parseUnary() throws ParseException {
            if (peek() == '-') {
                consume();
                countOp();
                return -parseAtom();
            }
            return parseAtom();
        }

        private float parseAtom() throws ParseException {
            skipWhitespace();

            if (peek() == '(') {
                consume();
                depth++;
                if (depth > MAX_BRACKET_DEPTH) {
                    throw new ParseException("Bracket nesting exceeds maximum depth of " +
                                             MAX_BRACKET_DEPTH);
                }
                float value = parseExpression();
                if (!matchChar(')')) {
                    throw new ParseException("Missing closing parenthesis");
                }
                depth--;
                return value;
            }

            if (pos < input.length() && (Character.isLetter(input.charAt(pos)) || input.charAt(pos) == '_')) {
                return parseIdentifierOrFunction();
            }

            return parseNumber();
        }

        private float parseIdentifierOrFunction() throws ParseException {
            int start = pos;
            while (pos < input.length() &&
                   (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_' || input.charAt(pos) == '.')) {
                pos++;
            }
            String token = input.substring(start, pos);

            if (token.equals("t")) {
                return t;
            }
            if (token.equals("frame")) {
                return frame;
            }

            return switch (token) {
                case "math.sin" -> {
                    countOp();
                    yield (float) Math.sin(Math.toRadians(parseFunctionArg()));
                }
                case "math.cos" -> {
                    countOp();
                    yield (float) Math.cos(Math.toRadians(parseFunctionArg()));
                }
                case "math.floor" -> {
                    countOp();
                    yield (float) Math.floor(parseFunctionArg());
                }
                default -> throw new ParseException("Unknown identifier: '" + token + "'");
            };
        }

        private float parseFunctionArg() throws ParseException {
            if (!matchChar('(')) {
                throw new ParseException("Expected '(' after function name");
            }
            depth++;
            if (depth > MAX_BRACKET_DEPTH) {
                throw new ParseException("Bracket nesting exceeds maximum depth of " +
                                         MAX_BRACKET_DEPTH);
            }
            float value = parseExpression();
            if (!matchChar(')')) {
                throw new ParseException("Missing closing parenthesis for function argument");
            }
            depth--;
            return value;
        }

        private float parseNumber() throws ParseException {
            skipWhitespace();
            int start = pos;
            boolean hasDot = false;
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' && !hasDot) {
                    hasDot = true;
                    pos++;
                } else {
                    break;
                }
            }
            if (pos == start) {
                throw new ParseException("Expected a number at position " + pos +
                                         (pos < input.length()
                                                 ? " but found '" + input.charAt(pos) + "'"
                                                 : " but reached end of expression"));
            }
            try {
                return Float.parseFloat(input.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid number: '" + input.substring(start, pos) + "'", e);
            }
        }
    }
}

