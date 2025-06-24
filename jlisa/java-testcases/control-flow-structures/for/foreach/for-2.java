public class ForLoopsExample2 {
    public static void main(String[] args) {

		for(String arg : args) {
			System.out.println(arg);
		}

    }
	
	public static void emptyStructures(String[] args) {

		for(String arg : args) {	}
		
		for(String arg : args)
			;

	}
}