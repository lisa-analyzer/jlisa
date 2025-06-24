public class ForLoopsExample1 {
    public static void main(String[] args) {

        for(int i=0;i < 10; i++) {
			System.out.println("i="+i);
		}
		
        for(;;) {
			System.out.println("Infinite Loop");
		}

    }
	
	public static void emptyStructures() {

		for(;;) {	}
		
		for(;;)
			;
	}
}