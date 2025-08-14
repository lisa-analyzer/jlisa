public class Main {
	
    public static void main(String[] args) {
    	Character c = new Character('a');  
    	char d = c.charValue();
    	Integer i = new Integer(5);
    	int j = i.intValue();
    	char a = 'a';
    	boolean b0 = Character.isLetter(a);
    	boolean b1 = Character.isDigit(a);
    }
}