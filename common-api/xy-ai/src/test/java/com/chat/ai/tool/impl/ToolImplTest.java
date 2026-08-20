package com.chat.ai.tool.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolImplTest {

    @Test
    void currentTimeToolShouldReturnFormattedTime() {
        CurrentTimeTool tool = new CurrentTimeTool();
        String result = tool.execute("{\"timezone\":\"Asia/Shanghai\"}");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void calculatorToolShouldComputeExpression() {
        CalculatorTool tool = new CalculatorTool();
        String result = tool.execute("{\"expression\":\"1+2*3\"}");
        assertEquals("7", result);
    }

    @Test
    void calculatorToolShouldRejectInvalidChars() {
        CalculatorTool tool = new CalculatorTool();
        String result = tool.execute("{\"expression\":\"1+alert(1)\"}");
        assertTrue(result.contains("非法字符"));
    }
}
