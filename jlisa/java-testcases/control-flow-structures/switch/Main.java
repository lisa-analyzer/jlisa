public class Main {
	public static void main(String[] args) {

		String s = "input";

		switch (s) {
			case "Hello":
				System.out.println("Hello");
				if (s == "TIGER")
					break;
			case "World":
				System.out.println("World");
				break;
			case "World2":
				System.out.println("World");
				break;
			default:
				System.out.println("Default");
				break;
		}
	
		switch (s) {
			case "TWO":
				System.out.println("2");
			case "THREE":
				System.out.println("3");
				break;
			default:
				System.out.println("unknown number");
				break;
		}

		switch (s) {
			case "Hi":
				System.out.println("Hello");
				if (s == "TIGER")
					break;
				System.out.println("World");
			case "Friend":
				System.out.println("World");
				break;
			default:
				System.out.println("Default");
				break;
		}

		switch (s) {
			case "DOG":
				System.out.println("doggo");
			case "CAT":
				System.out.println(s);
				System.out.println("domestic animal");
				break;
			case "TIGER":
				System.out.println(s);
				System.out.println("wild animal");
				break;
			default:
				System.out.println(s);
				System.out.println("unknown animal");
				
		}

		switch (s) {
			case "A":
			case "B":
			case "C":
			default:
		}

		switch (s) {
			case "1":
			case "2":
			case "3":
			default:
				System.out.println("unknown number");
		}

		switch (s) {
			default:
		}

		switch(s) {

		}
		
		switch (s) {
			case "1":
				return 0;
			case "2":
				return 1;
			case "3":
				return 2;
			default:
				return 3;
		}
	}
}
