public class Main {
    public static void main(String[] args) {
        int i = 0;

        do {
			i++;
			continue;
        } while (i < 5);
/*
        i = 0;
        do {
			i++;
			break;
    } while (i < 5); // unreachable condition, but the java compiler compiles the code, without trigger any warning or error.
*/
        i = 0;
        do {
			i++;
			System.out.println("pre"+i);
            if(i % 2 == 0)
				continue;
            System.out.println("post"+i);
        } while (i < 5);

		int j = 0;

        while (j < 5){
			if(j % 2 == 0)
				break;
			j++;
		}

		for(int k = 0; k < 5; k++) {

			if(k == 3)
				break;
			if(k == 2)
				continue;

			System.out.println(""+k);
		}

		System.out.print("End!");
    }

}
