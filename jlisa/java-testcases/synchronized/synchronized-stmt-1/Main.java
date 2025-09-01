public class Main
{
	public static void main(String[] args) {
	    Object o = new Object();
		synchronized(o){
		    System.out.println("Hello World!");
		}
	}
}