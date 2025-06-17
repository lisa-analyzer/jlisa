public class ForLoopsExample {
    public static void main(String[] args) {
		
		for(String arg : args) {
			System.out.println(arg);
		}
		
		for(String arg : args) {	}

        for(int i=0;i < 10; i++) {
			System.out.println("i="+i);
		}
		
        for(;;) {
			System.out.println("Infinite Loop");
		}
		
		for(;;) {	}

    }
}