public class Main {

	public static void main(String[] args) {
		try {
            int result = 10 / 0;
            System.out.println("Result: " + result);
        } catch (Object e) {
            System.out.println("Error: Cannot divide by zero!");
        } catch (Exception e) {
            System.out.println("Error: Cannot divide by zero!");
        } finally {
        	System.out.println("Finally");
        }

        System.out.println("Program continues after try-catch.");
    }
}
