import it.unive.jlisa.program.java.constructs.doublew.DoubleValue;

public class Main {
	
    public static void main(String[] args) {
    	Character c = new Character('a');  
    	char d = c.charValue();
    	Integer i = new Integer(5);
    	int j = i.intValue();
    	char a = 'a';
    	boolean b0 = Character.isLetter(a);
    	boolean b1 = Character.isDigit(a);
    	boolean b2 = c.equals(new Character('a'));
    	
    	Integer k = Integer.valueOf(4);
    	Double dx = Double.valueOf(5.3);
    	double dd = dx.doubleValue();
    	long l = Double.doubleToRawLongBits(543);
    	String s1 = Double.toString(dd);
    	double d2 = Double.longBitsToDouble(l);
    	double d3 = Double.parseDouble("123.5");
    }
}