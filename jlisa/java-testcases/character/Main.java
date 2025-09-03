public class Main{
	
	public static void main(String[] args) {
		
		Character c1 = new Character('a');
		//Character c2 = Character.valueOf('a');
		boolean b1 = Character.isDigit('0');
		boolean b2 = Character.isDefined('a');
		char char1 = Character.forDigit(2, 10);
		char char2 = Character.forDigit(10, 16);
		char char3 = Character.toLowerCase('H');
		char char4 = Character.toUpperCase('h');
		boolean b3 = Character.isJavaIdentifierPart(' ');
		
		// This returns true if passed 2 as argument
		boolean b4 = Character.isJavaIdentifierStart('+');
		
	}
	
}