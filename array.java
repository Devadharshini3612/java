import java.util.Scanner;



public class array {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Step 1: Get matrix size
        System.out.print("Enter number of rows: ");
        int rows = sc.nextInt();
        System.out.print("Enter number of columns: ");
        int cols = sc.nextInt();

        // Step 2: Create matrices
        int[][] A = new int[rows][cols];
        int[][] B = new int[rows][cols];
        int[][] sum = new int[rows][cols];

        // Step 3: Read Matrix A
        System.out.println("\nEnter elements of Matrix A:");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print(A[i][j]);
                A[i][j] = sc.nextInt();
            }
        }

        // Step 4: Read Matrix B
        System.out.println("\nEnter elements of Matrix B:");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print(B[i][j]);
                B[i][j] = sc.nextInt();
            }
        }

        // Step 5: Add matrices
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sum[i][j] = A[i][j] + B[i][j];
            }
        }
        System.out.println(sum[i][j])
