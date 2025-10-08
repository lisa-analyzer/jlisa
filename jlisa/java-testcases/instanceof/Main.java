import java.util.Random;

public class Main {
	
	public static void main(String[] args) {
		Object o = new Object();
		int x = 0;
		if (o instanceof Object)
			x = 1;
		else
			x = -1;
		
		int y = 0;
		if (o instanceof Random)
			y = 1;
		else
			y = -1;
		
		Random r = new Random();
		int z = 0;
		if (o instanceof Object)
			z = 1;
		else
			z = -1;
	}
}