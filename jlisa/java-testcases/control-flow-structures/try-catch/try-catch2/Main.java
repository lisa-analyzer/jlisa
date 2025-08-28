public class Main {
  
  private static char foo(String name) {

        try{
            char c = name.charAt(1);
			return c;
        }
        catch(Exception e){
            println(e.getMessage());
			return 'a';
        }
        return '\0';
  }
  
  public static void main(String[] args) {
	      String res = null;

	foo("asd");
	
  }
}