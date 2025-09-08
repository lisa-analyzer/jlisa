public class Main {
	
    public static void main(String[] args) {
    	String s = null;
    	int x = 0;
    	if (s != null)
    		x++; // not reachable
    	else 
    		x--; // reachable
    	
    	s = "hello";
    }
}