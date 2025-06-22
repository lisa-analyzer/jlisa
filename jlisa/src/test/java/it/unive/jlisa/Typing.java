package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.jlisa.program.type.*;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Typing {
    @Test
    public void testByteType() {
        JavaNumericType byteType = JavaByteType.INSTANCE;
        assert byteType instanceof JavaByteType : "Instance should be a ByteType";

        assert byteType.canBeAssignedTo(JavaByteType.INSTANCE) : "ByteType should be assignable to ByteType (self)";
        assert byteType.canBeAssignedTo(JavaShortType.INSTANCE) : "ByteType should be assignable to ShortType";
        assert byteType.canBeAssignedTo(JavaIntType.INSTANCE) : "ByteType should be assignable to IntType";
        assert byteType.canBeAssignedTo(JavaLongType.INSTANCE) : "ByteType should be assignable to LongType";
        assert byteType.canBeAssignedTo(JavaFloatType.INSTANCE) : "ByteType should be assignable to FloatType";
        assert byteType.canBeAssignedTo(JavaDoubleType.INSTANCE) : "ByteType should be assignable to DoubleType";

        assert byteType.commonSupertype(JavaByteType.INSTANCE).equals(JavaByteType.INSTANCE) : "Common supertype of Byte and Byte should be Byte";
        assert byteType.commonSupertype(JavaShortType.INSTANCE).equals(JavaShortType.INSTANCE) : "Common supertype of Byte and Short should be Short";
        assert byteType.commonSupertype(JavaIntType.INSTANCE).equals(JavaIntType.INSTANCE) : "Common supertype of Byte and Int should be Int";
        assert byteType.commonSupertype(JavaLongType.INSTANCE).equals(JavaLongType.INSTANCE) : "Common supertype of Byte and Long should be Long";
        assert byteType.commonSupertype(JavaFloatType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Byte and Float should be Float";
        assert byteType.commonSupertype(JavaDoubleType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Byte and Double should be Double";
    }

    @Test
    public void testShortType() {
        JavaNumericType shortType = JavaShortType.INSTANCE;
        assert shortType instanceof JavaShortType : "Instance should be a ShortType";

        assert !shortType.canBeAssignedTo(JavaByteType.INSTANCE) : "ShortType should NOT be assignable to ByteType";
        assert shortType.canBeAssignedTo(JavaShortType.INSTANCE) : "ShortType should be assignable to ShortType (self)";
        assert shortType.canBeAssignedTo(JavaIntType.INSTANCE) : "ShortType should be assignable to IntType";
        assert shortType.canBeAssignedTo(JavaLongType.INSTANCE) : "ShortType should be assignable to LongType";
        assert shortType.canBeAssignedTo(JavaFloatType.INSTANCE) : "ShortType should be assignable to FloatType";
        assert shortType.canBeAssignedTo(JavaDoubleType.INSTANCE) : "ShortType should be assignable to DoubleType";

        assert shortType.commonSupertype(JavaByteType.INSTANCE).equals(JavaShortType.INSTANCE) : "Common supertype of Short and Byte should be Short";
        assert shortType.commonSupertype(JavaShortType.INSTANCE).equals(JavaShortType.INSTANCE) : "Common supertype of Short and Short should be Short";
        assert shortType.commonSupertype(JavaIntType.INSTANCE).equals(JavaIntType.INSTANCE) : "Common supertype of Short and Int should be Int";
        assert shortType.commonSupertype(JavaLongType.INSTANCE).equals(JavaLongType.INSTANCE) : "Common supertype of Short and Long should be Long";
        assert shortType.commonSupertype(JavaFloatType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Short and Float should be Float";
        assert shortType.commonSupertype(JavaDoubleType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Short and Double should be Double";
    }

    @Test
    public void testIntType() {
        JavaNumericType intType = JavaIntType.INSTANCE;
        assert intType instanceof JavaIntType : "Instance should be an IntType";

        assert !intType.canBeAssignedTo(JavaByteType.INSTANCE) : "IntType should NOT be assignable to ByteType";
        assert !intType.canBeAssignedTo(JavaShortType.INSTANCE) : "IntType should NOT be assignable to ShortType";
        assert intType.canBeAssignedTo(JavaIntType.INSTANCE) : "IntType should be assignable to IntType (self)";
        assert intType.canBeAssignedTo(JavaLongType.INSTANCE) : "IntType should be assignable to LongType";
        assert intType.canBeAssignedTo(JavaFloatType.INSTANCE) : "IntType should be assignable to FloatType";
        assert intType.canBeAssignedTo(JavaDoubleType.INSTANCE) : "IntType should be assignable to DoubleType";

        assert intType.commonSupertype(JavaByteType.INSTANCE).equals(JavaIntType.INSTANCE) : "Common supertype of Int and Byte should be Int";
        assert intType.commonSupertype(JavaShortType.INSTANCE).equals(JavaIntType.INSTANCE) : "Common supertype of Int and Short should be Int";
        assert intType.commonSupertype(JavaIntType.INSTANCE).equals(JavaIntType.INSTANCE) : "Common supertype of Int and Int should be Int";
        assert intType.commonSupertype(JavaLongType.INSTANCE).equals(JavaLongType.INSTANCE) : "Common supertype of Int and Long should be Long";
        assert intType.commonSupertype(JavaFloatType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Int and Float should be Float";
        assert intType.commonSupertype(JavaDoubleType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Int and Double should be Double";
    }

    @Test
    public void testLongType() {
        JavaNumericType longType = JavaLongType.INSTANCE;
        assert longType instanceof JavaLongType : "Instance should be a LongType";

        assert !longType.canBeAssignedTo(JavaByteType.INSTANCE) : "LongType should NOT be assignable to ByteType";
        assert !longType.canBeAssignedTo(JavaShortType.INSTANCE) : "LongType should NOT be assignable to ShortType";
        assert !longType.canBeAssignedTo(JavaIntType.INSTANCE) : "LongType should NOT be assignable to IntType";
        assert longType.canBeAssignedTo(JavaLongType.INSTANCE) : "LongType should be assignable to LongType (self)";
        assert longType.canBeAssignedTo(JavaFloatType.INSTANCE) : "LongType should be assignable to FloatType";
        assert longType.canBeAssignedTo(JavaDoubleType.INSTANCE) : "LongType should be assignable to DoubleType";

        assert longType.commonSupertype(JavaByteType.INSTANCE).equals(JavaLongType.INSTANCE) : "Common supertype of Long and Byte should be Long";
        assert longType.commonSupertype(JavaShortType.INSTANCE).equals(JavaLongType.INSTANCE) : "Common supertype of Long and Short should be Long";
        assert longType.commonSupertype(JavaIntType.INSTANCE).equals(JavaLongType.INSTANCE) : "Common supertype of Long and Int should be Long";
        assert longType.commonSupertype(JavaLongType.INSTANCE).equals(JavaLongType.INSTANCE) : "Common supertype of Long and Long should be Long";
        assert longType.commonSupertype(JavaFloatType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Long and Float should be Float";
        assert longType.commonSupertype(JavaDoubleType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Long and Double should be Double";
    }

    @Test
    public void testFloatType() {
        JavaNumericType floatType = JavaFloatType.INSTANCE;
        assert floatType instanceof JavaFloatType : "Instance should be a FloatType";

        assert !floatType.canBeAssignedTo(JavaByteType.INSTANCE) : "FloatType should NOT be assignable to ByteType";
        assert !floatType.canBeAssignedTo(JavaShortType.INSTANCE) : "FloatType should NOT be assignable to ShortType";
        assert !floatType.canBeAssignedTo(JavaIntType.INSTANCE) : "FloatType should NOT be assignable to IntType";
        assert !floatType.canBeAssignedTo(JavaLongType.INSTANCE) : "FloatType should NOT be assignable to LongType";
        assert floatType.canBeAssignedTo(JavaFloatType.INSTANCE) : "FloatType should be assignable to FloatType (self)";
        assert floatType.canBeAssignedTo(JavaDoubleType.INSTANCE) : "FloatType should be assignable to DoubleType";

        assert floatType.commonSupertype(JavaByteType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Float and Byte should be Float";
        assert floatType.commonSupertype(JavaShortType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Float and Short should be Float";
        assert floatType.commonSupertype(JavaIntType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Float and Int should be Float";
        assert floatType.commonSupertype(JavaLongType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Float and Long should be Float";
        assert floatType.commonSupertype(JavaFloatType.INSTANCE).equals(JavaFloatType.INSTANCE) : "Common supertype of Float and Float should be Float";
        assert floatType.commonSupertype(JavaDoubleType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Float and Double should be Double";
    }

    @Test
    public void testDoubleType() {
        JavaNumericType doubleType = JavaDoubleType.INSTANCE;
        assert doubleType instanceof JavaDoubleType : "Instance should be a DoubleType";

        assert !doubleType.canBeAssignedTo(JavaByteType.INSTANCE) : "DoubleType should NOT be assignable to ByteType";
        assert !doubleType.canBeAssignedTo(JavaShortType.INSTANCE) : "DoubleType should NOT be assignable to ShortType";
        assert !doubleType.canBeAssignedTo(JavaIntType.INSTANCE) : "DoubleType should NOT be assignable to IntType";
        assert !doubleType.canBeAssignedTo(JavaLongType.INSTANCE) : "DoubleType should NOT be assignable to LongType";
        assert !doubleType.canBeAssignedTo(JavaFloatType.INSTANCE) : "DoubleType should NOT be assignable to FloatType";
        assert doubleType.canBeAssignedTo(JavaDoubleType.INSTANCE) : "DoubleType should be assignable to DoubleType (self)";

        assert doubleType.commonSupertype(JavaByteType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Double and Byte should be Double";
        assert doubleType.commonSupertype(JavaShortType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Double and Short should be Double";
        assert doubleType.commonSupertype(JavaIntType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Double and Int should be Double";
        assert doubleType.commonSupertype(JavaLongType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Double and Long should be Double";
        assert doubleType.commonSupertype(JavaFloatType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Double and Float should be Double";
        assert doubleType.commonSupertype(JavaDoubleType.INSTANCE).equals(JavaDoubleType.INSTANCE) : "Common supertype of Double and Double should be Double";
    }
    @Test
    public void testTypingConversion() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/typing/A.java");
        frontend.parseFromListOfFile(inputs);

        LiSA lisa = TestHelpers.getLiSA("outputs/typing/typing-1");
        lisa.run(frontend.getProgram());
    }
}
