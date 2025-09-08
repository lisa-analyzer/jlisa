public class Main {
	
    public static void main(String[] args) {
    	String s = null;
    	int x = 0;
    	if (s != null)
    		x++; // not reachable
    	else 
    		x--; // reachable
    	
    	s = "hello";
    	String s1 = s;
    	if (s1 == s)
    		x++; // reachable
    	else
    		x--; // not reachable
    	
    	
    	// expected x == 0;
    }
}