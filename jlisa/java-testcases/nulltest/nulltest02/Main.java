public class Main {
	
    public static void main(String[] args) {
    	String s = null;
    	if (Random.nextInt() > 0)
    		s = "hello";
    	else 
    		s = null;
    	
    	int x = 0;
    	if (s == null)
    		x = 1;
    	else
    		x = 2;
    	
    	// expected x == #TOP#
    }
}