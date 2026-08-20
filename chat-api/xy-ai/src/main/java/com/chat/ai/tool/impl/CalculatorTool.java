package com.chat.ai.tool.impl;

import com.chat.ai.tool.core.ToolExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 简单计算器工具
 */
@Slf4j
@Component("calculatorTool")
public class CalculatorTool implements ToolExecutor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode node = objectMapper.readTree(argumentsJson);
            if (!node.has("expression")) {
                return "错误：缺少 expression 参数";
            }
            String expression = node.get("expression").asText();
            // 简单安全校验：仅允许数字和基础运算符
            if (!expression.matches("[0-9+\\-*/().\\s]+")) {
                return "错误：表达式包含非法字符";
            }
            double result = evaluate(expression);
            // 若结果为整数则输出整数形式
            if (result == (long) result) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (Exception e) {
            log.warn("执行计算器工具失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 使用双栈法计算只包含 + - * / 和括号的表达式。
     */
    private double evaluate(String expression) {
        Deque<Double> values = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();

        char[] chars = expression.replaceAll("\\s+", "").toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isDigit(ch) || ch == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < chars.length && (Character.isDigit(chars[i]) || chars[i] == '.')) {
                    sb.append(chars[i]);
                    i++;
                }
                i--;
                values.push(Double.parseDouble(sb.toString()));
            } else if (ch == '(') {
                ops.push(ch);
            } else if (ch == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.pop(); // pop '('
            } else if (isOperator(ch)) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(ch)) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(ch);
            }
        }

        while (!ops.isEmpty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        }
        return values.pop();
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/';
    }

    private int precedence(char op) {
        return (op == '+' || op == '-') ? 1 : (op == '*' || op == '/') ? 2 : 0;
    }

    private double applyOp(char op, double b, double a) {
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> a / b;
            default -> throw new IllegalArgumentException("不支持的操作符: " + op);
        };
    }
}
