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
        JavaNumericType byteType = ByteType.INSTANCE;
        assert byteType instanceof ByteType : "Instance should be a ByteType";

        assert byteType.canBeAssignedTo(ByteType.INSTANCE) : "ByteType should be assignable to ByteType (self)";
        assert byteType.canBeAssignedTo(ShortType.INSTANCE) : "ByteType should be assignable to ShortType";
        assert byteType.canBeAssignedTo(IntType.INSTANCE) : "ByteType should be assignable to IntType";
        assert byteType.canBeAssignedTo(LongType.INSTANCE) : "ByteType should be assignable to LongType";
        assert byteType.canBeAssignedTo(FloatType.INSTANCE) : "ByteType should be assignable to FloatType";
        assert byteType.canBeAssignedTo(DoubleType.INSTANCE) : "ByteType should be assignable to DoubleType";

        assert byteType.commonSupertype(ByteType.INSTANCE).equals(ByteType.INSTANCE) : "Common supertype of Byte and Byte should be Byte";
        assert byteType.commonSupertype(ShortType.INSTANCE).equals(ShortType.INSTANCE) : "Common supertype of Byte and Short should be Short";
        assert byteType.commonSupertype(IntType.INSTANCE).equals(IntType.INSTANCE) : "Common supertype of Byte and Int should be Int";
        assert byteType.commonSupertype(LongType.INSTANCE).equals(LongType.INSTANCE) : "Common supertype of Byte and Long should be Long";
        assert byteType.commonSupertype(FloatType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Byte and Float should be Float";
        assert byteType.commonSupertype(DoubleType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Byte and Double should be Double";
    }

    @Test
    public void testShortType() {
        JavaNumericType shortType = ShortType.INSTANCE;
        assert shortType instanceof ShortType : "Instance should be a ShortType";

        assert !shortType.canBeAssignedTo(ByteType.INSTANCE) : "ShortType should NOT be assignable to ByteType";
        assert shortType.canBeAssignedTo(ShortType.INSTANCE) : "ShortType should be assignable to ShortType (self)";
        assert shortType.canBeAssignedTo(IntType.INSTANCE) : "ShortType should be assignable to IntType";
        assert shortType.canBeAssignedTo(LongType.INSTANCE) : "ShortType should be assignable to LongType";
        assert shortType.canBeAssignedTo(FloatType.INSTANCE) : "ShortType should be assignable to FloatType";
        assert shortType.canBeAssignedTo(DoubleType.INSTANCE) : "ShortType should be assignable to DoubleType";

        assert shortType.commonSupertype(ByteType.INSTANCE).equals(ShortType.INSTANCE) : "Common supertype of Short and Byte should be Short";
        assert shortType.commonSupertype(ShortType.INSTANCE).equals(ShortType.INSTANCE) : "Common supertype of Short and Short should be Short";
        assert shortType.commonSupertype(IntType.INSTANCE).equals(IntType.INSTANCE) : "Common supertype of Short and Int should be Int";
        assert shortType.commonSupertype(LongType.INSTANCE).equals(LongType.INSTANCE) : "Common supertype of Short and Long should be Long";
        assert shortType.commonSupertype(FloatType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Short and Float should be Float";
        assert shortType.commonSupertype(DoubleType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Short and Double should be Double";
    }

    @Test
    public void testIntType() {
        JavaNumericType intType = IntType.INSTANCE;
        assert intType instanceof IntType : "Instance should be an IntType";

        assert !intType.canBeAssignedTo(ByteType.INSTANCE) : "IntType should NOT be assignable to ByteType";
        assert !intType.canBeAssignedTo(ShortType.INSTANCE) : "IntType should NOT be assignable to ShortType";
        assert intType.canBeAssignedTo(IntType.INSTANCE) : "IntType should be assignable to IntType (self)";
        assert intType.canBeAssignedTo(LongType.INSTANCE) : "IntType should be assignable to LongType";
        assert intType.canBeAssignedTo(FloatType.INSTANCE) : "IntType should be assignable to FloatType";
        assert intType.canBeAssignedTo(DoubleType.INSTANCE) : "IntType should be assignable to DoubleType";

        assert intType.commonSupertype(ByteType.INSTANCE).equals(IntType.INSTANCE) : "Common supertype of Int and Byte should be Int";
        assert intType.commonSupertype(ShortType.INSTANCE).equals(IntType.INSTANCE) : "Common supertype of Int and Short should be Int";
        assert intType.commonSupertype(IntType.INSTANCE).equals(IntType.INSTANCE) : "Common supertype of Int and Int should be Int";
        assert intType.commonSupertype(LongType.INSTANCE).equals(LongType.INSTANCE) : "Common supertype of Int and Long should be Long";
        assert intType.commonSupertype(FloatType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Int and Float should be Float";
        assert intType.commonSupertype(DoubleType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Int and Double should be Double";
    }

    @Test
    public void testLongType() {
        JavaNumericType longType = LongType.INSTANCE;
        assert longType instanceof LongType : "Instance should be a LongType";

        assert !longType.canBeAssignedTo(ByteType.INSTANCE) : "LongType should NOT be assignable to ByteType";
        assert !longType.canBeAssignedTo(ShortType.INSTANCE) : "LongType should NOT be assignable to ShortType";
        assert !longType.canBeAssignedTo(IntType.INSTANCE) : "LongType should NOT be assignable to IntType";
        assert longType.canBeAssignedTo(LongType.INSTANCE) : "LongType should be assignable to LongType (self)";
        assert longType.canBeAssignedTo(FloatType.INSTANCE) : "LongType should be assignable to FloatType";
        assert longType.canBeAssignedTo(DoubleType.INSTANCE) : "LongType should be assignable to DoubleType";

        assert longType.commonSupertype(ByteType.INSTANCE).equals(LongType.INSTANCE) : "Common supertype of Long and Byte should be Long";
        assert longType.commonSupertype(ShortType.INSTANCE).equals(LongType.INSTANCE) : "Common supertype of Long and Short should be Long";
        assert longType.commonSupertype(IntType.INSTANCE).equals(LongType.INSTANCE) : "Common supertype of Long and Int should be Long";
        assert longType.commonSupertype(LongType.INSTANCE).equals(LongType.INSTANCE) : "Common supertype of Long and Long should be Long";
        assert longType.commonSupertype(FloatType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Long and Float should be Float";
        assert longType.commonSupertype(DoubleType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Long and Double should be Double";
    }

    @Test
    public void testFloatType() {
        JavaNumericType floatType = FloatType.INSTANCE;
        assert floatType instanceof FloatType : "Instance should be a FloatType";

        assert !floatType.canBeAssignedTo(ByteType.INSTANCE) : "FloatType should NOT be assignable to ByteType";
        assert !floatType.canBeAssignedTo(ShortType.INSTANCE) : "FloatType should NOT be assignable to ShortType";
        assert !floatType.canBeAssignedTo(IntType.INSTANCE) : "FloatType should NOT be assignable to IntType";
        assert !floatType.canBeAssignedTo(LongType.INSTANCE) : "FloatType should NOT be assignable to LongType";
        assert floatType.canBeAssignedTo(FloatType.INSTANCE) : "FloatType should be assignable to FloatType (self)";
        assert floatType.canBeAssignedTo(DoubleType.INSTANCE) : "FloatType should be assignable to DoubleType";

        assert floatType.commonSupertype(ByteType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Float and Byte should be Float";
        assert floatType.commonSupertype(ShortType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Float and Short should be Float";
        assert floatType.commonSupertype(IntType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Float and Int should be Float";
        assert floatType.commonSupertype(LongType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Float and Long should be Float";
        assert floatType.commonSupertype(FloatType.INSTANCE).equals(FloatType.INSTANCE) : "Common supertype of Float and Float should be Float";
        assert floatType.commonSupertype(DoubleType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Float and Double should be Double";
    }

    @Test
    public void testDoubleType() {
        JavaNumericType doubleType = DoubleType.INSTANCE;
        assert doubleType instanceof DoubleType : "Instance should be a DoubleType";

        assert !doubleType.canBeAssignedTo(ByteType.INSTANCE) : "DoubleType should NOT be assignable to ByteType";
        assert !doubleType.canBeAssignedTo(ShortType.INSTANCE) : "DoubleType should NOT be assignable to ShortType";
        assert !doubleType.canBeAssignedTo(IntType.INSTANCE) : "DoubleType should NOT be assignable to IntType";
        assert !doubleType.canBeAssignedTo(LongType.INSTANCE) : "DoubleType should NOT be assignable to LongType";
        assert !doubleType.canBeAssignedTo(FloatType.INSTANCE) : "DoubleType should NOT be assignable to FloatType";
        assert doubleType.canBeAssignedTo(DoubleType.INSTANCE) : "DoubleType should be assignable to DoubleType (self)";

        assert doubleType.commonSupertype(ByteType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Double and Byte should be Double";
        assert doubleType.commonSupertype(ShortType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Double and Short should be Double";
        assert doubleType.commonSupertype(IntType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Double and Int should be Double";
        assert doubleType.commonSupertype(LongType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Double and Long should be Double";
        assert doubleType.commonSupertype(FloatType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Double and Float should be Double";
        assert doubleType.commonSupertype(DoubleType.INSTANCE).equals(DoubleType.INSTANCE) : "Common supertype of Double and Double should be Double";
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
