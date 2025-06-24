public class Main {
	
    public static void main(String[] args) {
    	Object o = new Object();
    	o = (Object) new String();
    	
    	boolean b1 = o instanceof Object; 
    	boolean b2 = o instanceof String; 
    	
    }
}