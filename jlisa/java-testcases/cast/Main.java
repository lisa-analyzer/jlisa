import java.util.Random;

;public class Main {

	public static void main(String[] args) {
		Object o1 = (Object) new Object();  
		Object o2 = new Object();
		Object o3 = (Object) new Random();
		Random r = (Random) o3;
		Object o4 = (String) new Object();    
	}
}