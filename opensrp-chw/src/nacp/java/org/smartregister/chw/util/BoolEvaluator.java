package org.smartregister.chw.util;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BoolEvaluator {

    public boolean evaluate(String expression) {
        return evaluatePostfix(toPostfix(expression));
    }

    private void handleOperator(StringBuilder output, Stack<String> operators, String token) {
        while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(token)) {
            output.append(operators.pop()).append(' ');
        }
        operators.push(token);
    }

    // Convert infix expression to postfix (Reverse Polish Notation)
    private String toPostfix(String infix) {
        StringBuilder output = new StringBuilder();
        Stack<String> operators = new Stack<>();

        // Regex to match operands (numbers, single-quoted strings, and variables) and operators/parentheses
        Pattern tokenPattern = Pattern.compile("\\d+\\.?\\d*|'[^']*'|[a-zA-Z]+|[+\\-*/^<>!=&|~]+|[()]");
        Matcher matcher = tokenPattern.matcher(infix);

        while (matcher.find()) {
            String token = matcher.group();

            if (token.matches("\\d+\\.?\\d*|'[^']*'|[a-zA-Z]+"))
                output.append(token).append(' ');

            else if (token.equals(")") || token.equals("("))
                handleParenthesis(output, operators, token);

            else if (isOperator(token))
                handleOperator(output, operators, token);

            else
                throw new IllegalArgumentException("Unexpected token: " + token);
        }

        while (!operators.isEmpty()) {
            output.append(operators.pop()).append(' ');
        }

        return output.toString();
    }

    private static void handleParenthesis(StringBuilder output, Stack<String> operators, String parenthesis) {
        if (parenthesis.equals("("))
            operators.push(parenthesis);
        else if (parenthesis.equals(")")) {
            while (!operators.isEmpty() && !operators.peek().equals("(")) {
                output.append(operators.pop()).append(' ');
            }
            if (!operators.isEmpty() && operators.peek().equals("(")) {
                operators.pop();
            }
        }
    }

    private static int precedence(String operator) {
        return "+-".contains(operator) ? 1 :
                "*/".contains(operator) ? 2 :
                        "^".contains(operator) ? 3 :
                                "<><=>===!=!~".contains(operator) ? 4 : -1;

    }

    private boolean evaluatePostfix(String postfix) {
        Stack<Object> stack = new Stack<>();
        Pattern tokenPattern = Pattern.compile("\\d+\\.?\\d*|'[^']*'|[a-zA-Z]+|[+\\-*/^()<>!=&|~]+");
        Matcher matcher = tokenPattern.matcher(postfix);

        while (matcher.find()) {
            String token = matcher.group();
            if (token.equals("true") || token.equals("false"))
                stack.push(token.equals("true") ? 1.0 : 0);
            else if (token.matches("\\d+\\.?\\d*"))
                stack.push(Double.parseDouble(token));
            else if (token.matches("'[^']*'"))
                stack.push(token.substring(1, token.length() - 1));
            else if (isOperator(token))
                operate(token, stack);
            else if (token.matches("\\w+"))
                stack.push(token);
            else
                throw new IllegalArgumentException("Unexpected token: " + token);
        }
        return stack.pop().equals(1.0);
    }

    private void operate(String token, Stack<Object> stack) {
        String error = "Unsupported operand type for operator: %s operand %s and %s";
        Object a = null;
        Object b = stack.pop();
        double answer;

        if (token.equals("!") && (b instanceof Double))
            answer = (double) b == 0 ? 1.0 : 0.0;
        else {
            a = stack.pop();
            answer = a instanceof Double && b instanceof Double
                    ? applyOperator(token, (Double) a, (Double) b)
                    //treating string and double operands differently
                    : isStringOperator(token) ? applyStringOperator(token, String.valueOf(a), String.valueOf(b))
                    : Double.MIN_VALUE;
        }
        if (answer == Double.MIN_VALUE) {
            throw new IllegalArgumentException(String.format(error, token, a, b));
        }
        stack.push(answer);
    }

    private boolean isOperator(String token) {
        return token.matches("[+\\-*/^<>!=&|~]+");
    }

    private boolean isStringOperator(String token) {
        return token.matches("[<>=~!]+");
    }

    private double applyOperator(String op, double a, double b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return a / b;
            case ">": return a > b ? 1.0 : 0.0;
            case "<": return a < b ? 1.0 : 0.0;
            case "==": return a == b ? 1.0 : 0.0;
            case "!=": return a != b ? 1.0 : 0.0;
            case ">=": return a >= b ? 1.0 : 0.0;
            case "<=": return a <= b ? 1.0 : 0.0;
            case "&": return (a != 0 && b != 0) ? 1.0 : 0.0;
            case "|": return (a != 0 || b != 0) ? 1.0 : 0.0;
            default: throw new IllegalArgumentException("Unsupported operator: " + op);
        }
    }

    private double applyStringOperator(String op, String a, String b) {
        switch (op) {
            case "==": return a.equals(b) ? 1.0 : 0.0;
            case "!=": return !a.equals(b) ? 1.0 : 0.0;
            case ">": return a.compareTo(b) > 0 ? 1.0 : 0.0;
            case "<": return a.compareTo(b) < 0 ? 1.0 : 0.0;
            case ">=": return a.compareTo(b) >= 0 ? 1.0 : 0.0;
            case "<=": return a.compareTo(b) <= 0 ? 1.0 : 0.0;
            case "~": return a.matches(b) ? 1.0 : 0.0;
            default: throw new IllegalArgumentException("Unsupported operator for strings: " + op);
        }
    }

}
